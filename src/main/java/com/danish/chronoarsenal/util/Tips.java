package com.danish.chronoarsenal.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Adds the standard four tooltip lines for a weapon from the language file. */
public final class Tips {
    private Tips() {}

    public static void add(List<Component> tooltip, String item) {
        String base = "tooltip.chronoarsenal." + item;
        tooltip.add(Component.translatable(base + ".lore").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(base + ".passive").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(base + ".use").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(base + ".sneak").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
