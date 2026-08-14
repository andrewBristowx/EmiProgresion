package com.andrewbristowx.emiprogresion.story;

public enum StoryStage {
    NEW,
    STARTER_CHOSEN,
    PARCEL_RECEIVED,
    PARCEL_RETURNED,
    BROCK_DEFEATED;

    public boolean atLeast(StoryStage other) {
        return ordinal() >= other.ordinal();
    }
}
