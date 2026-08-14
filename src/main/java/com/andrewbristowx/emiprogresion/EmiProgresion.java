package com.andrewbristowx.emiprogresion;

import com.andrewbristowx.emiprogresion.command.ProgressionCommand;
import com.andrewbristowx.emiprogresion.config.ProgressionConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EmiProgresion implements ModInitializer {
    public static final String MOD_ID = "emiprogresion";
    public static final Logger LOGGER = LoggerFactory.getLogger("EmiProgresion");

    @Override
    public void onInitialize() {
        ProgressionConfig.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ProgressionCommand.register(dispatcher)
        );
        LOGGER.info("EmiProgresion 0.1.0-alpha.1 enabled. Kanto planning core is active; no world generation or border changes are automatic yet.");
    }
}
