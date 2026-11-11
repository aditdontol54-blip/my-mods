package com.danish.chronoarsenal.registry;

import com.danish.chronoarsenal.ChronoArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChronoArsenal.MODID);

    public static final RegistryObject<CreativeModeTab> ARSENAL = TABS.register("arsenal",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chronoarsenal"))
                    .icon(() -> new ItemStack(ModItems.CHRONO_BLADE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.QUANTUM_CORE.get());
                        output.accept(ModItems.CHRONO_BLADE.get());
                        output.accept(ModItems.PLASMA_REPEATER.get());
                        output.accept(ModItems.GRAVITON_MAUL.get());
                        output.accept(ModItems.RIFT_EDGE.get());
                        output.accept(ModItems.ARC_CASTER.get());
                    })
                    .build());

    private ModTabs() {}
}
