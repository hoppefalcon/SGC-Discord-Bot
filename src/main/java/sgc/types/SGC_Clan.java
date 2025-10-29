package sgc.types;

/**
 * Enum representing the clans in the SGC Discord Bot.
 */
public enum SGC_Clan {
    APEX("456910898838241280", "3076620"),
    // BOOP("456911177625108492", "3063489", Platform.PC),
    // CATS("908058949914329148", "3884528", Platform.PC),
    // DAWN("1020330250124660856", "4327418", Platform.XBOX),
    DEVL("1020330447466680371", "4327536"),
    DGEN("732790473474965510", "3100797"),
    // DISC("633029229184942128", "3949151", Platform.PC),
    FURY("732803519282806824", "3915247"),
    IX("466111356043657228", "3019103"),
    KOTR("1020330276670427217", "4327587"),
    LGIN("1020330473781731398", "4327584"),
    // MYHM("TBD", "3008645", Platform.PC),
    OMEN("1020330108092960849", "4327434"),
    // REAP("732803510697197569", "3087185", Platform.PC),
    RISE("732790465988132865", "3095868"),
    SENT("1020330421231288440", "4327575"),
    // SGN("732778914673590333", "2820714"),
    SHOT("732790471730003978", "3070603"),
    SLS("456910851337748480", "2801315"),
    // SOL("1020330210454929438", "4418635", Platform.XBOX),
    // SPAR("1020330311822876713", "4327542"),
    STRM("591686875165622303", "3795604"),
    VII("732795268222812301", "3007121"),
    BSTN("1020330056331034675", "4327389"),
    WOLF("1020330174190989392", "4327464"),
    WRTH("732779974091735132", "3090996");

    public final String Discord_Role_ID;
    public final String Bungie_ID;

    /**
     * Constructs a clan with the specified Discord Role ID, Bungie ID, and Primary
     * Platform.
     *
     * @param Discord_Role_ID the Discord Role ID of the clan
     * @param Bungie_ID       the Bungie ID of the clan
     */
    private SGC_Clan(String Discord_Role_ID, String Bungie_ID) {
        this.Discord_Role_ID = Discord_Role_ID;
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
     * @param discordRoleID the Discord Role ID
     * @return the clan associated with the Discord Role ID, or null if not found
     */
    public static SGC_Clan getClanByRoleId(String discordRoleID) {
        for (SGC_Clan clan : SGC_Clan.values()) {
            if (clan.Discord_Role_ID.equals(discordRoleID)) {
                return clan;
            }
        }
        return null;
    }
}
