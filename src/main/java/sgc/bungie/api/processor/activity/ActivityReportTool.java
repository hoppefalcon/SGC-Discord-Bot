package sgc.bungie.api.processor.activity;

import java.awt.Color;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;

import sgc.SGC_Clan;
import sgc.bungie.api.processor.RaidReportTool;
import sgc.discord.bot.BotApplication;
import sgc.discord.infographics.GoogleDriveUtil;

public class ActivityReportTool {

    private static final Logger LOGGER = BotApplication.getLogger();

    private static DiscordApi API = null;

    public static final HashMap<String, String> CLAN_ROLE_ID_MAP = initializeClanRoleDiscordIDMap();

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
        HashMap<User, ArrayList<SGC_Member>> allUsers = new HashMap<>();

        for (SGC_Clan clan : SGC_Clan.values()) {
            LOGGER.info("Processing the Discord Activity for " + clan.name());
            Optional<Role> roleById = API.getRoleById(clan.Discord_Role_ID);

            if (roleById.isPresent()) {
                Set<User> users = roleById.get().getUsers();

                users.forEach(user -> {
                    SGC_Member sgcMember = new SGC_Member(clan);
                    sgcMember.setDiscordDisplayName(user.getDisplayName(BotApplication.SGC_SERVER));
                    sgcMember.getDiscordMessageCounts().put("TOTAL", 0);
                    sgcMember.setDiscordUserName(user.getDiscriminatedName());
                    members.get(clan).add(sgcMember);

                    allUsers.computeIfAbsent(user, k -> new ArrayList<>()).add(sgcMember);
                });
            }
        }

        Set<Channel> channels = API.getChannels();
        StringBuilder sb = new StringBuilder();

