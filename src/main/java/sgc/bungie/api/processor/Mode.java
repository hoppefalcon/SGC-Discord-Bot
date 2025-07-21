/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.bungie.api.processor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chris hoppe
 */
public enum Mode {
    ALL("All", 0, 1.0),
    STORY("Story", 2, 1.0),
    STRIKE("Strike", 3, 1.0),
    RAID("Raid", 4, 2.0),
    CRUCIBLE("Crucible", 5, 1.0),
    EXPLORE("Explore", 6, 1.0),
    PVE("PvE", 7, 1.0),
    CONTROL("Control", 10, 1.0),
    CLASH("Clash", 12, 1.0),
    CRIMSON_DOUBLES("Crimson Doubles", 15, 1.0),
    NIGHTFALL_STRIKES("Nightfall Strikes", 16, 1.0),
    PRESTIGE_NIGHTFALL("Prestige Nightfall", 17, 1.0),
    STRIKES("Strikes", 18, 1.0),
    IRON_BANNER("Iron Banner", 19, 1.0),
    MAYHEM("Mayhem", 25, 1.0),
    SUPREMACY("Supremacy", 31, 1.0),
    PRIVATE_MATCHES("Private Matches", 32, 1.0),
    SURVIVAL("Survival", 37, 1.0),
    COUNTDOWN("Countdown", 38, 1.0),
    TRIALS_OF_THE_NINE("Trials of the Nine", 39, 1.0),
    SOCIAL("Social", 40, 1.0),
    TRIALS_OF_THE_NINE_COUNTDOWN("Trials of the Nine Countdown", 41, 1.0),
    TRIALS_OF_THE_NINE_SURVIVAL("Trials of the Nine Survival", 42, 1.0),
    IRON_BANNER_CONTROL("Iron Banner Control", 43, 1.0),
    IRON_BANNER_CLASH("Iron Banner Clash", 44, 1.0),
    IRON_BANNER_SUPREMACY("Iron Banner Supremacy", 45, 1.0),
    SCORED_NIGHTFALL_STRIKES("Scored Nightfall Strikes", 46, 1.0),
    SCORED_PRESTIGE_NIGHTFALL("Scored Prestige Nightfall", 47, 1.0),
    RUMBLE("Rumble", 48, 1.0),
    ALL_DOUBLES("All Doubles", 49, 1.0),
    DOUBLES("Doubles", 50, 1.0),
    PRIVATE_MATCHES_CLASH("Private Matches Clash", 51, 1.0),
    PRIVATE_MATCHES_CONTROL("Private Matches Control", 52, 1.0),
    PRIVATE_MATCHES_SUPREMACY("Private Matches Supremacy", 53, 1.0),
    PRIVATE_MATCHES_COUNTDOWN("Private Matches Countdown", 54, 1.0),
    PRIVATE_MATCHES_SURVIVAL("Private Matches Survival", 55, 1.0),
    PRIVATE_MATCHES_MAYHEM("Private Matches Mayhem", 56, 1.0),
    PRIVATE_MATCHES_RUMBLE("Private Matches Rumble", 57, 1.0),
    HEROIC_ADVENTURE("Heroic Adventure", 58, 1.0),
    SHOWDOWN("Showdown", 59, 1.0),
    LOCKDOWN("Lockdown", 60, 1.0),
    SCORCHED("Scorched", 61, 1.0),
    TEAM_SCORCHED("Team Scorched", 62, 1.0),
    GAMBIT("Gambit", 63, 1.0),
    COMPETITIVE_CO_OP("Competitive Co-Op", 64, 1.0),
    BREAKTHROUGH("Breakthrough", 65, 1.0),
    FORGE("Forge", 66, 1.0),
    SALVAGE("Salvage", 67, 1.0),
    IRON_BANNER_SALVAGE("Iron Banner Salvage", 68, 1.0),
    COMPETITIVE_PVP("Competitive PvP", 69, 1.0),
    QUICKPLAY_PVP("Quickplay PvP", 70, 1.0),
    CLASH_QUICKPLAY("Clash: Quickplay", 71, 1.0),
    CLASH_COMPETITIVE("Clash: Competitive", 72, 1.0),
    CONTROL_QUICKPLAY("Control: Quickplay", 73, 1.0),
    CONTROL_COMPETITIVE("Control: Competitive", 74, 1.0),
    GAMBIT_PRIME("Gambit Prime", 75, 1.0),
    THE_RECKONING("The Reckoning", 76, 1.0),
    THE_MENAGERIE("The Menagerie", 77, 1.0),
    VEX_OFFENSIVE("Vex Offensive", 78, 1.0),
    NIGHTMARE_HUNT("Nightmare Hunt", 79, 1.0),
    ELIMINATION("Elimination", 80, 1.0),
    MOMENTUM("Momentum", 81, 1.0),
    DUNGEON("Dungeon", 82, 2.0),
    THE_SUNDIAL("The Sundial", 83, 1.0),
    TRIALS_OF_OSIRIS("Trials of Osiris", 84, 1.0),
    DARES_OF_ETERNITY("Dares of Eternity", 85, 1.0),
    OFFENSIVE("Offensive", 86, 1.0),
    LOST_SECTOR("Lost Sector", 87, 1.0),
    RIFT("Rift", 88, 1.0),
    ZONE_CONTROL("Zone Control", 89, 1.0),
    IRON_BANNER_RIFT("Iron Banner Rift", 90, 1.0),
    IRON_BANNER_ZONE_CONTROL("Iron Banner Zone Control", 91, 1.0),
    RELIC("Relic", 92, 1.0);

