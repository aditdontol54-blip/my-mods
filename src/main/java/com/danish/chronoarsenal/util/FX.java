package com.danish.chronoarsenal.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Server-side particle and sound helpers. */
public final class FX {
    private FX() {}

    public static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double step) {
        Vec3 diff = to.subtract(from);
        double len = diff.length();
        if (len < 1.0E-4) return;
        Vec3 stepVec = diff.scale(step / len);
        Vec3 cur = from;
        for (double t = 0; t <= len; t += step) {
            level.sendParticles(particle, cur.x, cur.y, cur.z, 1, 0, 0, 0, 0);
            cur = cur.add(stepVec);
        }
    }

    /** A jagged lightning-style line. */
    public static void zigzag(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to) {
        RandomSource r = level.getRandom();
        int segments = Math.max(2, (int) (from.distanceTo(to) / 1.2));
        Vec3 prev = from;
        for (int i = 1; i <= segments; i++) {
            Vec3 point = from.add(to.subtract(from).scale(i / (double) segments));
            if (i < segments) {
                point = point.add((r.nextDouble() - 0.5) * 0.8, (r.nextDouble() - 0.5) * 0.8, (r.nextDouble() - 0.5) * 0.8);
            }
            line(level, particle, prev, point, 0.2);
            prev = point;
        }
    }

    public static void sphere(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        RandomSource r = level.getRandom();
        for (int i = 0; i < count; i++) {
            double u = r.nextDouble() * 2 - 1;
            double theta = r.nextDouble() * Math.PI * 2;
            double s = Math.sqrt(1 - u * u);
            level.sendParticles(particle,
                    center.x + radius * s * Math.cos(theta),
                    center.y + radius * u,
                    center.z + radius * s * Math.sin(theta),
                    1, 0, 0, 0, 0);
        }
    }

    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2 * i) / count;
            level.sendParticles(particle,
                    center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius,
                    1, 0, 0, 0, 0);
        }
    }

    public static void sound(Level level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
