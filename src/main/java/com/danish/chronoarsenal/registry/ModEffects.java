package com.danish.chronoarsenal.registry;

import com.danish.chronoarsenal.ChronoArsenal;
import com.danish.chronoarsenal.effect.TemporalStasisEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ChronoArsenal.MODID);

    public static final RegistryObject<MobEffect> TEMPORAL_STASIS =
            EFFECTS.register("temporal_stasis", TemporalStasisEffect::new);

    private ModEffects() {}
}
