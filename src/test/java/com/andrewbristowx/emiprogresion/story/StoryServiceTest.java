package com.andrewbristowx.emiprogresion.story;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryServiceTest {
    @Test
    void buildsRctPersistentTrainerCommandWithoutFragileNbt() {
        String command = StoryService.storyTrainerCommand("professor_oak_00c8", 154, 78, 150);
        assertEquals("rctmod trainer summon_persistent professor_oak_00c8 154 78 150", command);
        assertFalse(command.contains("{"));
    }

    @Test
    void buildsDirectRctEntityFallbackWithTrainerIdAndPersistence() {
        String command = StoryService.storyTrainerFallbackCommand("kanto_brock", 22, 127, -1294);
        assertEquals("summon rctmod:trainer 22 127 -1294 {TrainerId:\"kanto_brock\",Persistent:1b}", command);
    }

    @Test
    void legacyCleanupTargetsOnlyRctTrainerEntities() {
        assertTrue(StoryService.isRctTrainerType("rctmod:trainer"));
        assertFalse(StoryService.isRctTrainerType("minecraft:villager"));
        assertFalse(StoryService.isRctTrainerType("rctmod:pokeball"));
    }

    @Test
    void buildsTransientStagingAndPersistentUnregisterCommands() {
        assertEquals("rctmod trainer summon kanto_brock 0 64 1250",
                StoryService.storyTrainerTransientCommand("kanto_brock", 0, 64, 1250));
        UUID uuid = UUID.fromString("8bfb53f3-5de2-42dd-9edc-cd307615405b");
        assertEquals("rctmod trainer unregister_persistent " + uuid,
                StoryService.storyTrainerUnregisterCommand(uuid));
    }
}
