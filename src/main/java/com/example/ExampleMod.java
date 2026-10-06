package com.example;
import com.example.bogatyr.CopperBogatyrHandler;
import com.example.maxim.CleanMaximHandler;
import com.example.subaru.SubaruHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getPlayerManager().getPlayerList().forEach(player -> {
                SubaruHandler.tickSubaru(player);
                CopperBogatyrHandler.tick(player);
                CleanMaximHandler.tick(player);
            });
        });
    }
}