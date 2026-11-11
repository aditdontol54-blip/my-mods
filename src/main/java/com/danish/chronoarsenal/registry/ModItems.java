package com.danish.chronoarsenal.registry;

import com.danish.chronoarsenal.ChronoArsenal;
import com.danish.chronoarsenal.item.ArcCasterItem;
import com.danish.chronoarsenal.item.ChronoBladeItem;
import com.danish.chronoarsenal.item.GravitonMaulItem;
import com.danish.chronoarsenal.item.PlasmaRepeaterItem;
import com.danish.chronoarsenal.item.RiftEdgeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ChronoArsenal.MODID);

    public static final RegistryObject<Item> QUANTUM_CORE = ITEMS.register("quantum_core",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> CHRONO_BLADE = ITEMS.register("chrono_blade",
            () -> new ChronoBladeItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> PLASMA_REPEATER = ITEMS.register("plasma_repeater",
            () -> new PlasmaRepeaterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> GRAVITON_MAUL = ITEMS.register("graviton_maul",
            () -> new GravitonMaulItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> RIFT_EDGE = ITEMS.register("rift_edge",
            () -> new RiftEdgeItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> ARC_CASTER = ITEMS.register("arc_caster",
            () -> new ArcCasterItem(new Item.Properties().durability(1500).rarity(Rarity.EPIC).fireResistant()));

    private ModItems() {}
}
