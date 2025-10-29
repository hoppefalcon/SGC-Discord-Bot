package sgc.bungie.api.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum FireteamOps {

    EH_WARRIOR("Empire Hunt: The Warrior", Arrays.asList("47604735")),
    EH_TECHNOCRAT("Empire Hunt: The Technocrat", Arrays.asList("1019762304")),
    EH_DARK_PRIESTESS("Empire Hunt: The Dark Priestess", Arrays.asList("2963591247")),
    BG_DELVE("Battleground: Delve", Arrays.asList("112486683")),
    BG_ORACLE("Battleground: Oracle", Arrays.asList("1191481675")),
    BG_CONDUIT("Battleground: Conduit", Arrays.asList("1655726829")),
    BG_FOOTHOLD("Battleground: Foothold", Arrays.asList("1977716129")),
    THE_COIL("The Coil", Arrays.asList("218823224")),
    SAVATHUNS_SPIRE("Savathûn's Spire", Arrays.asList("2751501766")),
    OS_WIDOWS_COURT("Widow's Court", Arrays.asList("732075595")),
    OS_MIDTROWN("Midtown", Arrays.asList("849675207")),
    LIMINALITY("Liminality", Arrays.asList("1136877027")),
    THE_INVERTED_SPIRE("The Inverted Spire", Arrays.asList("1509484286")),
    PROVING_GROUNDS("Proving Grounds", Arrays.asList("3061857094")),
    THE_DEVILS_LAIR("The Devils' Lair", Arrays.asList("3610118907")),
    THE_GLASSWAY("The Glassway", Arrays.asList("4145089682"));

    public final String name;
    private final List<String> validHashes;

    private FireteamOps(String name, List<String> validHashes) {
        this.name = name;
        this.validHashes = validHashes;
    }

    public static FireteamOps getFireteamOp(String hash) {
        for (FireteamOps f : FireteamOps.values()) {
            if (f.getValidHashes().contains(hash)) {
                return f;
            }
        }
        return null;
    }

    public static List<String> getAllValidFireteamOpsHashes() {
        List<String> hashes = new ArrayList<>();
        for (FireteamOps f : FireteamOps.values()) {
            hashes.addAll(f.getValidHashes());
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
