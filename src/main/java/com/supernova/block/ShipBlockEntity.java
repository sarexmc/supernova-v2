package com.supernova.block;

import com.supernova.menu.ShipMenu;
import com.supernova.registry.ModBlockEntities;
import com.supernova.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

public class ShipBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_FUEL = 25;     // % de combustible por tanque de dilitio
    public static final int TAKEOFF_FUEL = 50;  // % minimo para despegar

    public final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.DILITHIUM_TANK.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<ItemStackHandler> lazyItems = LazyOptional.of(() -> items);
    public int fuel = 0; // 0..100

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return fuel; }
        @Override public void set(int i, int v) { fuel = v; }
        @Override public int getCount() { return 1; }
    };

    public ShipBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHIP.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShipBlockEntity be) {
        if (be.fuel <= 100 - TANK_FUEL && !be.items.getStackInSlot(0).isEmpty()) {
            be.items.extractItem(0, 1, false);
            be.fuel += TANK_FUEL;
            be.setChanged();
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(5).expandTowards(0, 4, 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.supernova.ship");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ShipMenu(id, inv, items, data, ContainerLevelAccess.create(level, worldPosition), worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("Fuel", fuel);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        fuel = tag.getInt("Fuel");
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItems.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItems.invalidate();
    }
}
