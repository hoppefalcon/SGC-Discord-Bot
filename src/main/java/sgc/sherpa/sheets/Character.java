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

    public Character(String UID) {
        this.UID = UID;
        activities.put(Raid.LAST_WISH, new Activity(Raid.LAST_WISH));
        activities.put(Raid.GARDEN_OF_SALVATION, new Activity(Raid.GARDEN_OF_SALVATION));
        activities.put(Raid.DEEP_STONE_CRYPT, new Activity(Raid.DEEP_STONE_CRYPT));
        activities.put(Raid.VAULT_OF_GLASS, new Activity(Raid.VAULT_OF_GLASS));
    }

    public String getUID() {
        return UID;
    }

    public HashMap<Raid, Activity> getActivities() {
        return activities;
    }

}
