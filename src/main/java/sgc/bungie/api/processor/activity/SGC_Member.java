package sgc.bungie.api.processor.activity;

import sgc.types.SGC_Clan;

public class SGC_Member {
    private final SGC_Clan clan;
    private String discordDisplayName = "";
    private String discordID = "";
    private String bungieDisplayName = "";
    private boolean discordActivity = false;
    private boolean gameActivity = false;
    private boolean discordVoiceActivity = false;
    private int discordMessages7Days = 0;
    private double discordVoice7Days = 0.0;
    private int discordClanMessages7Days = 0;
    private double discordClanVoice7Days = 0.0;

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
    public String getDiscordID() {
        return discordID;
    }

    /**
     * Sets the Discord username of the member.
     *
     * @param discordID The Discord username to set.
     */
    public void setDiscordID(final String discordID) {
        this.discordID = discordID;
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

    public int getDiscordMessages7Days() {
        return discordMessages7Days;
    }

    public void setDiscordMessages7Days(int discordMessages7Days) {
        this.discordMessages7Days = discordMessages7Days;
    }

    public double getDiscordVoice7Days() {
        return discordVoice7Days;
    }

    public void setDiscordVoice7Days(double discordVoice7Days) {
        this.discordVoice7Days = discordVoice7Days;
    }

    public int getDiscordClanMessages7Days() {
        return discordClanMessages7Days;
    }

    public void setDiscordClanMessages7Days(int discordClanMessages7Days) {
        this.discordClanMessages7Days = discordClanMessages7Days;
    }

    public double getDiscordClanVoice7Days() {
        return discordClanVoice7Days;
    }

    public void setDiscordClanVoice7Days(double discordClanVoice7Days) {
        this.discordClanVoice7Days = discordClanVoice7Days;
    }

    public boolean isDiscordVoiceActivity() {
        return discordVoiceActivity;
    }

    public void setDiscordVoiceActivity(boolean discordVoiceActivity) {
        this.discordVoiceActivity = discordVoiceActivity;
    }

}