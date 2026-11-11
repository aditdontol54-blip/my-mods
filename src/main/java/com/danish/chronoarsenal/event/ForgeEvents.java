package com.danish.chronoarsenal.event;

import com.danish.chronoarsenal.ChronoArsenal;
import com.danish.chronoarsenal.logic.RewindTracker;
import com.danish.chronoarsenal.logic.ScheduledEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChronoArsenal.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeEvents {
    private ForgeEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ScheduledEffects.tick();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            RewindTracker.record(event.player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        RewindTracker.forget(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ScheduledEffects.clear();
        RewindTracker.clearAll();
    }
}
