package com.danish.chronoarsenal.item;

import com.danish.chronoarsenal.logic.RewindTracker;
import com.danish.chronoarsenal.logic.ScheduledEffects;
import com.danish.chronoarsenal.registry.ModEffects;
import com.danish.chronoarsenal.util.FX;
import com.danish.chronoarsenal.util.ItemCooldowns;
import com.danish.chronoarsenal.util.RayUtil;
import com.danish.chronoarsenal.util.Tips;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * TIME. Hits slow the target and leave a "Temporal Echo" that strikes again 1s later.
 * Right-click: Time Stop. Sneak + right-click: Rewind 5 seconds.
 */
public class ChronoBladeItem extends SwordItem {
    private static final String TIME_STOP = "TimeStopAt";
    private static final String REWIND = "RewindAt";

    public ChronoBladeItem(Properties props) {
        super(Tiers.NETHERITE, 5, -2.2f, props); // 10 attack damage
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            if (attacker.getRandom().nextFloat() < 0.15f) {
                target.addEffect(new MobEffectInstance(ModEffects.TEMPORAL_STASIS.get(), 30, 0));
            }
            // Temporal Echo: the same strike lands again one second later
            ScheduledEffects.delay(20, () -> {
                if (target.isAlive() && target.level() instanceof ServerLevel level) {
                    DamageSource src = attacker.isAlive()
                            ? level.damageSources().indirectMagic(attacker, attacker)
                            : level.damageSources().magic();
                    target.invulnerableTime = 0;
                    target.hurt(src, 4.0f);
                    level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                            target.getZ(), 12, 0.3, 0.5, 0.3, 0.05);
                    FX.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
                }
            });
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerLevel server = (ServerLevel) level;

        if (player.isShiftKeyDown()) {
            if (!ItemCooldowns.tryUse(stack, REWIND, player, 900, "ability.chronoarsenal.rewind")) {
                return InteractionResultHolder.fail(stack);
            }
            Vec3 from = player.position().add(0, 1, 0);
            if (!RewindTracker.rewind((ServerPlayer) player)) {
                ItemCooldowns.reset(stack, REWIND);
                return InteractionResultHolder.fail(stack);
            }
            Vec3 to = player.position().add(0, 1, 0);
            FX.line(server, ParticleTypes.REVERSE_PORTAL, from, to, 0.3);
            FX.sphere(server, ParticleTypes.END_ROD, to, 1.2, 40);
            FX.sound(level, from, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, 0.6f);
            FX.sound(level, to, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, 0.6f);
            player.displayClientMessage(Component.translatable("message.chronoarsenal.rewind")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else {
            if (!ItemCooldowns.tryUse(stack, TIME_STOP, player, 600, "ability.chronoarsenal.time_stop")) {
                return InteractionResultHolder.fail(stack);
            }
            AABB area = player.getBoundingBox().inflate(12);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> RayUtil.isFoe(player, e) && e.distanceToSqr(player) <= 144)) {
                e.addEffect(new MobEffectInstance(ModEffects.TEMPORAL_STASIS.get(), 100, 0, false, true));
            }
            // Arrows, fireballs and tridents lose all momentum
            for (Projectile p : level.getEntitiesOfClass(Projectile.class, area)) {
                p.setDeltaMovement(Vec3.ZERO);
                p.hurtMarked = true;
            }
            Vec3 c = player.position().add(0, 1, 0);
            FX.sphere(server, ParticleTypes.REVERSE_PORTAL, c, 6, 220);
            FX.ring(server, ParticleTypes.END_ROD, player.position().add(0, 0.2, 0), 12, 100);
            FX.sound(level, c, SoundEvents.BEACON_DEACTIVATE, 2.0f, 0.5f);
            FX.sound(level, c, SoundEvents.BELL_RESONATE, 1.5f, 0.8f);
            player.displayClientMessage(Component.translatable("message.chronoarsenal.time_stop")
                    .withStyle(ChatFormatting.AQUA), true);
        }
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "chrono_blade");
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
