package sgc.types;

/**
 * Enum representing the clans in the SGC Discord Bot.
 */
public enum SGC_Clan {
    APEX("Shrouded Apex", "3076620"),
    // BOOP("456911177625108492", "3063489", Platform.PC),
    // CATS("908058949914329148", "3884528", Platform.PC),
    // DAWN("1020330250124660856", "4327418", Platform.XBOX),
    DEVL("Shrouded Devils", "4327536"),
    DGEN("Shrouded Degenerates", "3100797"),
    // DISC("633029229184942128", "3949151", Platform.PC),
    FURY("Shrouded Fury", "3915247"),
    IX("Shrouded IX", "3019103"),
    KOTR("Shrouded Knights", "4327587"),
    LGIN("Shrouded Legion", "4327584"),
    // MYHM("TBD", "3008645", Platform.PC),
    OMEN("Shrouded Omens", "4327434"),
    // REAP("732803510697197569", "3087185", Platform.PC),
    RISE("Shrouded Phoenix", "3095868"),
    SENT("Shrouded Sentinels", "4327575"),
    // SGN("732778914673590333", "2820714"),
    SHOT("Shrouded Outlaws", "3070603"),
    SLS("Shrouded Souls", "2801315"),
    // SOL("1020330210454929438", "4418635", Platform.XBOX),
    // SPAR("1020330311822876713", "4327542"),
    STRM("Shrouded Storm", "3795604"),
    VII("Shrouded VII", "3007121"),
    BSTN("Shrouded Bastion", "4327389"),
    WOLF("Shrouded Wolves", "4327464"),
    WRTH("Shrouded Wraiths", "3090996");

    public final String Clan_Name;
    public final String Bungie_ID;

    /**
     * Constructs a clan with the specified Discord Role ID, Bungie ID, and Primary
     * Platform.
     *
     * @param Clan_Name the Discord Role ID of the clan
     * @param Bungie_ID the Bungie ID of the clan
     */
    private SGC_Clan(String Clan_Name, String Bungie_ID) {
        this.Clan_Name = Clan_Name;
        this.Bungie_ID = Bungie_ID;
    }

    /**
     * Returns the clan associated with the given Bungie ID.
     *
     * @param bungie_id the Bungie ID
     * @return the clan associated with the Bungie ID, or null if not found
     */
    public static SGC_Clan getClanByBungieId(String bungie_id) {
        for (SGC_Clan clan : SGC_Clan.values()) {
            if (clan.Bungie_ID.equals(bungie_id)) {
                return clan;
            }
        }
        return null;
    }

    /**
     * Returns the clan associated with the given Discord Role ID.
     *
     * @param Clan_Name the Discord Role ID
     * @return the clan associated with the Discord Role ID, or null if not found
     */
    public static SGC_Clan getClanByName(String Clan_Name) {
        for (SGC_Clan clan : SGC_Clan.values()) {
            if (clan.Clan_Name.equals(Clan_Name)) {
                return clan;
            }
        }
        return null;
    }
}
