package sgc.bungie.api.processor.activity;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Update;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

import sgc.SGC_Clan;
import sgc.bungie.api.processor.RaidReportTool;
import sgc.discord.bot.BotApplication;

public class ActivityReportTool {

    private static final Logger LOGGER = BotApplication.getLogger();
    private static final String APPLICATION_NAME = "SGC Bot";
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * @param API
     */
    public static void runActivitySheets(DiscordApi API) {
        try {
            LOGGER.info(String.format("Starting the SGC Activity sheet update at %s",
                    ZonedDateTime.now(BotApplication.ZID).format(BotApplication.DATE_TIME_FORMATTER)));

            sendLogMessage(API, String.format("Starting the SGC Activity sheet update at %s",
                    ZonedDateTime.now(BotApplication.ZID).format(BotApplication.DATE_TIME_FORMATTER)));

            HashMap<SGC_Clan, ArrayList<SGC_Member>> members = new HashMap<>();
            for (SGC_Clan clan : SGC_Clan.values()) {
                members.put(clan, new ArrayList<>());
            }

            getAllClansDiscordActivity(API, members);
            getAllClansGameActivity(members);
            writeActivityToGoogleSheet(members);

            sendLogMessage(API, String.format("Completed the SGC Activity sheet update at %s",
                    ZonedDateTime.now(BotApplication.ZID).format(BotApplication.DATE_TIME_FORMATTER)));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            sendErrorMessage(API, String.format("An Error occured running the SGC Activity sheet update at %s",
                    ZonedDateTime.now(BotApplication.ZID).format(BotApplication.DATE_TIME_FORMATTER)));
        }
    }

    public static void initiateGoogleSheetsAuth() {
        try {
            LOGGER.info("Initiating Google Sheet Auth");
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                    getGoogleSheetsCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            final String spreadsheetId = "1R0RkQYKVWcy6DA71xNkoDU-XIc14UjXuD7M05H2VHPk";
            BatchClearValuesRequest batchClearValuesRequest = new BatchClearValuesRequest();
            List<String> ranges = new ArrayList<>();
            ranges.add("VOID!A2:D");
            batchClearValuesRequest.setRanges(ranges);
            service.spreadsheets().values().batchClear(spreadsheetId, batchClearValuesRequest).execute();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * @param API
     * @param members
     */
    private static void getAllClansDiscordActivity(DiscordApi API,
            HashMap<SGC_Clan, ArrayList<SGC_Member>> members) {
        HashMap<User, SGC_Member> allUsers = new HashMap<>();
        for (SGC_Clan clan : SGC_Clan.values()) {
            LOGGER.info("Processing the Discord Activity for " + clan.name());
            Optional<Role> roleById = API.getRoleById(clan.Discord_Role_ID);
            Set<User> users = roleById.get().getUsers();
            users.forEach(user -> {
                SGC_Member sgc_Member = new SGC_Member(clan);
                sgc_Member.setDiscordDisplayName(user.getDisplayName(BotApplication.SGC_SERVER));
                members.get(clan).add(sgc_Member);
                allUsers.put(user, sgc_Member);
            });
        }

        Set<Channel> channels = API.getChannels();
        channels.parallelStream().forEach(channel -> {
            Optional<TextChannel> textChannel = channel.asTextChannel();

            if (textChannel.isPresent() && textChannel.get().canReadMessageHistory(API.getYourself())) {
                try {
                    CompletableFuture<MessageSet> messagesWhile = textChannel.get().getMessagesWhile(message -> {
                        return message.getCreationTimestamp().compareTo(Instant.now().plus(-14, ChronoUnit.DAYS)) > 0;
                    });
                    messagesWhile.join().forEach(message -> {
                        Optional<User> userAuthor = message.getUserAuthor();
                        if (userAuthor.isPresent()) {
                            if (allUsers.containsKey(userAuthor.get())) {
                                allUsers.get(userAuthor.get()).setDiscord_activity(true);
                            }
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        });
    }

    private static void getAllClansGameActivity(HashMap<SGC_Clan, ArrayList<SGC_Member>> members) {
        HashMap<SGC_Clan, HashMap<String, Instant>> allMembersLastDatePlayed = RaidReportTool
                .getAllMembersLastDatePlayed();

        for (SGC_Clan sgc_clan : allMembersLastDatePlayed.keySet()) {
            LOGGER.info("Processing the In-Game Activity for " + sgc_clan.name());
            for (String bungieDisplayName : allMembersLastDatePlayed.get(sgc_clan).keySet()) {
                boolean isActive = allMembersLastDatePlayed.get(sgc_clan).get(bungieDisplayName)
                        .compareTo(Instant.now().plus(-10, ChronoUnit.DAYS)) > 0;
                boolean found = false;
                for (SGC_Member sgc_member : members.get(sgc_clan)) {
                    if (sgc_member.isSameMember(bungieDisplayName)) {
                        found = true;
                        sgc_member.setGame_activity(isActive);
                        sgc_member.setBungieDisplayName(bungieDisplayName);
                    }
                }
                if (!found) {
                    SGC_Member sgc_Member = new SGC_Member(sgc_clan);
                    sgc_Member.setBungieDisplayName(bungieDisplayName);
                    sgc_Member.setGame_activity(isActive);
                    members.get(sgc_clan).add(sgc_Member);
                }
            }
        }
    }

    /**
     * @param HTTP_TRANSPORT
     * @return
     * @throws IOException
     */
    private static Credential getGoogleSheetsCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = ActivityReportTool.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * @param members
     */
    private static void writeActivityToGoogleSheet(HashMap<SGC_Clan, ArrayList<SGC_Member>> members) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            final String spreadsheetId = "1R0RkQYKVWcy6DA71xNkoDU-XIc14UjXuD7M05H2VHPk";

            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                    getGoogleSheetsCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            BatchClearValuesRequest batchClearValuesRequest = new BatchClearValuesRequest();
            List<String> ranges = new ArrayList<>();
            List<Update> updates = new ArrayList<>();

            for (SGC_Clan clan : members.keySet()) {
                final String range = String.format("%s!A2:D", clan);
                ranges.add(range);

                ValueRange valueRange = new ValueRange();
                valueRange.setRange(range);
                List<List<Object>> values = new ArrayList<>();
                for (SGC_Member member : members.get(clan)) {
                    List<Object> row = new ArrayList<>();
                    row.add(member.getDiscordDisplayName());
                    row.add(member.getBungieDisplayName());
                    row.add(member.isDiscord_activity());
                    row.add(member.isGame_activity());
                    values.add(row);
                }
                valueRange.setValues(values);
                Update update = service.spreadsheets().values().update(spreadsheetId, range, valueRange);
                update.setValueInputOption("USER_ENTERED");
                updates.add(update);
            }
            batchClearValuesRequest.setRanges(ranges);
            service.spreadsheets().values().batchClear(spreadsheetId, batchClearValuesRequest).execute();
            for (Update update : updates) {
                update.execute();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private static void sendLogMessage(DiscordApi API, String logMessage) {
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

    private static void sendErrorMessage(DiscordApi API, String logMessage) {
        try {
            new MessageBuilder()
                    .addEmbed(new EmbedBuilder()
                            .setAuthor(API.getYourself())
                            .setTitle("SGC Activity Sheets")
                            .setDescription(logMessage)
                            .setFooter("ERROR")
                            .setThumbnail(ActivityReportTool.class
                                    .getClassLoader()
                                    .getResourceAsStream(
                                            "SGC.png"))
                            .setColor(Color.RED))
                    .send(API.getChannelById("629511503296593930").get().asTextChannel().get());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
