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
public enum Dungeon {
    SHATTERED_THRONE("Shattered Throne", Arrays.asList("2032534090", "1893059148")),
    PIT_OF_HERESY("Pit of Heresy",
            Arrays.asList("785700673", "785700678", "1375089621", "2559374368", "2559374374", "2559374375",
                    "2582501063")),
    PROPHECY("Prophecy",
            Arrays.asList("4148187374", "1077850348", "3637651331", "1788465402", "715153594", "3193125350")),
    GRASP_OF_Avarice("Grasp of Avarice", Arrays.asList("4078656646", "3774021532", "1112917203")),
    DUALITY("Duality", Arrays.asList("3012587626", "2823159265", "1668217731")),
    SPIRE_OF_THE_WATCHER("Spire of the Watcher",
            Arrays.asList("2296818662", "1801496203", "1262462921", "943878085", "1225969316", "3339002067",
                    "4046934917")),
    GHOSTS_OF_THE_DEEP("Ghosts of the Deep",
            Arrays.asList("2716998124", "313828469", "1094262727", "4190119662", "2961030534", "124340010")),
    WARLORDS_RUIN("Warlord's Ruin", Arrays.asList("2004855007", "2534833093")),
    VESPERS_HOST("Vesper's Host", Arrays.asList("300092127", "1915770060", "3492566689", "4293676253")),
    SUNDERED_DOCTRINE("Sundered Doctrine", Arrays.asList("247869137", "3521648250", "3834447244")),
    EQUILIBRIUM("Equilibrium", Arrays.asList("2727361621", "1754635208"));

    public final String name;
    private final List<String> validHashes;

    private Dungeon(String name, List<String> validHashes) {
        this.name = name;
        this.validHashes = validHashes;
    }

    public static Dungeon getDungeon(String hash) {
        for (Dungeon r : Dungeon.values()) {
            if (r.getValidHashes().contains(hash)) {
                return r;
            }
        }
        return null;
    }

    public static List<String> getAllValidDungeonHashes() {
        List<String> hashes = new ArrayList<>();
        for (Dungeon r : Dungeon.values()) {
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

    public static List<Dungeon> getDungeonsOrdered() {
        List<Dungeon> dungeons = new ArrayList<>();
        dungeons.add(SUNDERED_DOCTRINE);
        dungeons.add(VESPERS_HOST);
        dungeons.add(WARLORDS_RUIN);
        dungeons.add(GHOSTS_OF_THE_DEEP);
        dungeons.add(SPIRE_OF_THE_WATCHER);
        dungeons.add(DUALITY);
        dungeons.add(GRASP_OF_Avarice);
        dungeons.add(PROPHECY);
        dungeons.add(PIT_OF_HERESY);
        dungeons.add(SHATTERED_THRONE);
        return dungeons;
    }
}
