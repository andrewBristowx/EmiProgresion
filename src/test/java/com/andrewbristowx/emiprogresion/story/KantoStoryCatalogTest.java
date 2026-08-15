package com.andrewbristowx.emiprogresion.story;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KantoStoryCatalogTest {
    @Test
    void campaignContainsEightGymsAndCompleteLeague() {
        assertEquals(8, KantoStoryCatalog.gyms().size());
        assertEquals(5, KantoStoryCatalog.league().size());
        assertTrue(KantoStoryCatalog.league(5).orElseThrow().champion());
        assertEquals(8, StoryStage.CHAMPION_DEFEATED.badges());
    }

    @Test
    void everyStorySectionHasAuthoredContent() {
        assertTrue(KantoStoryCatalog.npcs().size() >= 20);
        assertTrue(KantoStoryCatalog.routeNames().size() >= 20);
        assertTrue(KantoStoryCatalog.routeNames().stream()
                .allMatch(route -> !KantoStoryCatalog.route(route).isEmpty()));
        assertTrue(KantoStoryCatalog.gyms().stream().allMatch(gym -> !gym.trainers().isEmpty()));
    }

    @Test
    void genericCharactersDoNotReuseTrainerSkins() {
        List<String> trainerIds = new ArrayList<>();
        KantoStoryCatalog.npcs().forEach(npc -> trainerIds.add(npc.trainerId()));
        KantoStoryCatalog.routeNames().forEach(route -> KantoStoryCatalog.route(route)
                .forEach(trainer -> trainerIds.add(trainer.trainerId())));
        KantoStoryCatalog.gyms().forEach(gym -> gym.trainers()
                .forEach(trainer -> trainerIds.add(trainer.trainerId())));

        Set<String> unique = new HashSet<>();
        List<String> repeated = trainerIds.stream().filter(id -> !unique.add(id)).toList();
        assertTrue(repeated.isEmpty(), "Repeated generic RCT trainer skins: " + repeated);
        assertFalse(trainerIds.contains("lady_selphy_01a3"));
    }

    @Test
    void gymStagesFormAForwardOnlyProgression() {
        KantoStoryCatalog.gyms().forEach(gym ->
                assertTrue(gym.resultStage().ordinal() > gym.requiredStage().ordinal()));
        for (int number = 1; number <= 8; number++) {
            assertEquals(number, KantoStoryCatalog.gym(number).orElseThrow().number());
        }
    }
}