    public final String name;
    private final int value;
    private final double weeklyActivityWeight;

    private Mode(String name, int value, double weeklyActivityWeight) {
        this.name = name;
        this.value = value;
        this.weeklyActivityWeight = weeklyActivityWeight;
    }

    public static Mode getFromValue(int value) {
        for (Mode mode : Mode.values()) {
            if (mode.getValue() == value) {
                return mode;
            }
        }
        return null;
    }

    public static Mode getFromName(String name) {
        for (Mode mode : Mode.values()) {
            if (mode.getName().equals(name)) {
                return mode;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public double getWeeklyActivityWeight() {
        return weeklyActivityWeight;
    }

    public static List<Mode> invalidModesForCPOTW() {
        ArrayList<Mode> list = new ArrayList<>();

        list.add(Mode.ALL);

        list.add(Mode.PRIVATE_MATCHES);
        list.add(Mode.PRIVATE_MATCHES_RUMBLE);
        /*
         * list.add(Mode.PRIVATE_MATCHES_CLASH);
         * list.add(Mode.PRIVATE_MATCHES_CONTROL);
         * list.add(Mode.PRIVATE_MATCHES_COUNTDOWN);
         * list.add(Mode.PRIVATE_MATCHES_MAYHEM);
         * list.add(Mode.PRIVATE_MATCHES_SUPREMACY);
         * list.add(Mode.PRIVATE_MATCHES_SURVIVAL);
         */
        list.add(Mode.EXPLORE);
        list.add(Mode.RUMBLE);

        return list;
    }

    public static List<Mode> validModesForCPOTW() {
        ArrayList<Mode> validList = new ArrayList<>();

        List<Mode> invalidModesForPOTW = invalidModesForCPOTW();

        for (Mode mode : Mode.values()) {
            if (!invalidModesForPOTW.contains(mode)) {
                validList.add(mode);
            }
        }

        validList.sort((m1, m2) -> {
            return Integer.compare(m1.value, m2.value);
        });

        return validList;
    }

    public static List<Mode> validModesForPOTW() {
        ArrayList<Mode> validList = new ArrayList<>();

        List<Mode> invalidModesForPOTW = invalidModesForCPOTW();

        for (Mode mode : Mode.values()) {
            if (!invalidModesForPOTW.contains(mode) || mode.equals(RAID) || mode.equals(DUNGEON)) {
                validList.add(mode);
            }
        }

        validList.sort((m1, m2) -> {
            return Integer.compare(m1.value, m2.value);
        });

        return validList;
    }

    public static List<Integer> validModeValuesForCPOTW() {
        ArrayList<Integer> validList = new ArrayList<>();

        List<Mode> validModesForCPOTW = validModesForCPOTW();

        for (Mode mode : validModesForCPOTW) {
            validList.add(mode.getValue());
        }

        return validList;
    }
}
