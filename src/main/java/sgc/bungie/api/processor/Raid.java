/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.bungie.api.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author chris hoppe
 */
public enum Raid {
    LAST_WISH("Last Wish", Arrays.asList("2214608157", "2214608156", "2122313384", "1661734046")),
    GARDEN_OF_SALVATION("Garden of Salvation",
            Arrays.asList("3845997235", "2659723068", "2497200493", "1042180643", "3458480158", "3823237780")),
    DEEP_STONE_CRYPT("Deep Stone Crypt", Arrays.asList("910380154", "3976949817")),
    VAULT_OF_GLASS("Vault of Glass",
            Arrays.asList("3881495763", "1485585878", "1681562271", "3711931140", "3022541210")),
    VOW_OF_THE_DISCIPLE("Vow of the Disciple",
            Arrays.asList("1441982566", "2906950631", "3889634515", "4156879541", "4217492330")),
    KINGS_FALL("King's Fall", Arrays.asList("1374392663", "1063970578", "2897223272", "2964135793", "3257594522")),
    ROOT_OF_NIGHTMARES("Root of Nightmares", Arrays.asList("2381413764", "1191701339", "2918919505")),
    CROTAS_END("Crota's End", Arrays.asList("156253568", "4179289725", "1507509200")),
    PANTHEON_ATRAKS_SOVEREIGN("The Pantheon: Atraks Sovereign", Arrays.asList("4169648179")),
    PANTHEON_ORYX_EXALTED("The Pantheon: Oryx Exalted", Arrays.asList("4169648176")),
    PANTHEON_RHULK_INDOMITABLE("The Pantheon: Rhulk Indomitable", Arrays.asList("4169648177")),
    PANTHEON_NEZAREC_SUBLIME("The Pantheon: Nezarec Sublime", Arrays.asList("4169648182")),
    SALVATIONS_EDGE("Salvation's Edge", Arrays.asList("2192826039", "1541433876"));

    public final String name;
    private final List<String> validHashes;

    private Raid(String name, List<String> validHashes) {
        this.name = name;
        this.validHashes = validHashes;
    }

    public static Raid getRaid(String hash) {
        for (Raid r : Raid.values()) {
            if (r.getValidHashes().contains(hash)) {
                return r;
            }
        }
        return null;
    }

    public static List<String> getAllValidRaidHashes() {
        List<String> hashes = new ArrayList<>();
        for (Raid r : Raid.values()) {
            hashes.addAll(r.getValidHashes());
        }
        return hashes;
    }

    public String getName() {
        return name;
    }

    public List<String> getValidHashes() {
        return validHashes;
    }

    public static List<Raid> getRaidsOrdered() {
        List<Raid> raids = new ArrayList<>();
        raids.add(SALVATIONS_EDGE);
        raids.add(CROTAS_END);
        raids.add(ROOT_OF_NIGHTMARES);
        raids.add(KINGS_FALL);
        raids.add(VOW_OF_THE_DISCIPLE);
        raids.add(VAULT_OF_GLASS);
        raids.add(DEEP_STONE_CRYPT);
        raids.add(GARDEN_OF_SALVATION);
        raids.add(LAST_WISH);
        raids.add(PANTHEON_ATRAKS_SOVEREIGN);
        raids.add(PANTHEON_ORYX_EXALTED);
        raids.add(PANTHEON_RHULK_INDOMITABLE);
        raids.add(PANTHEON_NEZAREC_SUBLIME);
        return raids;
    }
}
