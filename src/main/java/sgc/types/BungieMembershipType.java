/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.types;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chris hoppe
 */
public enum BungieMembershipType {
    NONE("None", 0),
    XBOX("Xbox", 1),
    PSN("Playstation", 2),
    STEAM("Steam", 3),
    BLIZZARD("Blizzard", 4),
    STADIA("Stadia", 5),
    EGS("Egs", 6),
    DEMON("Demon", 10),
    GOLIATHGAME("Goliath Game", 20),
    BungieNext("Bungie Next", 254),
    ALL("All", -1);

    private final String name;
    private final int value;

    /**
     * @param name
     * @param value
     */
    private BungieMembershipType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the value
     */
    public int getValue() {
        return value;
    }

    public static BungieMembershipType getBungieMembershipType(int value) {
        for (BungieMembershipType type : BungieMembershipType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }

    public static List<BungieMembershipType> getOrderedList() {
        ArrayList<BungieMembershipType> list = new ArrayList<>();

        list.add(NONE);
        list.add(XBOX);
        list.add(PSN);
        list.add(STEAM);
        list.add(BLIZZARD);
        list.add(STADIA);
        list.add(EGS);
        list.add(DEMON);
        list.add(GOLIATHGAME);
        list.add(BungieNext);
        list.add(ALL);

        return list;
    }
}
