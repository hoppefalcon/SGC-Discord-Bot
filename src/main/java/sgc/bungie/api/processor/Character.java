/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.bungie.api.processor;

import java.util.HashMap;

/**
 * @author chris hoppe
 */
public class Character {

    private final String UID;
    private final DestinyClassType classType;
    private final HashMap<Raid, RaidActivity> raidActivities = new HashMap<>();
    private final HashMap<Raid, RaidActivity> weeklyRaidActivities = new HashMap<>();
    private int activitiesWithSGCMembersScore = 0;
    private int activitiesWithSGCMembersCount = 0;

    public Character(String UID, DestinyClassType classType) {
        this.UID = UID;
        this.classType = classType;
        Raid.getRaidsOrdered().forEach((Raid raid) -> {
            raidActivities.put(raid, new RaidActivity(raid));
        });
    }

    /**
     * @return the uID
     */
    public String getUID() {
        return UID;
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
            activitiesWithSGCMembersCount++;
        }
    }

    public int getActivitiesWithSGCMembersScore() {
        return activitiesWithSGCMembersScore;
    }

    public int getActivitiesWithSGCMembersCount() {
        return activitiesWithSGCMembersCount;
    }

    public DestinyClassType getClassType() {
        return classType;
    }

}