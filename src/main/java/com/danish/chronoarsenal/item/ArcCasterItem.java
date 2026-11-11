package com.danish.chronoarsenal.item;

import com.danish.chronoarsenal.util.FX;
import com.danish.chronoarsenal.util.ItemCooldowns;
import com.danish.chronoarsenal.util.RayUtil;
import com.danish.chronoarsenal.util.Tips;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ELECTRICITY. Right-click: Chain Lightning, an arc that jumps between up to 6 enemies.
 * Wet targets (rain or water) take 50% more. Sneak + right-click: Storm Call, a real
 * lightning strike where you look, with arcs leaping to everything nearby.
 */
public class ArcCasterItem extends Item {
    private static final String STORM = "StormAt";

    public ArcCasterItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerLevel server = (ServerLevel) level;

        if (player.isShiftKeyDown()) {
            if (!ItemCooldowns.tryUse(stack, STORM, player, 200, "ability.chronoarsenal.storm_call")) {
                return InteractionResultHolder.fail(stack);
            }
            stormCall(server, player);
        } else {
            player.getCooldowns().addCooldown(this, 15);
            chainLightning(server, player);
        }
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.success(stack);
    }

    private void chainLightning(ServerLevel level, Player player) {
        RayUtil.Hit hit = RayUtil.raycast(player, 24);
        Vec3 from = RayUtil.muzzle(player);
        FX.sound(level, from, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8f, 1.6f);

        if (hit.entity() == null) {
            FX.zigzag(level, ParticleTypes.ELECTRIC_SPARK, from, hit.pos());
            return;
        }

        Set<LivingEntity> struck = new HashSet<>();
        LivingEntity current = hit.entity();
        float damage = 9.0f;
        for (int jump = 0; jump < 6 && current != null; jump++) {
            Vec3 to = current.getBoundingBox().getCenter();
            FX.zigzag(level, ParticleTypes.ELECTRIC_SPARK, from, to);
            level.sendParticles(ParticleTypes.FLASH, to.x, to.y, to.z, 1, 0, 0, 0, 0);

            float dealt = current.isInWaterOrRain() ? damage * 1.5f : damage;
            current.invulnerableTime = 0;
            current.hurt(player.damageSources().playerAttack(player), dealt);
            current.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2)); // stunned

            struck.add(current);
            from = to;
            damage *= 0.8f;

            final Vec3 origin = to;
            current = level.getEntitiesOfClass(LivingEntity.class, new AABB(to, to).inflate(7),
                            e -> RayUtil.isFoe(player, e) && !struck.contains(e))
                    .stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(origin)))
                    .orElse(null);
        }
    }

    private void stormCall(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 pos = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(48)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.x, pos.y, pos.z);
            bolt.setCause((ServerPlayer) player);
            level.addFreshEntity(bolt);
        }
        // Three harmless "visual" bolts around it for drama
        for (int i = 0; i < 3; i++) {
            LightningBolt fx = EntityType.LIGHTNING_BOLT.create(level);
            if (fx != null) {
                double a = level.getRandom().nextDouble() * Math.PI * 2;
                fx.moveTo(pos.x + Math.cos(a) * 3, pos.y, pos.z + Math.sin(a) * 3);
                fx.setVisualOnly(true);
                level.addFreshEntity(fx);
            }
        }
        // Arcs leap from the impact to everything within 6 blocks
        Vec3 core = pos.add(0, 1, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(core, core).inflate(6),
                e -> RayUtil.isFoe(player, e))) {
            FX.zigzag(level, ParticleTypes.ELECTRIC_SPARK, core, e.getBoundingBox().getCenter());
            e.invulnerableTime = 0;
            e.hurt(player.damageSources().playerAttack(player), 6.0f);
        }
        player.displayClientMessage(Component.translatable("message.chronoarsenal.storm_call"), true);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "arc_caster");
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
