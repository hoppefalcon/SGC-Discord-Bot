package sgc.bungie.api.processor.activity;

import java.awt.Color;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.slf4j.Logger;

import com.google.api.services.sheets.v4.model.ValueRange;

import sgc.bungie.api.processor.RaidReportTool;
import sgc.discord.bot.BotApplication;
import sgc.discord.infographics.GoogleDriveUtil;
import sgc.types.SGC_Clan;

public class ActivityReportTool {

    private static final Logger LOGGER = BotApplication.getLogger();

    private static DiscordApi API = null;

    public static void setDiscordAPI(DiscordApi api) {
        API = api;
    }

    /**
     * Runs the activity sheets update process.
     */
    public static void runActivitySheets() {

        try {
            RaidReportTool.resourceLock.lock();
            LOGGER.info("Starting the SGC Activity sheet update at " +
                    ZonedDateTime.now(BotApplication.ZID).format(BotApplication.DATE_TIME_FORMATTER));

            sendLogMessage("Starting the SGC Activity sheet update at " +
                    ZonedDateTime.now(BotApplication.ZID).format(BotApplication.DATE_TIME_FORMATTER));

            HashMap<SGC_Clan, ArrayList<SGC_Member>> members = initializeMembers();
            getAllClansDiscordActivity(members);

            getAllClansGameActivity(members);
            GoogleDriveUtil.writeActivityToGoogleSheet(members);

            sendLogMessage("Completed the SGC Activity sheet update at " +
                    ZonedDateTime.now(BotApplication.ZID).format(BotApplication.DATE_TIME_FORMATTER));

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            sendErrorMessage("An Error occurred while running the SGC Activity sheet update at " +
                    ZonedDateTime.now(BotApplication.ZID).format(BotApplication.DATE_TIME_FORMATTER));

        } finally {
            RaidReportTool.resourceLock.unlock();
        }
    }

    /**
     * Initializes the members map with empty lists for each clan.
     *
     * @return Initialized members map.
     */
    private static HashMap<SGC_Clan, ArrayList<SGC_Member>> initializeMembers() {
        HashMap<SGC_Clan, ArrayList<SGC_Member>> members = new HashMap<>();
        for (SGC_Clan clan : SGC_Clan.values()) {
            members.put(clan, new ArrayList<>());
        }
        return members;
    }

    /**
     * Retrieves Discord activity for all clans and updates the member information.
     *
     * @param members A HashMap containing clans as keys and their members as
     *                values.
     */
    private static void getAllClansDiscordActivity(HashMap<SGC_Clan, ArrayList<SGC_Member>> members) {
        LOGGER.info("Processing the Discord Activity");

        ValueRange communityDiscordActivityData = GoogleDriveUtil.getCommunityDiscordActivityData();
        List<List<Object>> values = communityDiscordActivityData.getValues();

        for (List<Object> row : values) {
            SGC_Member sgcMember = new SGC_Member(SGC_Clan.getClanByName((String) row.get(0)));
            sgcMember.setDiscordID((String) row.get(1));
            sgcMember.setDiscordDisplayName((String) row.get(2));
            sgcMember.setDiscordMessages7Days(Integer.parseInt((String) row.get(3)));
            sgcMember.setDiscordActivity(sgcMember.getDiscordClanMessages7Days() > 0);
            sgcMember.setDiscordVoice7Days(Double.parseDouble((String) row.get(4)));
            sgcMember.setDiscordClanMessages7Days(Integer.parseInt((String) row.get(5)));
            sgcMember.setDiscordClanVoice7Days(Double.parseDouble((String) row.get(6)));

            members.get(sgcMember.getClan()).add(sgcMember);
        }
    }

    /**
     * Retrieves game activity for all clans and updates the member information.
     *
     * @param members A HashMap containing clans as keys and their members as
     *                values.
     */
    private static void getAllClansGameActivity(HashMap<SGC_Clan, ArrayList<SGC_Member>> members) {
        HashMap<SGC_Clan, HashMap<String, Instant>> allMembersLastDatePlayed = RaidReportTool
                .getAllMembersLastDatePlayed();

        for (SGC_Clan clan : allMembersLastDatePlayed.keySet()) {
            LOGGER.info("Processing the In-Game Activity for " + clan.name());

            HashMap<String, Instant> clanMembersLastDatePlayed = allMembersLastDatePlayed.get(clan);

            for (String bungieDisplayName : clanMembersLastDatePlayed.keySet()) {
                boolean isActive = false;

                try {
                    isActive = clanMembersLastDatePlayed.get(bungieDisplayName)
                            .isAfter(Instant.now().minus(10, ChronoUnit.DAYS));
                } catch (Exception e) {
                    LOGGER.error("An error occured getting game active activity for " + bungieDisplayName, e);
                }

                boolean found = false;

                for (SGC_Member member : members.get(clan)) {
                    if (member.isSameMember(bungieDisplayName)) {
                        found = true;
                        member.setGameActivity(isActive);
                        member.setBungieDisplayName(bungieDisplayName);
                        break;
                    }
                }

                if (!found) {
                    SGC_Member newMember = new SGC_Member(clan);
                    newMember.setBungieDisplayName(bungieDisplayName);
                    newMember.setGameActivity(isActive);
                    members.get(clan).add(newMember);
                }

            }
        }
    }

    private static void sendLogMessage(String logMessage) {
        try {
            new MessageBuilder()
                    .addEmbed(new EmbedBuilder()
                            .setAuthor(API.getYourself())
                            .setTitle("SGC Activity Sheets")
                            .setDescription(logMessage)
                            .setFooter("#AreYouShrouded")
                            .setThumbnail(ActivityReportTool.class
                                    .getClassLoader()
                                    .getResourceAsStream(
                                            "SGC.png"))
                            .setColor(Color.BLUE))
                    .send(API.getChannelById("629511503296593930").get().asTextChannel().get());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private static void sendErrorMessage(String logMessage) {
        try {
            MessageBuilder messageBuilder = new MessageBuilder()
                    .addEmbed(new EmbedBuilder()
                            .setAuthor(API.getYourself())
                            .setTitle("SGC Activity Sheets")
                            .setDescription(logMessage)
                            .setFooter("ERROR")
                            .setThumbnail(ActivityReportTool.class.getClassLoader()
                                    .getResourceAsStream("SGC.png"))
                            .setColor(Color.RED));

            Optional<TextChannel> channel = API.getChannelById("629511503296593930").get().asTextChannel();
            if (channel.isPresent()) {
                messageBuilder.send(channel.get());
            } else {
                LOGGER.error("Channel not found with ID: 629511503296593930");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Retrieves the name of the Discord role based on the role ID.
     *
     * @param discordRoleID The ID of the Discord role.
     * @return The name of the Discord role, or null if the role is not found.
     */
    public static String getDiscordRoleName(String discordRoleID) {
        Optional<Role> roleById = API.getRoleById(discordRoleID);
        return roleById.map(Role::getName).orElse(null);
    }

}