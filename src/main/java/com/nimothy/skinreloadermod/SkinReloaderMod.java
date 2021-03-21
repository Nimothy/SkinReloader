package com.nimothy.skinreloadermod;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SkinReloaderMod.MOD_ID)
public class SkinReloaderMod
{
    public static final String MOD_ID = "skin_reloader";

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public SkinReloaderMod() {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
