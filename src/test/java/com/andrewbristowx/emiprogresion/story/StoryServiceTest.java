package com.andrewbristowx.emiprogresion.story;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
