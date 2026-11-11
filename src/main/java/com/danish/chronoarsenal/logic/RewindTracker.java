package com.danish.chronoarsenal.logic;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Remembers where every player was over the last 5 seconds, for the Chrono Blade's Rewind. */
public final class RewindTracker {
    private RewindTracker() {}

    private record Snapshot(ResourceKey<Level> dimension, double x, double y, double z,
                            float yRot, float xRot, float health) {}

    private static final int INTERVAL = 5;      // ticks between snapshots
    private static final int MAX_SNAPSHOTS = 20; // 20 x 5 ticks = 5 seconds

    private static final Map<UUID, ArrayDeque<Snapshot>> HISTORY = new HashMap<>();

    public static void record(Player player) {
        if (player.tickCount % INTERVAL != 0 || !player.isAlive()) return;
        ArrayDeque<Snapshot> history = HISTORY.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        history.addLast(new Snapshot(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), player.getHealth()));
        while (history.size() > MAX_SNAPSHOTS) history.removeFirst();
    }

    /** Sends the player back to their oldest snapshot in this dimension. Restores lost health. */
    public static boolean rewind(ServerPlayer player) {
        ArrayDeque<Snapshot> history = HISTORY.get(player.getUUID());
        if (history == null || history.isEmpty()) return false;

        Snapshot target = null;
        for (Snapshot s : history) {
            if (s.dimension().equals(player.level().dimension())) {
                target = s;
                break;
            }
        }
        if (target == null) return false;

        player.stopRiding();
        player.connection.teleport(target.x(), target.y(), target.z(), target.yRot(), target.xRot());
        if (target.health() > player.getHealth()) player.setHealth(target.health());
        player.clearFire();
        player.fallDistance = 0;
        history.clear();
        return true;
    }

    public static void forget(Player player) {
        HISTORY.remove(player.getUUID());
    }

    public static void clearAll() {
        HISTORY.clear();
    }
}
