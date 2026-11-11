package com.danish.chronoarsenal.item;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * SPACE. A fast void dagger. Hits can rip the target through space (random teleport).
 * Right-click: Phase Blink, dash 14 blocks and slash everything on the path.
 * Sneak + right-click: Void Swap, trade places with the creature you're looking at.
 */
public class RiftEdgeItem extends SwordItem {
    private static final String BLINK = "BlinkAt";
    private static final String SWAP = "SwapAt";

    public RiftEdgeItem(Properties props) {
        super(Tiers.NETHERITE, 3, -1.6f, props); // 8 attack damage, very fast
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && attacker.getRandom().nextFloat() < 0.2f) {
            ServerLevel level = (ServerLevel) attacker.level();
            Vec3 before = target.position();
            double x = target.getX() + (attacker.getRandom().nextDouble() - 0.5) * 16;
            double y = target.getY() + attacker.getRandom().nextInt(8) - 4;
            double z = target.getZ() + (attacker.getRandom().nextDouble() - 0.5) * 16;
            if (target.randomTeleport(x, y, z, true)) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, before.x, before.y + 1, before.z, 30, 0.3, 0.6, 0.3, 0.05);
                FX.sound(level, before, SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0f, 0.7f);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerLevel server = (ServerLevel) level;
        ServerPlayer sp = (ServerPlayer) player;

        boolean done = player.isShiftKeyDown() ? voidSwap(server, sp, stack) : blink(server, sp, stack);
        if (!done) return InteractionResultHolder.fail(stack);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.success(stack);
    }

    private boolean blink(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (ItemCooldowns.secondsLeft(stack, BLINK, level) > 0) {
            return ItemCooldowns.tryUse(stack, BLINK, player, 50, "ability.chronoarsenal.blink"); // shows message
        }
        Vec3 start = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        BlockHitResult wall = level.clip(new ClipContext(eye, eye.add(look.scale(14)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDist = (wall.getType() == HitResult.Type.MISS ? 14 : wall.getLocation().distanceTo(eye)) - 0.7;

        // Walk back from the furthest point until the player's hitbox fits
        Vec3 dest = null;
        for (double d = maxDist; d > 0.5; d -= 0.5) {
            Vec3 candidate = start.add(look.scale(d));
            if (level.noCollision(player, player.getBoundingBox().move(candidate.subtract(start)))) {
                dest = candidate;
                break;
            }
        }
        if (dest == null) {
            player.displayClientMessage(Component.translatable("message.chronoarsenal.no_space")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        ItemCooldowns.set(stack, BLINK, level, 50);

        Vec3 a = start.add(0, player.getBbHeight() * 0.5, 0);
        Vec3 b = dest.add(0, player.getBbHeight() * 0.5, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(1.5),
                x -> RayUtil.isFoe(player, x))) {
            AABB box = e.getBoundingBox().inflate(0.9);
            if (box.clip(a, b).isPresent() || box.contains(b)) {
                e.invulnerableTime = 0;
                e.hurt(player.damageSources().playerAttack(player), 10.0f);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(),
                        1, 0, 0, 0, 0);
            }
        }

        FX.line(level, ParticleTypes.REVERSE_PORTAL, a, b, 0.25);
        level.sendParticles(ParticleTypes.PORTAL, a.x, a.y, a.z, 40, 0.3, 0.6, 0.3, 0.5);
        FX.sound(level, start, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.3f);

        player.stopRiding();
        player.connection.teleport(dest.x, dest.y, dest.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0;
        FX.sound(level, dest, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.3f);
        return true;
    }

    private boolean voidSwap(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (ItemCooldowns.secondsLeft(stack, SWAP, level) > 0) {
            return ItemCooldowns.tryUse(stack, SWAP, player, 100, "ability.chronoarsenal.void_swap"); // shows message
        }
        LivingEntity target = RayUtil.raycast(player, 24).entity();
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.chronoarsenal.no_target")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        ItemCooldowns.set(stack, SWAP, level, 100);

        Vec3 playerPos = player.position();
        Vec3 targetPos = target.position();
        player.stopRiding();
        target.stopRiding();

        if (target instanceof ServerPlayer other) {
            other.connection.teleport(playerPos.x, playerPos.y, playerPos.z, other.getYRot(), other.getXRot());
        } else {
            target.teleportTo(playerPos.x, playerPos.y, playerPos.z);
        }
        player.connection.teleport(targetPos.x, targetPos.y, targetPos.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0;

        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));

        FX.line(level, ParticleTypes.REVERSE_PORTAL, playerPos.add(0, 1, 0), targetPos.add(0, 1, 0), 0.3);
        level.sendParticles(ParticleTypes.PORTAL, playerPos.x, playerPos.y + 1, playerPos.z, 40, 0.3, 0.6, 0.3, 0.5);
        level.sendParticles(ParticleTypes.PORTAL, targetPos.x, targetPos.y + 1, targetPos.z, 40, 0.3, 0.6, 0.3, 0.5);
        FX.sound(level, playerPos, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.6f);
        FX.sound(level, targetPos, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.6f);
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "rift_edge");
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
