package sgc.bungie.api.processor.activity;

import java.util.HashMap;

import sgc.SGC_Clan;

public class SGC_Member {
    private final SGC_Clan clan;
    private String discordDisplayName = "";
    private String discordUserName = "";
    private String bungieDisplayName = "";
    private boolean discordActivity = false;
    private boolean gameActivity = false;
    private HashMap<String, Integer> discordMessageCounts = new HashMap<>();

    /**
     * Constructs an instance of SGC_Member with the specified clan.
     *
     * @param clan The clan to which the member belongs.
     */
    public SGC_Member(final SGC_Clan clan) {
        this.clan = clan;
    }

    /**
     * Returns the clan of the member.
     *
     * @return The clan of the member.
     */
    public SGC_Clan getClan() {
        return clan;
    }

    /**
     * Returns the Discord display name of the member.
     *
     * @return The Discord display name of the member.
     */
    public String getDiscordDisplayName() {
        return discordDisplayName;
    }

    /**
     * Sets the Discord display name of the member.
     *
     * @param discordDisplayName The Discord display name to set.
     */
    public void setDiscordDisplayName(final String discordDisplayName) {
        this.discordDisplayName = discordDisplayName;
    }

    /**
     * Returns the Discord username of the member.
     *
     * @return The Discord username of the member.
     */
    public String getDiscordUserName() {
        return discordUserName;
    }

    /**
     * Sets the Discord username of the member.
     *
     * @param discordUserName The Discord username to set.
     */
    public void setDiscordUserName(final String discordUserName) {
        this.discordUserName = discordUserName;
    }

    /**
     * Returns the Bungie display name of the member.
     *
     * @return The Bungie display name of the member.
     */
    public String getBungieDisplayName() {
        return bungieDisplayName;
    }

    /**
     * Sets the Bungie display name of the member.
     *
     * @param bungieDisplayName The Bungie display name to set.
     */
    public void setBungieDisplayName(final String bungieDisplayName) {
        this.bungieDisplayName = bungieDisplayName;
    }

    /**
     * Returns the Discord activity status of the member.
     *
     * @return The Discord activity status of the member.
     */
    public boolean isDiscordActivity() {
        return discordActivity;
    }

    /**
     * Sets the Discord activity status of the member.
     *
     * @param discordActivity The Discord activity status to set.
     */
    public void setDiscordActivity(final boolean discordActivity) {
        this.discordActivity = discordActivity;
    }

    /**
     * Returns the game activity status of the member.
     *
     * @return The game activity status of the member.
     */
    public boolean isGameActivity() {
        return gameActivity;
    }

    /**
     * Sets the game activity status of the member.
     *
     * @param gameActivity The game activity status to set.
     */
    public void setGameActivity(final boolean gameActivity) {
        this.gameActivity = gameActivity;
    }

    /**
     * Compares the Bungie display name with the Discord display name to check if
     * they belong to the same member.
     *
     * @param bungieDisplayName The Bungie display name to compare with the Discord
     *                          display name.
     * @return true if the Bungie display name and Discord display name belong to
     *         the same member, false otherwise.
     */
    public boolean isSameMember(final String bungieDisplayName) {
        String[] split = this.discordDisplayName.toLowerCase().split("[|]");
        if (split.length == 2) {
            return split[1].trim().equals(bungieDisplayName.toLowerCase());
        }
        return false;
    }

    /**
     * Returns the message counts for the member in Discord.
     *
     * @return The message counts for the member in Discord.
     */
    public HashMap<String, Integer> getDiscordMessageCounts() {
        return discordMessageCounts;
    }
}