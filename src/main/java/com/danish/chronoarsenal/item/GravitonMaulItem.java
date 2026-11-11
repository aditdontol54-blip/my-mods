package com.danish.chronoarsenal.item;

import com.danish.chronoarsenal.logic.ScheduledEffects;
import com.danish.chronoarsenal.logic.Singularity;
import com.danish.chronoarsenal.util.FX;
import com.danish.chronoarsenal.util.ItemCooldowns;
import com.danish.chronoarsenal.util.RayUtil;
import com.danish.chronoarsenal.util.Tips;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
 * GRAVITY. Hits launch enemies into the air; hitting an airborne enemy deals bonus damage (juggle combos).
 * Right-click: Singularity (black hole). Sneak + right-click: Repulsor shockwave that also reflects projectiles.
 */
public class GravitonMaulItem extends SwordItem {
    private static final String SINGULARITY = "SingularityAt";
    private static final String REPULSE = "RepulseAt";

    public GravitonMaulItem(Properties props) {
        super(Tiers.NETHERITE, 8, -3.2f, props); // 13 attack damage, slow and heavy
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            boolean airborne = !target.onGround();
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.85, 0));
            target.hurtMarked = true;
            if (airborne) {
                target.invulnerableTime = 0;
                target.hurt(attacker.damageSources().indirectMagic(attacker, attacker), 5.0f);
                ((ServerLevel) attacker.level()).sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 20, 0.4, 0.4, 0.4, 0.3);
                FX.sound(attacker.level(), target.position(), SoundEvents.ANVIL_LAND, 0.6f, 1.4f);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerLevel server = (ServerLevel) level;

        if (player.isShiftKeyDown()) {
            if (!ItemCooldowns.tryUse(stack, REPULSE, player, 160, "ability.chronoarsenal.repulse")) {
                return InteractionResultHolder.fail(stack);
            }
            repulse(server, player);
        } else {
            if (!ItemCooldowns.tryUse(stack, SINGULARITY, player, 400, "ability.chronoarsenal.singularity")) {
                return InteractionResultHolder.fail(stack);
            }
            RayUtil.Hit hit = RayUtil.raycast(player, 10);
            Vec3 center = hit.entity() != null
                    ? hit.entity().getBoundingBox().getCenter()
                    : hit.pos().subtract(player.getLookAngle().scale(0.6)).add(0, 0.5, 0);
            ScheduledEffects.add(new Singularity(server, center, player.getUUID()));
            FX.sound(level, center, SoundEvents.WARDEN_SONIC_CHARGE, 2.0f, 0.6f);
            player.displayClientMessage(Component.translatable("message.chronoarsenal.singularity"), true);
        }
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.success(stack);
    }

    private void repulse(ServerLevel level, Player player) {
        Vec3 c = player.position();
        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox().inflate(7);

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> RayUtil.isFoe(player, e))) {
            Vec3 away = e.position().subtract(c);
            double dist = away.length();
            if (dist > 7) continue;
            Vec3 flat = new Vec3(away.x, 0, away.z).normalize();
            if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(look.x, 0, look.z).normalize();
            double force = Math.max(0.6, 2.2 * (1.0 - dist / 8.0));
            e.setDeltaMovement(flat.x * force, 0.8, flat.z * force);
            e.hurtMarked = true;
            e.invulnerableTime = 0;
            e.hurt(player.damageSources().indirectMagic(player, player), 6.0f);
        }
        // Reflect incoming projectiles back at whoever shot them
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, area)) {
            if (p.getOwner() == player) continue;
            p.setDeltaMovement(p.getDeltaMovement().scale(-1.5));
            p.setOwner(player);
            p.hurtMarked = true;
        }

        for (double r = 2; r <= 6; r += 2) {
            FX.ring(level, ParticleTypes.EXPLOSION, c.add(0, 0.3, 0), r, (int) (r * 6));
        }
        FX.sound(level, c, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.8f);
        player.fallDistance = 0;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "graviton_maul");
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
