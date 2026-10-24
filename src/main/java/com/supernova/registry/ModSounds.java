package com.supernova.registry;

import com.supernova.SupernovaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SupernovaMod.MODID);

    public static final RegistryObject<SoundEvent> EXPLOSION = reg("supernova_explosion");
    public static final RegistryObject<SoundEvent> RUMBLE = reg("rumble");
    public static final RegistryObject<SoundEvent> SHIP_LAUNCH = reg("ship_launch");
    public static final RegistryObject<SoundEvent> SHIP_WARP = reg("ship_warp");
    public static final RegistryObject<SoundEvent> BEEP = reg("beep");
    public static final RegistryObject<SoundEvent> RINGING = reg("ringing");
    public static final RegistryObject<SoundEvent> ENGINE_CHARGE = reg("engine_charge");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SupernovaMod.MODID, name)));
    }
}
