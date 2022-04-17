/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.sherpa.sheets;

/**
 * @author chris hoppe
 */
public enum DestinyClassType {
    TITAN("Titan", 0),
    HUNTER("Hunter", 1),
    WARLOCK("Warlock", 2),
    UNKNOWN("Unknown", 3);

    private final String name;
    private final int value;

    private DestinyClassType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%s | %d", name, value);
    }

    public static DestinyClassType getByValue(int asInt) {
        switch (asInt) {
            case 0:
                return TITAN;
            case 1:
                return HUNTER;
            case 2:
                return WARLOCK;
            case 3:
                return UNKNOWN;
        }
        return UNKNOWN;
    }

    public static DestinyClassType getByName(String asString) {
        switch (asString.toLowerCase()) {
            case "titan":
                return TITAN;
            case "hunter":
                return HUNTER;
            case "warlock":
                return WARLOCK;
            default:
                return UNKNOWN;
        }
    }

}
