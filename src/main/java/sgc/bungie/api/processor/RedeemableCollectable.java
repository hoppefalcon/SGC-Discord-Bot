/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.bungie.api.processor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chris hoppe
 */
public enum RedeemableCollectable {
    A_CLASSY_ORDER(CollectableType.EMBLEM, "YRC-C3D-YNC", "3282448382"),
    ADVENTUROUS_SPIRIT(CollectableType.EMBLEM, "9FY-KDD-PRT", "1737730014"),
    AIRLOCK_INVITATION(CollectableType.EMBLEM, "HN3-7K9-93G", "1822059434"),
    ARCHIVED(CollectableType.EMBLEM, "PTD-GKG-CVN", "1670619542"),
    BE_TRUE(CollectableType.EMBLEM, "ML3-FD4-ND9", "3215337910"),
    BULBUL_TARANG(CollectableType.EMBLEM, "A67-C7X-3GN", "3215337912"),
    COUNTDOWN_TO_CONVERGENCE(CollectableType.EMBLEM, "PHV-6LF-9CP", "3399891685"),
    CRUSHED_GAMMA(CollectableType.EMBLEM, "D97-YCX-7JK", "1939502752"),
    CRYONAUTICS(CollectableType.EMBLEM, "RA9-XPH-6KJ", "4137754755"),
    END_OF_THE_RAINBOW(CollectableType.TRANSMAT_EFFECT, "R9J-79M-J6C", "1009936720"),
    FOLDING_SPACE(CollectableType.EMBLEM, "3J9-AMM-7MG", "1670619543"),
    FUTURE_IN_SHADOW(CollectableType.EMBLEM, "7LV-GTK-T7J", "4137754752"),
    GALILEAN_EXCURSION(CollectableType.EMBLEM, "JYN-JAA-Y7D", "4137754753"),
    GONE_HOME(CollectableType.EMBLEM, "3CV-D6K-RD4", "1670619541"),
    HARMONIC_COMMENCEMENT(CollectableType.EMBLEM, "VXN-V3T-MRP", "1653841957"),
    HELIOTROPE_WARREN(CollectableType.EMBLEM, "L7T-CVV-3RD", "3332781245"),
    IN_URBE_INVENTA(CollectableType.EMBLEM, "XVK-RLA-RAM", "1822059429"),
    IN_VINO_MENDACIUM(CollectableType.EMBLEM, "J6P-9YH-LLP", "1822059428"),
    JADES_BURROW(CollectableType.EMBLEM, "TNN-DKM-6LG", "2040168511"),
    LIMINAL_NADIR(CollectableType.EMBLEM, "VA7-L7H-PNC", "3399891682"),
    LIMITLESS_HORIZON(CollectableType.EMBLEM, "XMY-G9M-6XH", "2056946094"),
    MSTART(CollectableType.EMBLEM, "JND-HLR-L69", "1670619538"),
    MYOPIA(CollectableType.EMBLEM, "FMM-44A-RKP", "1670619540"),
    NEON_MIRAGE(CollectableType.EMBLEM, "YAA-37T-FCN", "1822059431"),
    OUT_THE_AIRLOCK(CollectableType.EMBLEM, "L3P-XXR-GJ4", "802420309"),
    RAINBOW_CONNECTION(CollectableType.EMOTE, "TK7-D3P-FDF", "1063785104"),
    RISEN(CollectableType.EMBLEM, "THR-33A-YKC", "1687397126"),
    SCHRDINGERS_GUN(CollectableType.EMBLEM, "9LX-7YC-6TX", "1973057929"),
    SEQUENCE_FLOURISH(CollectableType.EMBLEM, "7D4-PKR-MD7", "3232115498"),
    SERAPHIMS_GAUNTLETS(CollectableType.EMBLEM, "XVX-DKJ-CVM", "2056946089"),
    SHADOWS_LIGHT(CollectableType.EMBLEM, "F99-KPX-NCF", "3399891683"),
    SNEER_OF_THE_ONI(CollectableType.EMBLEM, "6LJ-GH7-TPA", "3399891680"),
    STAGS_SPIRIT(CollectableType.EMBLEM, "T67-JXY-PH6", "1939502753"),
    SUNFLOWER(CollectableType.EMBLEM, "JVG-VNT-GGG", "2006613205"),
    TANGLED_WEB(CollectableType.EMBLEM, "PKH-JL6-L4R", "3299225966"),
    THE_VISIONARY(CollectableType.EMBLEM, "XFV-KHP-N97", "930889102"),
    VISIO_SPEI(CollectableType.EMBLEM, "993-H3H-M6K", "1822059430"),
    TIGRIS_FATI(CollectableType.EMBLEM, "6AJ-XFR-9ND", "802420307");

    public static final String BungieRedeemURL = "https://www.bungie.net/7/en/codes/redeem?token=";
    private final String code;
    private final String collectableHash;
    private final CollectableType collectableType;

    private RedeemableCollectable(CollectableType collectableType, String code, String collectableHash) {
        this.collectableType = collectableType;
        this.code = code;
        this.collectableHash = collectableHash;
    }

    public CollectableType getCollectableType() {
        return collectableType;
    }

    public String getCode() {
        return code;
    }

    public String getCollectableHash() {
        return collectableHash;
    }

    public static List<String> getAllCollectableHashes() {
        ArrayList<String> collectableHashes = new ArrayList<>();

        for (RedeemableCollectable rc : RedeemableCollectable.values()) {
            if (!rc.getCollectableType().equals(CollectableType.EMOTE)) {
                collectableHashes.add(rc.getCollectableHash());
            }
        }
        return collectableHashes;
    }

    public static List<String> getAllNonCollectableHashes() {
        ArrayList<String> nonCollectableHashes = new ArrayList<>();

        for (RedeemableCollectable rc : RedeemableCollectable.values()) {
            if (rc.getCollectableType().equals(CollectableType.EMOTE)) {
                nonCollectableHashes.add(rc.getCollectableHash());
            }
        }
        return nonCollectableHashes;
    }

    public static RedeemableCollectable getRedeemableCollectable(String hash) {
        for (RedeemableCollectable rc : RedeemableCollectable.values()) {
            if (rc.collectableHash.equals(hash)) {
                return rc;
            }
        }
        return null;
    }

}