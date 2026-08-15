package com.andrewbristowx.emiprogresion.story;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class PlayerStoryProgress {
    public StoryStage stage = StoryStage.NEW;
    public String starter = "";
    public Set<String> flags = new HashSet<>();
    public Map<String, Long> cooldowns = new HashMap<>();

    public void normalize() {
        if (stage == null) stage = StoryStage.NEW;
        if (starter == null) starter = "";
        if (flags == null) flags = new HashSet<>();
        if (cooldowns == null) cooldowns = new HashMap<>();
    }

    public boolean markOnce(String flag) {
        return flags.add(flag);
    }

    public long cooldownRemaining(String key, long now) {
        return Math.max(0L, cooldowns.getOrDefault(key, 0L) - now);
    }

    public void setCooldown(String key, long until) {
        cooldowns.put(key, until);
    }

    public int badges() {
        return stage.badges();
    }
}
