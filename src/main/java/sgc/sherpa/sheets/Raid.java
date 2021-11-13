/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.sherpa.sheets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author chris hoppe
 */
public enum Raid {
    LAST_WISH("Last Wish", Arrays.asList("2214608157", "2214608156", "2122313384", "1661734046")),
    GARDEN_OF_SALVATION("Garden of Salvation", Arrays.asList("3845997235", "2659723068", "3458480158", "2497200493")),
    DEEP_STONE_CRYPT("Deep Stone Crypt", Arrays.asList("910380154", "3976949817")),
    VAULT_OF_GLASS("Vault of Glass", Arrays.asList("3881495763", "1485585878", "1681562271", "3711931140"));

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
}
