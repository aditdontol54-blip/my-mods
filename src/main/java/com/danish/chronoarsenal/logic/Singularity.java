package com.danish.chronoarsenal.logic;

import com.danish.chronoarsenal.util.FX;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** A 3-second black hole: pulls everything in, crushes what's at the core, then implodes. */
public class Singularity implements ScheduledEffects.Effect {
    private static final int LIFETIME = 60;
    private static final double PULL_RADIUS = 10.0;
    private static final double IMPLODE_RADIUS = 6.0;

    private final ServerLevel level;
    private final Vec3 center;
    private final UUID ownerId;
    private int age = 0;

    public Singularity(ServerLevel level, Vec3 center, UUID ownerId) {
        this.level = level;
        this.center = center;
        this.ownerId = ownerId;
    }

    private DamageSource source() {
        Player owner = level.getPlayerByUUID(ownerId);
        return owner != null ? level.damageSources().indirectMagic(owner, owner) : level.damageSources().magic();
    }

    private boolean affects(Entity e) {
        if (e.getUUID().equals(ownerId) || e.isSpectator() || e instanceof ArmorStand) return false;
        if (e instanceof Player p && p.isCreative()) return false;
        return true;
    }

    @Override
    public boolean tick() {
        age++;

        // Visuals: a spiral collapsing inwards plus a dark core
        double r = 3.0 * (1.0 - (age % 20) / 20.0) + 0.3;
        for (int k = 0; k < 8; k++) {
            double a = age * 0.45 + k * (Math.PI * 2 / 8);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    center.x + Math.cos(a) * r, center.y + Math.sin(age * 0.2 + k) * 0.4, center.z + Math.sin(a) * r,
                    1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 4, 0.15, 0.15, 0.15, 0.01);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 2, 0.3, 0.3, 0.3, 0.0);
        if (age % 20 == 1) {
            FX.sound(level, center, SoundEvents.BEACON_AMBIENT, 2.0f, 0.5f);
        }

        // Pull everything nearby (items and XP orbs too)
        AABB area = new AABB(center, center).inflate(PULL_RADIUS);
        for (Entity e : level.getEntities((Entity) null, area, this::affects)) {
            Vec3 toCenter = center.subtract(e.position().add(0, e.getBbHeight() * 0.5, 0));
            double dist = toCenter.length();
            if (dist > PULL_RADIUS || dist < 0.4) continue;
            double strength = 0.12 + 0.28 * (1.0 - dist / PULL_RADIUS);
            e.setDeltaMovement(e.getDeltaMovement().scale(0.55).add(toCenter.normalize().scale(strength)));
            e.hurtMarked = true;
            e.fallDistance = 0;
        }

        // Crush whatever is at the core
        if (age % 10 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(2.5), this::affects)) {
                e.invulnerableTime = 0;
                e.hurt(source(), 3.0f);
            }
        }

        if (age >= LIFETIME) {
            implode();
            return false;
        }
        return true;
    }

    private void implode() {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        FX.sphere(level, ParticleTypes.END_ROD, center, 2.5, 60);
        FX.sound(level, center, SoundEvents.GENERIC_EXPLODE, 2.0f, 0.6f);
        FX.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.7f);

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(IMPLODE_RADIUS), this::affects)) {
            if (e.position().distanceTo(center) > IMPLODE_RADIUS) continue;
            e.invulnerableTime = 0;
            e.hurt(source(), 12.0f);
            Vec3 out = e.position().subtract(center);
            Vec3 flat = new Vec3(out.x, 0, out.z).normalize();
            e.setDeltaMovement(flat.x * 1.8, 0.7, flat.z * 1.8);
            e.hurtMarked = true;
        }
    }
}
