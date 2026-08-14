package com.andrewbristowx.emiprogresion;

import com.andrewbristowx.emiprogresion.command.EmiProgresionCommand;
import com.andrewbristowx.emiprogresion.config.EmiProgresionConfig;
import com.andrewbristowx.emiprogresion.region.AdventureRegionService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EmiProgresion implements ModInitializer {
    public static final String MOD_ID = "emiprogresion";
    public static final Logger LOGGER = LoggerFactory.getLogger("EmiProgresion");

    @Override
    public void onInitialize() {
        EmiProgresionConfig.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EmiProgresionCommand.register(dispatcher)
        );
        ServerLifecycleEvents.SERVER_STARTED.register(AdventureRegionService::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(AdventureRegionService::tick);

        LOGGER.info("EmiProgresion 0.1.0-alpha.3 enabled: cleaned Wild Kanto map-test mode.");
    }
}
