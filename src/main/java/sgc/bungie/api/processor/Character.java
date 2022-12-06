/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.bungie.api.processor;

import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * @author chris hoppe
 */
public class Character {

    private final String UID;
    private final DestinyClassType classType;
    private final String dateLastPlayed;
    private final HashMap<Raid, RaidActivity> raidActivities = new HashMap<>();
    private final HashMap<Raid, RaidActivity> weeklyRaidActivities = new HashMap<>();
    private final HashMap<Mode, Integer> activitiesWithSGCMembersByMode = new HashMap<>();
    private int activitiesWithSGCMembersScore = 0;

    public Character(String UID, DestinyClassType classType, String dateLastPlayed) {
        this.UID = UID;
        this.classType = classType;
        this.dateLastPlayed = dateLastPlayed;
        Raid.getRaidsOrdered().forEach((Raid raid) -> {
            raidActivities.put(raid, new RaidActivity(raid));
        });
        Mode.validModesForCPOTW().forEach((mode) -> {
            activitiesWithSGCMembersByMode.put(mode, 0);
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

    public void addClearedActivitiesWithSGCMembers(GenericActivity activity) {
        if (activity != null && activity.getEarnedPoints() > 0) {
            activitiesWithSGCMembersScore += activity.getEarnedPoints();
            activitiesWithSGCMembersByMode.put(activity.getMODE(),
                    activitiesWithSGCMembersByMode.get(activity.getMODE()) + 1);
        }
    }

    public int getActivitiesWithSGCMembersScore() {
        return activitiesWithSGCMembersScore;
    }

    public int getActivitiesWithSGCMembersCount() {
        Integer total = activitiesWithSGCMembersByMode.values().stream()
                .collect(Collectors.summingInt(Integer::intValue));
        return total;
    }

    public HashMap<Mode, Integer> getActivitiesWithSGCMembersByMode() {
        return activitiesWithSGCMembersByMode;
    }

}
