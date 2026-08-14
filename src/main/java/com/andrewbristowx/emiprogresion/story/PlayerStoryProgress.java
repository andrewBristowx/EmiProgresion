package com.andrewbristowx.emiprogresion.story;

import java.util.HashSet;
import java.util.Set;

public final class PlayerStoryProgress {
    public StoryStage stage = StoryStage.NEW;
    public String starter = "";
    public Set<String> flags = new HashSet<>();

    public void normalize() {
        if (stage == null) stage = StoryStage.NEW;
        if (starter == null) starter = "";
        if (flags == null) flags = new HashSet<>();
    }

    public boolean markOnce(String flag) {
        return flags.add(flag);
    }
}
