package com.andrewbristowx.emiprogresion.story;

public enum StoryStage {
    NEW,
    STARTER_CHOSEN,
    PARCEL_RECEIVED,
    PARCEL_RETURNED,
    BROCK_DEFEATED,
    MOUNT_MOON_CLEARED,
    BILL_HELPED,
    MISTY_DEFEATED,
    SS_ANNE_CLEARED,
    SURGE_DEFEATED,
    ROCKET_HIDEOUT_CLEARED,
    ERIKA_DEFEATED,
    POKEMON_TOWER_CLEARED,
    KOGA_DEFEATED,
    SILPH_CO_CLEARED,
    SABRINA_DEFEATED,
    CINNABAR_KEY_FOUND,
    BLAINE_DEFEATED,
    GIOVANNI_DEFEATED,
    VICTORY_ROAD_CLEARED,
    LORELEI_DEFEATED,
    BRUNO_DEFEATED,
    AGATHA_DEFEATED,
    LANCE_DEFEATED,
    CHAMPION_DEFEATED;

    public boolean atLeast(StoryStage other) {
        return ordinal() >= other.ordinal();
    }

    public int badges() {
        int badges = 0;
        if (atLeast(BROCK_DEFEATED)) badges++;
        if (atLeast(MISTY_DEFEATED)) badges++;
        if (atLeast(SURGE_DEFEATED)) badges++;
        if (atLeast(ERIKA_DEFEATED)) badges++;
        if (atLeast(KOGA_DEFEATED)) badges++;
        if (atLeast(SABRINA_DEFEATED)) badges++;
        if (atLeast(BLAINE_DEFEATED)) badges++;
        if (atLeast(GIOVANNI_DEFEATED)) badges++;
        return badges;
    }
}
