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
public class Member {

    private final String UID;
    private final String DisplayName;
    private final String bungieGlobalDisplayName;
    private final String bungieGlobalDisplayNameCode;
    private final String MemberType;
    private HashMap<String, Character> characters = new HashMap<>();

    public Member(String UID, String DisplayName, String MemberType, String bungieGlobalDisplayName,
            String bungieGlobalDisplayNameCode) {
        this.UID = UID;
        this.DisplayName = DisplayName;
        this.MemberType = MemberType;
        this.bungieGlobalDisplayName = bungieGlobalDisplayName;
        this.bungieGlobalDisplayNameCode = bungieGlobalDisplayNameCode;
    }

    public String getUID() {
        return UID;
    }

    public String getDisplayName() {
        return DisplayName;
    }

    public String getMemberType() {
        return MemberType;
    }

    public String getBungieGlobalDisplayName() {
        return bungieGlobalDisplayName;
    }

    public String getBungieGlobalDisplayNameCode() {
        return bungieGlobalDisplayNameCode;
    }

    public HashMap<String, Character> getCharacters() {
        return characters;
    }

    public HashMap<Raid, Integer> getRaidClears() {
        HashMap<Raid, Integer> raidClears = new HashMap<>();
        for (Raid r : Raid.values()) {
            raidClears.put(r, 0);
        }
        characters.values().forEach((c) -> {
            for (Raid r : Raid.values()) {
                Activity activity = c.getActivities().get(r);
                if (activity != null) {
                    int total = 0;
                    total += raidClears.get(r);
                    total += activity.getTotalClears();
                    raidClears.put(r, total);
                }
            }
        });
        return raidClears;
    }

    public HashMap<Raid, Integer> getXlRaidClears() {
        HashMap<Raid, Integer> raidClears = new HashMap<>();
        for (Raid r : Raid.values()) {
            raidClears.put(r, 0);
        }
        characters.values().forEach((c) -> {
            for (Raid r : Raid.values()) {
                Activity activity = c.getActivities().get(r);
                if (activity != null) {
                    int total = 0;
                    total += raidClears.get(r);
                    total += activity.getXlClears();
                    raidClears.put(r, total);
                }
            }
        });
        return raidClears;
    }

    public String getCombinedungieGlobalDisplayName() {
        return String.format("%s#%s", this.getBungieGlobalDisplayName(), this.getBungieGlobalDisplayNameCode());
    }
}
