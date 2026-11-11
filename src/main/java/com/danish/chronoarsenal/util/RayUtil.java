package com.danish.chronoarsenal.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Hitscan helpers shared by the weapons. */
public final class RayUtil {
    private RayUtil() {}

    /** entity is null when nothing living was hit; pos is where the ray stopped. */
    public record Hit(LivingEntity entity, Vec3 pos, boolean hitBlock) {}

    /** Something a weapon is allowed to hurt: not the user, not their pets, not armor stands. */
    public static boolean isFoe(Player owner, Entity e) {
        if (!(e instanceof LivingEntity living)) return false;
        if (e == owner || !living.isAlive() || e.isSpectator()) return false;
        if (e instanceof ArmorStand) return false;
        if (e instanceof TamableAnimal pet && pet.isOwnedBy(owner)) return false;
        return true;
    }

    public static Hit raycast(Player shooter, double range) {
        Level level = shooter.level();
        Vec3 start = shooter.getEyePosition();
        Vec3 end = start.add(shooter.getLookAngle().scale(range));

        BlockHitResult blockHit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
        Vec3 stop = hitBlock ? blockHit.getLocation() : end;

        LivingEntity best = null;
        Vec3 bestPos = stop;
        double bestDist = start.distanceToSqr(stop);

        AABB search = new AABB(start, stop).inflate(1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, search, e -> isFoe(shooter, e))) {
            Optional<Vec3> clip = e.getBoundingBox().inflate(0.3).clip(start, stop);
            if (clip.isPresent()) {
                double d = start.distanceToSqr(clip.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = e;
                    bestPos = clip.get();
                }
            }
        }
        return new Hit(best, bestPos, best == null && hitBlock);
    }

    /** Where beams should visually start: slightly below and in front of the eyes. */
    public static Vec3 muzzle(Player player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.8)).add(0, -0.2, 0);
    }
}
