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
    private final HashMap<Raid, Activity> activities = new HashMap<>();
    private final HashMap<Raid, Activity> weeklyActivities = new HashMap<>();

    public Character(String UID) {
        this.UID = UID;
        Raid.getRaidsOrdered().forEach((Raid raid) -> {activities.put(raid, new Activity(raid));
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
    public HashMap<Raid, Activity> getActivities() {
        return activities;
    }

    /**
     * @return the weeklyActivities
     */
    public HashMap<Raid, Activity> getWeeklyActivities() {
        return weeklyActivities;
    }

}