        channels.parallelStream().forEach(channel -> {
            if (channel.canYouSee()) {
                Optional<TextChannel> textChannel = channel.asTextChannel();

                if (textChannel.isPresent() && textChannel.get().canReadMessageHistory(API.getYourself())) {
                    try {
                        CompletableFuture<MessageSet> messagesWhile = textChannel.get()
                                .getMessagesWhile(message -> message.getCreationTimestamp()
                                        .compareTo(Instant.now().plus(-14, ChronoUnit.DAYS)) > 0);
                        messagesWhile.join().forEach(message -> {
                            Optional<User> userAuthor = message.getUserAuthor();
                            if (userAuthor.isPresent() && allUsers.containsKey(userAuthor.get())) {
                                for (SGC_Member member : allUsers.get(userAuthor.get())) {
                                    member.getDiscordMessageCounts().put("TOTAL",
                                            member.getDiscordMessageCounts().get("TOTAL") + 1);
                                    member.setDiscordActivity(true);
                                }
                            }
                        });
                    } catch (Exception e) {
                        sb.append(channel.getIdAsString()).append("\n");
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        });
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
     * Retrieves the clan's Discord activity for a specific channel within a certain
     * number of days.
     *
     * @param clan      The clan for which to retrieve the Discord activity.
     * @param channelID The ID of the Discord channel.
     * @param days      The number of days to consider for the activity.
     * @return A string representing the clan's Discord activity.
     * @throws Exception If an error occurs during the process.
     */
    public static String getClanDiscordActivityForForum(SGC_Clan clan, String channelID, int days) throws Exception {
        HashMap<User, ArrayList<SGC_Member>> allUsers = new HashMap<>();

        LOGGER.info("Processing the Discord Activity for " + clan.name());
        Optional<Role> role = API.getRoleById(clan.Discord_Role_ID);
        if (role.isEmpty()) {
            throw new Exception("Role not found with ID: " + clan.Discord_Role_ID);
        }
        Set<User> users = role.get().getUsers();

        users.forEach(user -> {
            SGC_Member sgcMember = new SGC_Member(clan);
            sgcMember.setDiscordDisplayName(user.getDisplayName(BotApplication.SGC_SERVER));
            allUsers.computeIfAbsent(user, key -> new ArrayList<>()).add(sgcMember);
        });

        Optional<Channel> channel = API.getChannelById(channelID);
        if (channel.isPresent()) {
            API.getServerThreadChannels().forEach(thread -> {
                if (thread.getParent().getIdAsString().equals(channelID)) {
                    try {
                        CompletableFuture<MessageSet> messagesWhile = thread.getMessagesWhile(message -> message
                                .getCreationTimestamp().compareTo(Instant.now().plus(days * -1, ChronoUnit.DAYS)) > 0);
                        messagesWhile.join().forEach(message -> {
                            Optional<User> userAuthor = message.getUserAuthor();
                            if (userAuthor.isPresent()) {
                                ArrayList<SGC_Member> members = allUsers.get(userAuthor.get());
                                if (members != null) {
                                    for (SGC_Member member : members) {
                                        HashMap<String, Integer> messageCounts = member.getDiscordMessageCounts();
                                        messageCounts.put(channelID, messageCounts.getOrDefault(channelID, 0) + 1);
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            });

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("\"Discord Name\",");
            stringBuilder.append("\"").append(channel.get().asServerChannel().get().getName()).append("\",");
            stringBuilder.append("\n");

            allUsers.keySet().forEach(user -> {
                stringBuilder.append("\"").append(allUsers.get(user).get(0).getDiscordDisplayName()).append("\",");
                int count = 0;
                for (SGC_Member member : allUsers.get(user)) {
                    HashMap<String, Integer> messageCounts = member.getDiscordMessageCounts();
                    count += messageCounts.getOrDefault(channelID, 0);
                }
                stringBuilder.append("\"").append(count).append("\",");
                stringBuilder.append("\n");
            });

            return stringBuilder.toString();
        } else {
            throw new Exception("Channel not found with ID: " + channelID);
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

    /**
     * Retrieves the members of the Discord role based on the role ID.
     *
     * @param discordRoleID The ID of the Discord role.
     * @return A string containing the display names of the role members, separated
     *         by newlines,
     *         or null if the role is not found.
     */
    public static String getDiscordRoleMembers(String discordRoleID) {
        ArrayList<User> users = getDiscordRoleMembersList(discordRoleID);
        if (users.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            users.forEach(user -> {
                sb.append(user.getDisplayName(BotApplication.SGC_SERVER)).append("\n");
            });
            return sb.toString();
        } else {
            return null;
        }
    }

    private static ArrayList<User> getDiscordRoleMembersList(String discordRoleID) {
        ArrayList<User> memberList = new ArrayList<>();
        Optional<Role> roleById = API.getRoleById(discordRoleID);
        if (roleById.isPresent()) {
            Set<User> users = roleById.get().getUsers();
            users.forEach(user -> {
                memberList.add(user);
            });
        }
        return memberList;
    }

    private static String crossreferenceRoleMemberLists(String discordRoleID1, String discordRoleID2) {
        ArrayList<User> role1UserList = getDiscordRoleMembersList(discordRoleID1);
        ArrayList<User> role2UserList = getDiscordRoleMembersList(discordRoleID2);

        HashMap<String, Integer> userRoleMap = new HashMap<>();
        role1UserList.forEach(user -> {
            String userString = String.format("%s (%s)",
                    user.getDisplayName(BotApplication.SGC_SERVER),
                    user.getMentionTag());
            userRoleMap.putIfAbsent(userString, 0);
            userRoleMap.put(userString, userRoleMap.get(userString) + 1);
        });
        role2UserList.forEach(user -> {
            String userString = String.format("%s (%s)",
                    user.getDisplayName(BotApplication.SGC_SERVER),
                    user.getMentionTag());
            userRoleMap.putIfAbsent(userString, 0);
            userRoleMap.put(userString, userRoleMap.get(userString) + 1);
        });
        StringBuilder sb = new StringBuilder();
        userRoleMap.forEach((userString, value) -> {
            LOGGER.info("DEBUG TEST: %s :: %s", userString, value);
            if (value == 2)
                sb.append(userString).append("\n");
        });

        return sb.toString();
    }

    public static String getClanNonRegisteredMembers(String discordClanRoleID) {
        return crossreferenceRoleMemberLists(discordClanRoleID, GoogleDriveUtil.getNotRegisteredRoleID());
    }

    private static HashMap<String, String> initializeClanRoleDiscordIDMap() {
        return (HashMap<String, String>) GoogleDriveUtil.getClanRoleIDs();
    }

}