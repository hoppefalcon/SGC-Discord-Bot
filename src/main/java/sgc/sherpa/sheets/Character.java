/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.sherpa.sheets;

import java.util.HashMap;

/**
 * @author chris hoppe
 */
public class Character {

    final String UID;
    private final HashMap<Raid, RaidActivity> raidActivities = new HashMap<>();
    private final HashMap<Raid, RaidActivity> weeklyRaidActivities = new HashMap<>();
    private final HashMap<String, GenericActivity> activitiesWithSGCMembers = new HashMap<>();

    public Character(String UID) {
        this.UID = UID;
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
        activitiesWithSGCMembers.put(activity.getUID(), activity);
    }

    public HashMap<String, GenericActivity> getClearedActivitiesWithSGCMembers() {
        return activitiesWithSGCMembers;
    }

    public void clearActivitiesWithSGCMembers() {
        activitiesWithSGCMembers.clear();
    }
}
