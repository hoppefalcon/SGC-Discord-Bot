package sgc.bungie.api.processor.activity;

import java.util.HashMap;

import sgc.SGC_Clan;

public class SGC_Member {
    private final SGC_Clan clan;
    private String discordDisplayName = "";
    private String bungieDisplayName = "";
    private boolean discord_activity = false;
    private boolean game_activity = false;
    private HashMap<String, Integer> discord_message_counts = new HashMap<>();

    /**
     * @param clan
     */
    public SGC_Member(final SGC_Clan clan) {
        this.clan = clan;
    }

    /**
     * @return the clan
     */
    public SGC_Clan getClan() {
        return clan;
    }

    /**
     * @return the discordDisplayName
     */
    public String getDiscordDisplayName() {
        return discordDisplayName;
    }

    /**
     * @param discordDisplayName the discordDisplayName to set
     */
    public void setDiscordDisplayName(final String discordDisplayName) {
        this.discordDisplayName = discordDisplayName;
    }

    /**
     * @return the bungieDisplayName
     */
    public String getBungieDisplayName() {
        return bungieDisplayName;
    }

    /**
     * @param bungieDisplayName the bungieDisplayName to set
     */
    public void setBungieDisplayName(final String bungieDisplayName) {
        this.bungieDisplayName = bungieDisplayName;
    }

    /**
     * @return the discord_activity
     */
    public boolean isDiscord_activity() {
        return discord_activity;
    }

    /**
     * @param discord_activity the discord_activity to set
     */
    public void setDiscord_activity(final boolean discord_activity) {
        this.discord_activity = discord_activity;
    }

    /**
     * @return the game_activity
     */
    public boolean isGame_activity() {
        return game_activity;
    }

    /**
     * @param game_activity the game_activity to set
     */
    public void setGame_activity(final boolean game_activity) {
        this.game_activity = game_activity;
    }

    /**
     * @param bungieDisplayName the bungieDisplayName to compare with the
     *                          discordDisplayName
     * @return if they are the same user
     */
    public boolean isSameMember(final String bungieDisplayName) {
        return this.discordDisplayName.toLowerCase().endsWith(bungieDisplayName.toLowerCase());
    }

    /**
     * @return the discord_message_counts
     */
    public HashMap<String, Integer> getDiscord_message_counts() {
        return discord_message_counts;
    }
}
