package com.danish.chronoarsenal.item;

import com.danish.chronoarsenal.util.FX;
import com.danish.chronoarsenal.util.RayUtil;
import com.danish.chronoarsenal.util.Tips;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * PLASMA. Hold right-click for full-auto plasma bolts that set targets on fire.
 * Every shot adds heat (the bar under the icon). Hit 100% and it overheats:
 * vents steam, burns your hand and locks for 3 seconds.
 * Sneak while firing: Overcharge, each shot is stronger but heats twice as fast.
 */
public class PlasmaRepeaterItem extends Item {
    private static final String HEAT = "Heat";
    private static final float MAX_HEAT = 100f;
    private static final DustParticleOptions PLASMA = new DustParticleOptions(new Vector3f(1.0f, 0.25f, 0.85f), 1.2f);
    private static final DustParticleOptions OVERCHARGED = new DustParticleOptions(new Vector3f(0.3f, 0.9f, 1.0f), 1.6f);

    public PlasmaRepeaterItem(Properties props) {
        super(props);
    }

    private static float getHeat(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0f : tag.getFloat(HEAT);
    }

    private static void setHeat(ItemStack stack, float heat) {
        if (heat <= 0f) {
            CompoundTag tag = stack.getTag();
            if (tag != null) tag.remove(HEAT);
        } else {
            stack.getOrCreateTag().putFloat(HEAT, Math.min(heat, MAX_HEAT));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(living instanceof Player player)) return;
        int used = getUseDuration(stack) - remainingUseDuration;
        if (used % 4 != 0) return;

        boolean overcharge = player.isShiftKeyDown();
        fire((ServerLevel) level, player, overcharge);

        float heat = getHeat(stack) + (overcharge ? 14f : 7f);
        if (heat >= MAX_HEAT) {
            setHeat(stack, MAX_HEAT);
            player.stopUsingItem();
            player.getCooldowns().addCooldown(this, 60);
            Vec3 m = RayUtil.muzzle(player);
            ((ServerLevel) level).sendParticles(ParticleTypes.LARGE_SMOKE, m.x, m.y, m.z, 15, 0.2, 0.2, 0.2, 0.03);
            FX.sound(level, m, SoundEvents.FIRE_EXTINGUISH, 1.0f, 0.8f);
            player.hurt(player.damageSources().onFire(), 2.0f);
            player.displayClientMessage(Component.translatable("message.chronoarsenal.overheated")
                    .withStyle(ChatFormatting.GOLD), true);
        } else {
            setHeat(stack, heat);
        }
    }

    private void fire(ServerLevel level, Player player, boolean overcharge) {
        RayUtil.Hit hit = RayUtil.raycast(player, 40);
        Vec3 muzzle = RayUtil.muzzle(player);
        FX.line(level, overcharge ? OVERCHARGED : PLASMA, muzzle, hit.pos(), 0.35);
        FX.sound(level, muzzle, SoundEvents.BLAZE_SHOOT, 0.35f, overcharge ? 1.2f : 1.7f + level.getRandom().nextFloat() * 0.2f);

        if (hit.entity() != null) {
            LivingEntity target = hit.entity();
            target.invulnerableTime = 0; // full-auto: every bolt counts
            target.hurt(player.damageSources().playerAttack(player), overcharge ? 9.0f : 5.0f);
            target.setSecondsOnFire(overcharge ? 6 : 3);
            level.sendParticles(ParticleTypes.FLAME, hit.pos().x, hit.pos().y, hit.pos().z, 8, 0.15, 0.15, 0.15, 0.05);
        } else if (hit.hitBlock()) {
            level.sendParticles(ParticleTypes.LAVA, hit.pos().x, hit.pos().y, hit.pos().z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SMOKE, hit.pos().x, hit.pos().y, hit.pos().z, 4, 0.1, 0.1, 0.1, 0.01);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 2 != 0) return;
        float heat = getHeat(stack);
        if (heat <= 0f) return;
        if (entity instanceof LivingEntity living && living.isUsingItem() && living.getUseItem() == stack) return;
        setHeat(stack, heat - 2.5f); // cools fully in about 4 seconds
    }

    // The heat gauge reuses the durability bar under the item icon.
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getHeat(stack) > 0.5f;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * getHeat(stack) / MAX_HEAT);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float t = getHeat(stack) / MAX_HEAT; // cyan when cool, red when hot
        int r = (int) (64 + t * (255 - 64));
        int g = (int) (224 - t * (224 - 40));
        int b = (int) (255 - t * (255 - 40));
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "plasma_repeater");
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
