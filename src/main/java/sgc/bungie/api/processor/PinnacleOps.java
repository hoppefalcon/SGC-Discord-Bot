package sgc.bungie.api.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum PinnacleOps {

    THE_WHISPER("The Whisper", Arrays.asList("576782083")),
    STARCROSSED("Starcrossed", Arrays.asList("1768099736")),
    KELLS_FALL("Kell's Fall", Arrays.asList("1948474391")),
    ENCORE("Encore", Arrays.asList("3120544689"));

    public final String name;
    private final List<String> validHashes;

    private PinnacleOps(String name, List<String> validHashes) {
        this.name = name;
        this.validHashes = validHashes;
    }

    public static PinnacleOps getPinnacleOp(String hash) {
        for (PinnacleOps p : PinnacleOps.values()) {
            if (p.getValidHashes().contains(hash)) {
                return p;
            }
        }
        return null;
    }

    public static List<String> getAllValidPinnacleOpsHashes() {
        List<String> hashes = new ArrayList<>();
        for (PinnacleOps p : PinnacleOps.values()) {
            hashes.addAll(p.getValidHashes());
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
