package com.danish.chronoarsenal.util;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Per-ability cooldowns stored on the item itself, so one weapon can have two
 * abilities with separate timers (vanilla cooldowns are one per item type).
 */
public final class ItemCooldowns {
    private ItemCooldowns() {}

    public static int secondsLeft(ItemStack stack, String key, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        long left = tag.getLong(key) - level.getGameTime();
        return left <= 0 ? 0 : (int) Math.ceil(left / 20.0);
    }

    public static void set(ItemStack stack, String key, Level level, int ticks) {
        stack.getOrCreateTag().putLong(key, level.getGameTime() + ticks);
    }

    public static void reset(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        if (tag != null) tag.remove(key);
    }

    /** Starts the cooldown and returns true if ready; otherwise tells the player how long is left. */
    public static boolean tryUse(ItemStack stack, String key, Player player, int ticks, String abilityKey) {
        int left = secondsLeft(stack, key, player.level());
        if (left > 0) {
            player.displayClientMessage(Component.translatable("message.chronoarsenal.recharging",
                    Component.translatable(abilityKey), left).withStyle(ChatFormatting.RED), true);
            return false;
        }
        set(stack, key, player.level(), ticks);
        return true;
    }
}
