package com.supernova.entity;

import com.supernova.block.ShipBlockEntity;
import com.supernova.registry.*;
import com.supernova.world.GlebaChunkGenerator;
import com.supernova.world.GlebaDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.*;

/**
 * La nave. Fases: CHARGING (mantener CTRL hasta 100%) -> COUNTDOWN (3-2-1) -> LIFTOFF -> ATMOSPHERE -> EXIT
 * -> TIME_JUMP (pasa a Gleba) -> STABILIZE -> APPROACH -> LANDING.
 */
public class ShipEntity extends Entity {
    public enum Stage { CHARGING, COUNTDOWN, LIFTOFF, ATMOSPHERE, EXIT, TIME_JUMP, STABILIZE, APPROACH, LANDING }

    public static final float SCALE = 2.0f;
    private static final EntityDataAccessor<Integer> D_STAGE = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> D_METERS = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> D_FUEL = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> D_SPEED = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> D_CHARGE = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);

    public static final double GLEBA_ARRIVAL_Y = 1000.0;
    public static final int JUMP_METERS = 700;

    private final Set<UUID> crew = new HashSet<>();
    private final Map<UUID, Integer> lastHeld = new HashMap<>();
    private BlockPos homePos = BlockPos.ZERO;
    private double launchY;
    private double groundY = 70;
    private double vy;
    private float charge;
    private int stageTicks;
    private boolean chargeSound;

    // solo cliente
    private int clientStageTicks;
    private int lastStage = -1;
    private int lerpSteps;
    private double lx, ly, lz;

    public ShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // ------------------------------------------------------------------ API
    public void setup(BlockPos home, double launchY, double groundY, int fuel, Stage stage, Collection<ServerPlayer> players) {
        this.homePos = home;
        this.launchY = launchY;
        this.groundY = groundY;
        this.entityData.set(D_FUEL, fuel);
        this.entityData.set(D_STAGE, stage.ordinal());
        this.stageTicks = 0;
        crew.clear();
        for (ServerPlayer p : players) crew.add(p.getUUID());
    }

    public Stage stage() { return Stage.values()[Mth.clamp(entityData.get(D_STAGE), 0, Stage.values().length - 1)]; }
    public int meters() { return entityData.get(D_METERS); }
    public int fuel() { return entityData.get(D_FUEL); }
    public int charge() { return entityData.get(D_CHARGE); }
    public float speedMs() { return entityData.get(D_SPEED) / 10f; }
    public int stageTicksClient() { return clientStageTicks; }
    public double groundY() { return groundY; }

    public void markHeld(ServerPlayer p) {
        lastHeld.put(p.getUUID(), tickCount);
    }

    private void setStage(Stage s) {
        entityData.set(D_STAGE, s.ordinal());
        stageTicks = 0;
    }

    // ------------------------------------------------------------------ boilerplate
    @Override
    protected void defineSynchedData() {
        entityData.define(D_STAGE, 0);
        entityData.define(D_METERS, 0);
        entityData.define(D_FUEL, 0);
        entityData.define(D_SPEED, 0);
        entityData.define(D_CHARGE, 0);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean hurt(DamageSource src, float amount) { return false; }
    @Override public boolean isNoGravity() { return true; }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 4;
    }

    /** Los pasajeros van dentro de la cabina (no se dibujan, asi su skin no sale por fuera). */
    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) return;
        int i = getPassengers().indexOf(passenger);
        double dx = 0, dz = 0;
        if (i > 0) {
            double a = (i - 1) * (Math.PI * 2 / 3.0);
            dx = Math.cos(a) * 0.45;
            dz = Math.sin(a) * 0.45;
        }
        callback.accept(passenger, getX() + dx, getY() + 1.9, getZ() + dz);
    }

    /** Interpolacion suave en el cliente (la nave no "salta" de posicion). */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.lx = x;
        this.ly = y;
        this.lz = z;
        this.lerpSteps = Math.max(steps, 2);
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) clientTick(); else serverTick();
    }

    private List<ServerPlayer> ridersNow() {
        List<ServerPlayer> list = new ArrayList<>();
        for (Entity e : getPassengers()) if (e instanceof ServerPlayer sp) list.add(sp);
        return list;
    }

    private void serverTick() {
        stageTicks++;
        ServerLevel sl = (ServerLevel) level();
        Stage stage = stage();

        if (stage == Stage.CHARGING) {
            chargingTick(sl);
            return;
        }
        keepCrewAboard();

        double target = 0, ease = 0.05;
        switch (stage) {
            case COUNTDOWN -> {
                if (stageTicks == 1 || stageTicks == 21 || stageTicks == 41)
                    sl.playSound(null, blockPosition(), ModSounds.BEEP.get(), SoundSource.NEUTRAL, 2f, stageTicks == 41 ? 1.4f : 1f);
                if (stageTicks == 1) sl.playSound(null, blockPosition(), ModSounds.SHIP_LAUNCH.get(), SoundSource.NEUTRAL, 6f, 1f);
                if (stageTicks % 2 == 0) {
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.2, getZ(), 8, 1.8, 0.1, 1.8, 0.03);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 5, 1.4, 0.1, 1.4, 0.05);
                }
                if (stageTicks >= 60) {
                    burnPad(sl);
                    setStage(Stage.LIFTOFF);
                }
            }
            case LIFTOFF -> {
                target = 0.9; ease = 0.03;
                smash(sl);
                if (meters() >= 60) setStage(Stage.ATMOSPHERE);
            }
            case ATMOSPHERE -> {
                target = 1.4;
                smash(sl);
                if (meters() >= 350) setStage(Stage.EXIT);
            }
            case EXIT -> {
                target = 1.8;
                smash(sl);
                if (meters() >= JUMP_METERS) setStage(Stage.TIME_JUMP);
            }
            case TIME_JUMP -> {
                target = 0.3;
                if (stageTicks == 1) sl.playSound(null, blockPosition(), ModSounds.SHIP_WARP.get(), SoundSource.NEUTRAL, 4f, 1f);
                if (stageTicks >= 200) {
                    jumpToGleba();
                    return;
                }
            }
            case STABILIZE -> {
                target = -0.5;
                if (stageTicks >= 220) setStage(Stage.APPROACH);
            }
            case APPROACH -> {
                target = -2.0;
                if (getY() <= 400) setStage(Stage.LANDING);
            }
            case LANDING -> {
                target = -Mth.clamp((getY() - groundY) * 0.05, 0.12, 1.5);
                if (getY() <= groundY + 1.5) {
                    land();
                    return;
                }
            }
            default -> { }
        }

        vy += (target - vy) * ease;
        if (stage != Stage.COUNTDOWN) setPos(getX(), getY() + vy, getZ());
        setDeltaMovement(0, vy, 0);
        entityData.set(D_METERS, computeMeters());
        entityData.set(D_SPEED, Math.round((float) Math.abs(vy) * 20f * 10f));
    }

    /** Fase de encendido: hay que mantener CTRL hasta llegar a 100 %. */
    private void chargingTick(ServerLevel sl) {
        List<ServerPlayer> riders = ridersNow();
        if (riders.isEmpty() && stageTicks > 40) {
            cancelCharge(sl, "Despegue cancelado: no hay tripulacion a bordo.");
            return;
        }
        boolean held = false;
        for (ServerPlayer p : riders) {
            Integer t = lastHeld.get(p.getUUID());
            if (t != null && tickCount - t <= 3) held = true;
        }
        if (held) {
            if (!chargeSound) {
                chargeSound = true;
                sl.playSound(null, blockPosition(), ModSounds.ENGINE_CHARGE.get(), SoundSource.NEUTRAL, 5f, 1f);
            }
            charge = Math.min(100f, charge + 1f);   // 5 segundos manteniendo
        } else {
            charge = Math.max(0f, charge - 2f);
            if (charge <= 0f) chargeSound = false;
        }
        entityData.set(D_CHARGE, (int) charge);
        if (charge >= 100f) {
            entityData.set(D_FUEL, Math.max(0, fuel() - ShipBlockEntity.TAKEOFF_FUEL));
            crew.clear();
            for (ServerPlayer p : riders) crew.add(p.getUUID());
            setStage(Stage.COUNTDOWN);
        } else if (!held && charge <= 0f && stageTicks > 200) {
            cancelCharge(sl, "Despegue cancelado.");
        }
    }

    private void cancelCharge(ServerLevel sl, String msg) {
        for (ServerPlayer p : ridersNow()) {
            p.stopRiding();
            p.displayClientMessage(Component.literal(msg), true);
        }
        sl.setBlock(homePos, ModBlocks.SHIP.get().defaultBlockState(), 3);
        if (sl.getBlockEntity(homePos) instanceof ShipBlockEntity be) {
            be.fuel = fuel();
            be.setChanged();
        }
        discard();
    }

    private int computeMeters() {
        if (level().dimension().equals(ModDimensions.GLEBA)) {
            double f = Mth.clamp((GLEBA_ARRIVAL_Y - getY()) / (GLEBA_ARRIVAL_Y - groundY), 0, 1);
            return (int) (JUMP_METERS + 500 * f);
        }
        return (int) Math.max(0, getY() - launchY);
    }

    private List<ServerPlayer> crewPlayers() {
        MinecraftServer server = ((ServerLevel) level()).getServer();
        List<ServerPlayer> list = new ArrayList<>();
        for (UUID id : crew) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && p.isAlive()) list.add(p);
        }
        return list;
    }

    private void keepCrewAboard() {
        for (ServerPlayer p : crewPlayers())
            if (p.level() == level() && p.getVehicle() != this) p.startRiding(this, true);
    }

    /** La nave destruye todo a su paso al subir por la atmosfera. */
    private void smash(ServerLevel sl) {
        int r = 3;
        int y0 = (int) Math.floor(getY() + 0.5);
        int y1 = (int) Math.floor(getY() + 5.5 + Math.max(0, vy) * 2);
        if (y0 >= sl.getMaxBuildHeight()) return;
        y1 = Math.min(y1, sl.getMaxBuildHeight() - 1);
        int cx = Mth.floor(getX()), cz = Mth.floor(getZ());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int broken = 0;
        for (int y = y0; y <= y1; y++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 1) continue;
                    p.set(cx + dx, y, cz + dz);
                    BlockState st = sl.getBlockState(p);
                    if (st.isAir() || st.getDestroySpeed(sl, p) < 0) continue;
                    sl.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    broken++;
                }
        if (broken > 0 && tickCount % 2 == 0)
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), y0 + 2, getZ(), 1, 1.2, 0.8, 1.2, 0.0);
    }

    /** Las llamas del despegue queman lo que haya cerca de la base. */
    private void burnPad(ServerLevel sl) {
        AABB box = new AABB(getX() - 5, getY() - 2, getZ() - 5, getX() + 5, getY() + 3, getZ() + 5);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, e -> !crew.contains(e.getUUID()))) {
            e.setSecondsOnFire(6);
            e.hurt(sl.damageSources().inFire(), 6f);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
    }

    private void jumpToGleba() {
        ServerLevel old = (ServerLevel) level();
        ServerLevel gleba = GlebaDimension.get(old.getServer());
        List<ServerPlayer> players = crewPlayers();
        if (gleba == null) {
            for (ServerPlayer p : players) {
                p.stopRiding();
                p.sendSystemMessage(Component.literal("No se pudo crear la dimension Gleba (mira el log del servidor)."));
            }
            discard();
            return;
        }
        gleba.getChunk(0, 0);
        for (ServerPlayer p : players) p.stopRiding();
        ShipEntity next = new ShipEntity(ModEntities.SHIP.get(), gleba);
        next.setPos(0.5, GLEBA_ARRIVAL_Y, 0.5);
        next.setup(homePos, launchY, GlebaChunkGenerator.heightAt(0, 0), fuel(), Stage.STABILIZE, players);
        next.vy = -0.3;
        for (ServerPlayer p : players) p.teleportTo(gleba, 0.5, GLEBA_ARRIVAL_Y, 0.5, p.getYRot(), 0f);
        gleba.addFreshEntity(next);
        for (ServerPlayer p : players) p.startRiding(next, true);
        discard();
    }

    private void land() {
        ServerLevel sl = (ServerLevel) level();
        int cx = Mth.floor(getX()), cz = Mth.floor(getZ());
        int gy = GlebaChunkGenerator.heightAt(cx, cz);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                sl.setBlock(new BlockPos(cx + dx, gy, cz + dz), ModBlocks.LAUNCH_PAD.get().defaultBlockState(), 3);
                for (int dy = 1; dy <= 6; dy++)
                    sl.setBlock(new BlockPos(cx + dx, gy + dy, cz + dz), Blocks.AIR.defaultBlockState(), 3);
            }
        BlockPos shipPos = new BlockPos(cx, gy + 1, cz);
        sl.setBlock(shipPos, ModBlocks.SHIP.get().defaultBlockState(), 3);
        if (sl.getBlockEntity(shipPos) instanceof ShipBlockEntity be) {
            be.fuel = fuel();
            be.setChanged();
        }
        int i = 0;
        for (ServerPlayer p : crewPlayers()) {
            p.stopRiding();
            int px = cx + 5, pz = cz - 1 + i++;
            p.teleportTo(sl, px + 0.5, GlebaChunkGenerator.heightAt(px, pz) + 1, pz + 0.5, p.getYRot(), 0f);
            p.sendSystemMessage(Component.literal("Hemos aterrizado en Gleba."));
            p.displayClientMessage(Component.literal("Bienvenidos a Gleba"), true);
        }
        crew.clear();
        discard();
    }

    // ------------------------------------------------------------------ cliente: fuego, humo, aros y nubes
    private void clientTick() {
        if (lerpSteps > 0) {
            double k = 1.0 / lerpSteps;
            setPos(Mth.lerp(k, getX(), lx), Mth.lerp(k, getY(), ly), Mth.lerp(k, getZ(), lz));
            lerpSteps--;
        }
        int s = entityData.get(D_STAGE);
        if (s != lastStage) {
            lastStage = s;
            clientStageTicks = 0;
        }
        clientStageTicks++;
        Stage st = Stage.values()[Mth.clamp(s, 0, Stage.values().length - 1)];
        RandomSource r = random;
        double x = getX(), z = getZ(), ny = getY() - 0.6;

        int flames = switch (st) {
            case COUNTDOWN -> clientStageTicks > 20 ? 5 : 0;
            case LIFTOFF, ATMOSPHERE, EXIT, APPROACH, LANDING -> 14;
            case TIME_JUMP -> 20;
            case STABILIZE -> 3;
            default -> 0;
        };
        for (int i = 0; i < flames; i++) {
            double ox = (r.nextDouble() - .5) * 1.4, oz = (r.nextDouble() - .5) * 1.4;
            level().addParticle(ParticleTypes.FLAME, x + ox, ny, z + oz, ox * 0.05, -0.55 - r.nextDouble() * 0.4, oz * 0.05);
        }
        if (flames > 0) {
            for (int i = 0; i < 6; i++) {
                double ox = (r.nextDouble() - .5) * 0.9, oz = (r.nextDouble() - .5) * 0.9;
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + ox, ny - 0.2, z + oz, 0, -0.9, 0);
            }
            for (int i = 0; i < 4; i++)
                level().addParticle(ParticleTypes.LARGE_SMOKE, x + (r.nextDouble() - .5), ny - 0.5, z + (r.nextDouble() - .5),
                        (r.nextDouble() - .5) * 0.2, -0.25, (r.nextDouble() - .5) * 0.2);
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + (r.nextDouble() - .5), ny - 1.0, z + (r.nextDouble() - .5), 0, -0.05, 0);
            for (int i = 0; i < 3; i++)
                level().addParticle(ParticleTypes.FIREWORK, x, ny, z, (r.nextDouble() - .5) * 0.6, -0.4 - r.nextDouble() * 0.4, (r.nextDouble() - .5) * 0.6);
            if (r.nextInt(2) == 0) level().addParticle(ParticleTypes.LAVA, x + (r.nextDouble() - .5), ny, z + (r.nextDouble() - .5), 0, 0, 0);
        }
        // nube de polvo al encender motores en la plataforma
        if ((st == Stage.COUNTDOWN || st == Stage.LIFTOFF) && clientStageTicks % 2 == 0 && clientStageTicks < 90) {
            for (int k = 0; k < 14; k++) {
                double a = k * Math.PI * 2 / 14.0;
                level().addParticle(ParticleTypes.CLOUD, x + Math.cos(a), getY() + 0.1, z + Math.sin(a), Math.cos(a) * 0.35, 0.02, Math.sin(a) * 0.35);
            }
        }
        // aros de energia que salen de la nave
        boolean rings = st == Stage.LIFTOFF || st == Stage.ATMOSPHERE || st == Stage.EXIT || st == Stage.TIME_JUMP;
        if (rings && clientStageTicks % 6 == 0) {
            for (int k = 0; k < 40; k++) {
                double a = k * Math.PI * 2 / 40.0;
                level().addParticle(ParticleTypes.END_ROD, x + Math.cos(a) * 1.3, ny - 0.4, z + Math.sin(a) * 1.3, Math.cos(a) * 0.4, -0.15, Math.sin(a) * 0.4);
            }
        }
        // desde los 450 m: anillo de condensacion en la punta y nubes alrededor
        int m = entityData.get(D_METERS);
        if (!level().dimension().equals(ModDimensions.GLEBA) && m >= 450 && (st == Stage.ATMOSPHERE || st == Stage.EXIT)) {
            double ty = getY() + 4.7;
            if (clientStageTicks % 2 == 0) {
                for (int k = 0; k < 28; k++) {
                    double a = k * Math.PI * 2 / 28.0;
                    level().addParticle(ParticleTypes.CLOUD, x + Math.cos(a) * 1.1, ty, z + Math.sin(a) * 1.1, Math.cos(a) * 0.3, -0.05, Math.sin(a) * 0.3);
                }
            }
            for (int k = 0; k < 8; k++)
                level().addParticle(ParticleTypes.CLOUD, x + (r.nextDouble() - .5) * 14, getY() + (r.nextDouble() - .5) * 14, z + (r.nextDouble() - .5) * 14, 0, -0.6, 0);
        }
        // entrada en la atmosfera de Gleba: fuego alrededor del casco
        if (level().dimension().equals(ModDimensions.GLEBA) && (st == Stage.APPROACH || st == Stage.LANDING) && getY() - groundY < 620) {
            for (int k = 0; k < 14; k++) {
                double a = r.nextDouble() * Math.PI * 2;
                level().addParticle(ParticleTypes.FLAME, x + Math.cos(a) * 1.6, getY() + r.nextDouble() * 4.5, z + Math.sin(a) * 1.6, 0, 0.5, 0);
            }
        }
    }
}
