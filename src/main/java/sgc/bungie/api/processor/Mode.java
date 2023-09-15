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
    NONE("None", 0, 1.0),
    STORY("Story", 2, 1.0),
    STRIKE("Strike", 3, 1.0),
    RAID("Raid", 4, 1.0),
    ALL_PVP("AllPvP", 5, 1.0),
    PATROL("Patrol", 6, 1.0),
    ALL_PVE("AllPvE", 7, 1.0),
    RESERVED_9("Reserved9", 9, 1.0),
    CONTROL("Control", 10, 1.0),
    RESERVED_11("Reserved11", 11, 1.0),
    CLASH("Clash", 12, 1.0),
    RESERVED_13("Reserved13", 13, 1.0),
    CRIMSON_DOUBLES("CrimsonDoubles", 15, 1.0),
    NIGHTFALL("Nightfall", 16, 1.0),
    HEROIC_NIGHTFALL("HeroicNightfall", 17, 1.0),
    ALLS_TRIKES("AllStrikes", 18, 1.0),
    IRON_BANNER("IronBanner", 19, 1.0),
    RESERVED_20("Reserved20", 20, 1.0),
    RESERVED_21("Reserved21", 21, 1.0),
    RESERVED_22("Reserved22", 22, 1.0),
    RESERVED_24("Reserved24", 24, 1.0),
    ALL_MAYHEM("AllMayhem", 25, 1.0),
    RESERVED_26("Reserved26", 26, 1.0),
    RESERVED_27("Reserved27", 27, 1.0),
    RESERVED_28("Reserved28", 28, 1.0),
    RESERVED_29("Reserved29", 29, 1.0),
    RESERVED_30("Reserved30", 30, 1.0),
    SUPREMACY("Supremacy", 31, 1.0),
    PRIVATE_MATCHES_ALL("PrivateMatchesAll", 32, 1.0),
    SURVIVAL("Survival", 37, 1.0),
    COUNTDOWN("Countdown", 38, 1.0),
    TRIALS_OF_THE_NINE("TrialsOfTheNine", 39, 1.0),
    SOCIAL("Social", 40, 1.0),
    TRIALS_COUNTDOWN("TrialsCountdown", 41, 1.0),
    TRIALS_SURVIVAL("TrialsSurvival", 42, 1.0),
    IRON_BANNER_CONTROL("IronBannerControl", 43, 1.0),
    IRON_BANNER_CLASH("IronBannerClash", 44, 1.0),
    IRON_BANNER_SUPREMACY("IronBannerSupremacy", 45, 1.0),
    SCORED_NIGHTFALL("ScoredNightfall", 46, 1.0),
    SCORED_HEROIC_NIGHTFALL("ScoredHeroicNightfall", 47, 1.0),
    RUMBLE("Rumble", 48, 1.0),
    ALL_DOUBLES("AllDoubles", 49, 1.0),
    DOUBLES("Doubles", 50, 1.0),
    PRIVATE_MATCHES_CLASH("PrivateMatchesClash", 51, 1.0),
    PRIVATE_MATCHES_CONTROL("PrivateMatchesControl", 52, 1.0),
    PRIVATE_MATCHES_SUPREMACY("PrivateMatchesSupremacy", 53, 1.0),
    PRIVATE_MATCHES_COUNTDOWN("PrivateMatchesCountdown", 54, 1.0),
    PRIVATE_MATCHES_SURVIVAL("PrivateMatchesSurvival", 55, 1.0),
    PRIVATE_MATCHES_MAYHEM("PrivateMatchesMayhem", 56, 1.0),
    PRIVATE_MATCHES_RUMBLE("PrivateMatchesRumble", 57, 1.0),
    HEROIC_ADVENTURE("HeroicAdventure", 58, 1.0),
    SHOWDOWN("Showdown", 59, 1.0),
    LOCKDOWN("Lockdown", 60, 1.0),
    SCORCHED("Scorched", 61, 1.0),
    SCORCHED_TEAM("ScorchedTeam", 62, 1.0),
    GAMBIT("Gambit", 63, 1.0),
    ALL_PVE_COMPETITIVE("AllPvECompetitive", 64, 1.0),
    BREAKTHROUGH("Breakthrough", 65, 1.0),
    BLACK_ARMORY_RUN("BlackArmoryRun", 66, 1.0),
    SALVAGE("Salvage", 67, 1.0),
    IRON_BANNER_SALVAGE("IronBannerSalvage", 68, 1.0),
    PVP_COMPETITIVE("PvPCompetitive", 69, 1.0),
    PVP_QUICKPLAY("PvPQuickplay", 70, 1.0),
    CLASH_QUICKPLAY("ClashQuickplay", 71, 1.0),
    CLASH_COMPETITIVE("ClashCompetitive", 72, 1.0),
    CONTROL_QUICKPLAY("ControlQuickplay", 73, 1.0),
    CONTROL_COMPETITIVE("ControlCompetitive", 74, 1.0),
    GAMBIT_PRIME("GambitPrime", 75, 1.0),
    RECKONING("Reckoning", 76, 1.0),
    MENAGERIE("Menagerie", 77, 1.0),
    VEX_OFFENSIVE("VexOffensive", 78, 1.0),
    NIGHTMARE_HUNT("NightmareHunt", 79, 1.0),
    ELIMINATION("Elimination", 80, 1.0),
    MOMENTUM("Momentum", 81, 1.0),
    DUNGEON("Dungeon", 82, 1.0),
    SUNDIAL("Sundial", 83, 1.0),
    TRIALS_OF_OSIRIS("TrialsOfOsiris", 84, 1.0),
    DARES("Dares", 85, 1.0),
    OFFENSIVE("Offensive", 86, 1.0),
    LOST_SECTOR("LostSector", 87, 1.0),
    RIFT("Rift", 88, 1.0),
    ZONE_CONTROL("ZoneControl", 89, 1.0),
    IRON_BANNER_RIFT("IronBannerRift", 90, 1.0),
    IRON_BANNER_ZONE_CONTROL("IronBannerZoneControl", 91, 1.0);

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

        list.add(Mode.NONE);

        list.add(Mode.PRIVATE_MATCHES_ALL);
        list.add(Mode.PRIVATE_MATCHES_CLASH);
        list.add(Mode.PRIVATE_MATCHES_CONTROL);
        list.add(Mode.PRIVATE_MATCHES_COUNTDOWN);
        list.add(Mode.PRIVATE_MATCHES_MAYHEM);
        list.add(Mode.PRIVATE_MATCHES_RUMBLE);
        list.add(Mode.PRIVATE_MATCHES_SUPREMACY);
        list.add(Mode.PRIVATE_MATCHES_SURVIVAL);

        list.add(Mode.PATROL);

        list.add(Mode.RESERVED_9);
        list.add(Mode.RESERVED_11);
        list.add(Mode.RESERVED_13);
        list.add(Mode.RESERVED_20);
        list.add(Mode.RESERVED_21);
        list.add(Mode.RESERVED_22);
        list.add(Mode.RESERVED_24);
        list.add(Mode.RESERVED_26);
        list.add(Mode.RESERVED_27);
        list.add(Mode.RESERVED_28);
        list.add(Mode.RESERVED_29);
        list.add(Mode.RESERVED_30);

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
