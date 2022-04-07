/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.sherpa.sheets;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chris hoppe
 */
public enum Mode {
    NONE("None", 0),
    STORY("Story", 2),
    STRIKE("Strike", 3),
    RAID("Raid", 4),
    ALL_PVP("AllPvP", 5),
    PATROL("Patrol", 6),
    ALL_PVE("AllPvE", 7),
    RESERVED_9("Reserved9", 9),
    CONTROL("Control", 10),
    RESERVED_11("Reserved11", 11),
    CLASH("Clash", 12),
    RESERVED_13("Reserved13", 13),
    CRIMSON_DOUBLES("CrimsonDoubles", 15),
    NIGHTFALL("Nightfall", 16),
    HEROIC_NIGHTFALL("HeroicNightfall", 17),
    ALLS_TRIKES("AllStrikes", 18),
    IRON_BANNER("IronBanner", 19),
    RESERVED_20("Reserved20", 20),
    RESERVED_21("Reserved21", 21),
    RESERVED_22("Reserved22", 22),
    RESERVED_24("Reserved24", 24),
    ALL_MAYHEM("AllMayhem", 25),
    RESERVED_26("Reserved26", 26),
    RESERVED_27("Reserved27", 27),
    RESERVED_28("Reserved28", 28),
    RESERVED_29("Reserved29", 29),
    RESERVED_30("Reserved30", 30),
    SUPREMACY("Supremacy", 31),
    PRIVATE_MATCHES_ALL("PrivateMatchesAll", 32),
    SURVIVAL("Survival", 37),
    COUNTDOWN("Countdown", 38),
    TRIALS_OF_THE_NINE("TrialsOfTheNine", 39),
    SOCIAL("Social", 40),
    TRIALS_COUNTDOWN("TrialsCountdown", 41),
    TRIALS_SURVIVAL("TrialsSurvival", 42),
    IRON_BANNER_CONTROL("IronBannerControl", 43),
    IRON_BANNER_CLASH("IronBannerClash", 44),
    IRON_BANNER_SUPREMACY("IronBannerSupremacy", 45),
    SCORED_NIGHTFALL("ScoredNightfall", 46),
    SCORED_HEROIC_NIGHTFALL("ScoredHeroicNightfall", 47),
    RUMBLE("Rumble", 48),
    ALL_DOUBLES("AllDoubles", 49),
    DOUBLES("Doubles", 50),
    PRIVATE_MATCHES_CLASH("PrivateMatchesClash", 51),
    PRIVATE_MATCHES_CONTROL("PrivateMatchesControl", 52),
    PRIVATE_MATCHES_SUPREMACY("PrivateMatchesSupremacy", 53),
    PRIVATE_MATCHES_COUNTDOWN("PrivateMatchesCountdown", 54),
    PRIVATE_MATCHES_SURVIVAL("PrivateMatchesSurvival", 55),
    PRIVATE_MATCHES_MAYHEM("PrivateMatchesMayhem", 56),
    PRIVATE_MATCHES_RUMBLE("PrivateMatchesRumble", 57),
    HEROIC_ADVENTURE("HeroicAdventure", 58),
    SHOWDOWN("Showdown", 59),
    LOCKDOWN("Lockdown", 60),
    SCORCHED("Scorched", 61),
    SCORCHED_TEAM("ScorchedTeam", 62),
    GAMBIT("Gambit", 63),
    ALL_PVE_COMPETITIVE("AllPvECompetitive", 64),
    BREAKTHROUGH("Breakthrough", 65),
    BLACK_ARMORY_RUN("BlackArmoryRun", 66),
    SALVAGE("Salvage", 67),
    IRON_BANNER_SALVAGE("IronBannerSalvage", 68),
    PVP_COMPETITIVE("PvPCompetitive", 69),
    PVP_QUICKPLAY("PvPQuickplay", 70),
    CLASH_QUICKPLAY("ClashQuickplay", 71),
    CLASH_COMPETITIVE("ClashCompetitive", 72),
    CONTROL_QUICKPLAY("ControlQuickplay", 73),
    CONTROL_COMPETITIVE("ControlCompetitive", 74),
    GAMBIT_PRIME("GambitPrime", 75),
    RECKONING("Reckoning", 76),
    MENAGERIE("Menagerie", 77),
    VEX_OFFENSIVE("VexOffensive", 78),
    NIGHTMARE_HUNT("NightmareHunt", 79),
    ELIMINATION("Elimination", 80),
    MOMENTUM("Momentum", 81),
    DUNGEON("Dungeon", 82),
    SUNDIAL("Sundial", 83),
    TRIALS_OF_OSIRIS("TrialsOfOsiris", 84),
    DARES("Dares", 85),
    OFFENSIVE("Offensive", 86),
    LOST_SECTOR("LostSector", 87);

    public final String name;
    private final int value;

    private Mode(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public static List<Integer> invalidModesForPOTW() {
        ArrayList<Integer> list = new ArrayList<>();

        list.add(Mode.NONE.getValue());

        list.add(Mode.PRIVATE_MATCHES_ALL.getValue());
        list.add(Mode.PRIVATE_MATCHES_CLASH.getValue());
        list.add(Mode.PRIVATE_MATCHES_CONTROL.getValue());
        list.add(Mode.PRIVATE_MATCHES_COUNTDOWN.getValue());
        list.add(Mode.PRIVATE_MATCHES_MAYHEM.getValue());
        list.add(Mode.PRIVATE_MATCHES_RUMBLE.getValue());
        list.add(Mode.PRIVATE_MATCHES_SUPREMACY.getValue());
        list.add(Mode.PRIVATE_MATCHES_SURVIVAL.getValue());

        list.add(Mode.PATROL.getValue());

        list.add(Mode.RESERVED_9.getValue());
        list.add(Mode.RESERVED_11.getValue());
        list.add(Mode.RESERVED_13.getValue());
        list.add(Mode.RESERVED_20.getValue());
        list.add(Mode.RESERVED_21.getValue());
        list.add(Mode.RESERVED_22.getValue());
        list.add(Mode.RESERVED_24.getValue());
        list.add(Mode.RESERVED_26.getValue());
        list.add(Mode.RESERVED_27.getValue());
        list.add(Mode.RESERVED_28.getValue());
        list.add(Mode.RESERVED_29.getValue());
        list.add(Mode.RESERVED_30.getValue());

        return list;
    }
}
