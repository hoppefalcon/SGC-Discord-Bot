package sgc;

public enum SGC_Clan {
    ANRC("456910898838241280", "3076620", Platform.PC),
    BOOP("456911177625108492", "3063489", Platform.PC),
    CATS("908058949914329148", "3884528", Platform.PC),
    DAWN("1020330250124660856", "4327418", Platform.XBOX),
    DEVL("1020330447466680371", "4327536", Platform.PSN),
    DGEN("732790473474965510", "3100797", Platform.PC),
    DISC("633029229184942128", "3949151", Platform.PC),
    FURY("732803519282806824", "3915247", Platform.PC),
    IX("466111356043657228", "3019103", Platform.PC),
    KOTR("1020330276670427217", "4327587", Platform.PSN),
    LGIN("1020330473781731398", "4327584", Platform.PSN),
    // MYHM("TBD", "3008645", Platform.PC),
    OMEN("1020330108092960849", "4327434", Platform.XBOX),
    REAP("732803510697197569", "3087185", Platform.PC),
    SENT("1020330421231288440", "4327575", Platform.PSN),
    SGN("732778914673590333", "2820714", Platform.PC),
    SHOT("732790471730003978", "3070603", Platform.PC),
    SIVA("732790465988132865", "3095868", Platform.PC),
    SLS("456910851337748480", "2801315", Platform.PC),
    SOL("1020330210454929438", "4418635", Platform.XBOX),
    SPAR("1020330311822876713", "4327542", Platform.PSN),
    STRM("591686875165622303", "3795604", Platform.PC),
    VII("732795268222812301", "3007121", Platform.PC),
    WARD("1020330056331034675", "4327389", Platform.XBOX),
    WOLF("1020330174190989392", "4327464", Platform.XBOX),
    WRTH("732779974091735132", "3090996", Platform.PC);

    public final String Discord_Role_ID;
    public final String Bungie_ID;
    public final Platform Primary_Platform;

    private SGC_Clan(String Discord_Role_ID, String Bungie_ID, Platform Primary_Platform) {
        this.Discord_Role_ID = Discord_Role_ID;
        this.Bungie_ID = Bungie_ID;
        this.Primary_Platform = Primary_Platform;
    }

    public static Platform getClansPrimaryPlatform(String bungie_id) {
        for (SGC_Clan clan : SGC_Clan.values()) {
            if (clan.Bungie_ID.equals(bungie_id)) {
                return clan.Primary_Platform;
            }
        }
        return null;
    }

    public static SGC_Clan getGetClanByRoleId(String discordRoleID) {
        for (SGC_Clan clan : SGC_Clan.values()) {
            if (clan.Discord_Role_ID.equals(discordRoleID)) {
                return clan;
            }
        }
        return null;
    }

}
