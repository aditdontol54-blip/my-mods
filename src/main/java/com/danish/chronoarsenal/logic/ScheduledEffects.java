package com.danish.chronoarsenal.logic;

import java.util.ArrayList;
import java.util.List;

/** Tiny server-side scheduler for effects that last several ticks (singularities, echo hits). */
public final class ScheduledEffects {
    private ScheduledEffects() {}

    public interface Effect {
        /** @return true to keep running next tick, false when finished */
        boolean tick();
    }

    private static final List<Effect> ACTIVE = new ArrayList<>();
    private static final List<Effect> PENDING = new ArrayList<>();

    public static void add(Effect effect) {
        PENDING.add(effect);
    }

    public static void delay(int ticks, Runnable action) {
        add(new Effect() {
            private int remaining = ticks;

            @Override
            public boolean tick() {
                if (--remaining <= 0) {
                    action.run();
                    return false;
                }
                return true;
            }
        });
    }

    public static void tick() {
        ACTIVE.addAll(PENDING);
        PENDING.clear();
        ACTIVE.removeIf(effect -> {
            try {
                return !effect.tick();
            } catch (Exception e) {
                return true; // never let one broken effect crash the server
            }
        });
    }

    public static void clear() {
        ACTIVE.clear();
        PENDING.clear();
    }
}
