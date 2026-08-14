package com.andrewbristowx.emiprogresion.story;

import java.util.List;

public record DialogueState(
        String id,
        String speaker,
        String portrait,
        List<String> pages,
        List<Choice> choices,
        boolean allowSkip,
        boolean allowExit
) {
    public record Choice(String id, String label, String detail) {}
}
