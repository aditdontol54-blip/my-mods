package com.danish.chronoarsenal.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

/**
 * Frozen in time: no movement, no AI, hangs in mid-air. Creepers stop fusing.
 * Mobs get their AI back when the effect ends.
 */
public class TemporalStasisEffect extends MobEffect {
    private static final String FROZEN_TAG = "chronoarsenal_frozen";

    public TemporalStasisEffect() {
        super(MobEffectCategory.HARMFUL, 0x7FFFF4);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "3f6b8a5e-1c2d-4e7f-9a0b-2c3d4e5f6a7b",
                -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;

        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof Mob mob) {
            if (!mob.isNoAi()) {
                mob.setNoAi(true);
                mob.getPersistentData().putBoolean(FROZEN_TAG, true);
            }
            mob.getNavigation().stop();
        }
        if (entity instanceof Creeper creeper) {
            creeper.setSwellDir(-1);
        }
        if (entity.tickCount % 4 == 0 && entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    3, entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.4, entity.getBbWidth() * 0.4, 0.01);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        if (entity instanceof Mob mob && mob.getPersistentData().getBoolean(FROZEN_TAG)) {
            mob.setNoAi(false);
            mob.getPersistentData().remove(FROZEN_TAG);
        }
    }
}
