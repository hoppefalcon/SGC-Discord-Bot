/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.bungie.api.processor;

import java.util.HashMap;
import java.util.stream.Collectors;

import sgc.types.DestinyClassType;
import sgc.types.Dungeon;
import sgc.types.Mode;
import sgc.types.Raid;

/**
 * @author chris hoppe
 */
public class Character {

    private final String UID;
    private final DestinyClassType classType;
    private final String dateLastPlayed;
    private final HashMap<Raid, RaidActivity> raidActivities = new HashMap<>();
    private final HashMap<Raid, RaidActivity> weeklyRaidActivities = new HashMap<>();
    private final HashMap<Dungeon, DungeonActivity> dungeonActivities = new HashMap<>();
    private final HashMap<Dungeon, DungeonActivity> weeklyDungeonActivities = new HashMap<>();
    private final HashMap<Mode, Integer> cpotwActivitiesWithSGCMembersByMode = new HashMap<>();
    private final HashMap<Mode, Boolean> potwCompletionsByMode = new HashMap<>();
    private final HashMap<Raid, Boolean> potwCompletionsByRaid = new HashMap<>();
    private final HashMap<Dungeon, Boolean> potwCompletionsByDungeon = new HashMap<>();
    private int activitiesWithSGCMembersScore = 0;

    public Character(String UID, DestinyClassType classType, String dateLastPlayed) {
        this.UID = UID;
        this.classType = classType;
        this.dateLastPlayed = dateLastPlayed;
        Raid.getRaidsOrdered().forEach((Raid raid) -> {
            raidActivities.put(raid, new RaidActivity(raid));
        });
        Dungeon.getDungeonsOrdered().forEach((Dungeon dungeon) -> {
            dungeonActivities.put(dungeon, new DungeonActivity(dungeon));
        });
        Mode.validModesForCPOTW().forEach((mode) -> {
            cpotwActivitiesWithSGCMembersByMode.put(mode, 0);
        });
        Mode.validModesForPOTW().forEach((mode) -> {
            potwCompletionsByMode.put(mode, false);
        });
        Raid.getRaidsOrdered().forEach((raid) -> {
            potwCompletionsByRaid.put(raid, false);
        });
        Dungeon.getDungeonsOrdered().forEach((dungeon) -> {
            potwCompletionsByDungeon.put(dungeon, false);
        });
    }

    /**
     * @return the uID
     */
    public String getUID() {
        return UID;
    }

    /**
     * @return the classType
     */
    public DestinyClassType getClassType() {
        return classType;
    }

    /**
     * @return the dateLastPlayed
     */
    public String getDateLastPlayed() {
        return dateLastPlayed;
    }

    /**
     * @return the activities
     */
    public HashMap<Raid, RaidActivity> getRaidActivities() {
        return raidActivities;
    }

    /**
     * @return the weeklyActivities
     */
    public HashMap<Raid, RaidActivity> getWeeklyRaidActivities() {
        return weeklyRaidActivities;
    }

    /**
     * @return the activities
     */
    public HashMap<Dungeon, DungeonActivity> getDungeonActivities() {
        return dungeonActivities;
    }

    /**
     * @return the weeklyActivities
     */
    public HashMap<Dungeon, DungeonActivity> getWeeklyDungeonActivities() {
        return weeklyDungeonActivities;
    }

    public void addClearedActivitiesWithSGCMembers(GenericActivity activity) {
        if (activity != null && activity.getEarnedPoints() > 0) {
            activitiesWithSGCMembersScore += activity.getEarnedPoints();
            cpotwActivitiesWithSGCMembersByMode.put(activity.getMODE(),
                    cpotwActivitiesWithSGCMembersByMode.get(activity.getMODE()) + 1);
        }
    }

    public int getActivitiesWithSGCMembersScore() {
        return activitiesWithSGCMembersScore;
    }

    public int getActivitiesWithSGCMembersCount() {
        Integer total = cpotwActivitiesWithSGCMembersByMode.values().stream()
                .collect(Collectors.summingInt(Integer::intValue));
        return total;
    }

    public HashMap<Mode, Integer> getCpotwActivitiesWithSGCMembersByMode() {
        return cpotwActivitiesWithSGCMembersByMode;
    }

    public void addCompletedMode(Mode mode) {
        potwCompletionsByMode.put(mode, true);
    }

    public void addCompletedRaid(Raid raid) {
        potwCompletionsByRaid.put(raid, true);
    }

    public void addCompletedDungeon(Dungeon dungeon) {
        potwCompletionsByDungeon.put(dungeon, true);
    }

    public HashMap<Mode, Boolean> getPotwCompletionsByMode() {
        return potwCompletionsByMode;
    }

    public HashMap<Raid, Boolean> getPotwCompletionsByRaid() {
        return potwCompletionsByRaid;
    }

    public HashMap<Dungeon, Boolean> getPotwCompletionsByDungeon() {
        return potwCompletionsByDungeon;
    }

    public void zeroOut() {
        Raid.getRaidsOrdered().forEach((Raid raid) -> {
            raidActivities.put(raid, new RaidActivity(raid));
        });
        Dungeon.getDungeonsOrdered().forEach((Dungeon dungeon) -> {
            dungeonActivities.put(dungeon, new DungeonActivity(dungeon));
        });
        Mode.validModesForCPOTW().forEach((mode) -> {
            cpotwActivitiesWithSGCMembersByMode.put(mode, 0);
        });
        Mode.validModesForPOTW().forEach((mode) -> {
            potwCompletionsByMode.put(mode, false);
        });
        Raid.getRaidsOrdered().forEach((raid) -> {
            potwCompletionsByRaid.put(raid, false);
        });
        Dungeon.getDungeonsOrdered().forEach((dungeon) -> {
            potwCompletionsByDungeon.put(dungeon, false);
        });
    }
}
