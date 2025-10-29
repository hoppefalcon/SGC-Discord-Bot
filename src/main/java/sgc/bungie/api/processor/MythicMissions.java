package sgc.bungie.api.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum MythicMissions {

    FALLOW("Fallow", Arrays.asList("251203635")),
    SATURNISM("Saturnism", Arrays.asList("1031141960")),
    THE_MESSAGE("The Message", Arrays.asList("1350796201")),
    QUARANTINE("Quarantine", Arrays.asList("1459303703")),
    MORPHOLOGY("Morphology", Arrays.asList("1633298726")),
    CALCULUS("Calculus", Arrays.asList("2083729881")),
    GOUGE("Gouge", Arrays.asList("2163004628")),
    NOSTOS("Nostos", Arrays.asList("2337450948")),
    COMMENCEMENT("Commencement", Arrays.asList("2406687437")),
    TRANSIENT("Transient", Arrays.asList("2552742883")),
    DISRUPTION("Disruption", Arrays.asList("2765528836")),
    CHARGE("Charge", Arrays.asList("3491522835")),
    CRITICALITY("Criticality", Arrays.asList("3864101700")),
    THE_INVITATION("The Invitation", Arrays.asList("4031602889"));

    public final String name;
    private final List<String> validHashes;

    private MythicMissions(String name, List<String> validHashes) {
        this.name = name;
        this.validHashes = validHashes;
    }

    public static MythicMissions getMythicMission(String hash) {
        for (MythicMissions f : MythicMissions.values()) {
            if (f.getValidHashes().contains(hash)) {
                return f;
            }
        }
        return null;
    }

    public static List<String> getAllValidMythicMissionsHashes() {
        List<String> hashes = new ArrayList<>();
        for (MythicMissions f : MythicMissions.values()) {
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
