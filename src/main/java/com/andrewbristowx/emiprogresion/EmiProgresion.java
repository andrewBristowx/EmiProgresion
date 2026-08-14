package com.andrewbristowx.emiprogresion;

import com.andrewbristowx.emiprogresion.command.EmiProgresionCommand;
import com.andrewbristowx.emiprogresion.config.EmiProgresionConfig;
import com.andrewbristowx.emiprogresion.network.StoryNetworking;
import com.andrewbristowx.emiprogresion.region.AdventureRegionService;
import com.andrewbristowx.emiprogresion.region.KantoMapInstaller;
import com.andrewbristowx.emiprogresion.story.StoryService;
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
        StoryNetworking.initialize();
        StoryService.initializeEvents();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EmiProgresionCommand.register(dispatcher)
        );
        ServerLifecycleEvents.SERVER_STARTING.register(KantoMapInstaller::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            AdventureRegionService.onServerStarted(server);
            StoryService.onServerStarted(server);
            if (EmiProgresionConfig.get().storyEnabled
                    && !EmiProgresionConfig.get().storyNpcSetupComplete) {
                var kanto = AdventureRegionService.getLevel(server, EmiProgresionConfig.get().kantoWorld);
                if (kanto != null && AdventureRegionService.hasWildKantoSignature(kanto)) {
                    StoryService.SetupResult result = StoryService.setupNpcs(server);
                    LOGGER.info("Automatic Kanto NPC setup: {}/{} - {}", result.spawned(), result.expected(), result.message());
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> StoryService.onServerStopping());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            AdventureRegionService.tick(server);
            StoryService.tick(server);
        });

        LOGGER.info("EmiProgresion 0.1.0-alpha.5.1 enabled: corrected RCT story NPC setup.");
    }
}
