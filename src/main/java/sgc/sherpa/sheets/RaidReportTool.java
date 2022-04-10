/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.sherpa.sheets;

import static java.util.Map.entry;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.slf4j.Logger;

import sgc.raid.report.bot.BotApplication;;

/**
 * @author chris hoppe
 */

public class RaidReportTool {

    private static final Logger LOGGER = BotApplication.getLogger();
    private static String apiKey = System.getenv("BUNGIE-TOKEN");

    private static final HashMap<String, String> pcClanIdMap = new HashMap<>();
    private static final HashMap<String, String> xbClanIdMap = new HashMap<>();
    private static final HashMap<String, String> psClanIdMap = new HashMap<>();

    // private static List<String> pcClanIds = Arrays.asList("3087185", "3019103",
    // "3063489", "3007121", "3008645",
    // "3076620", "3090996", "3100797", "3095868", "2820714", "2801315", "3915247",
    // "3949151", "3070603", "3795604");
    // private static List<String> xbClanIds = Arrays.asList("4327464", "4327434",
    // "4327418", "4327389", "4418635");
    // private static List<String> psClanIds = Arrays.asList("4327587", "4327584",
    // "4327575", "4327536", "4327542");

    private static Map<String, List<String>> clanIdMap = Map.ofEntries(
            entry("PC",
                    Arrays.asList("3087185", "3019103",
                            "3063489", "3007121", "3008645",
                            "3076620", "3090996", "3100797", "3095868", "2820714",
                            "2801315", "3915247", "3949151", "3070603", "3795604")),
            entry("Xbox",
                    Arrays.asList("4327464", "4327434", "4327418", "4327389", "4418635")),
            entry("PSN",
                    Arrays.asList("4327587", "4327584", "4327575", "4327536", "4327542")));

    private static ExecutorService executorService = Executors.newFixedThreadPool(15);

    /**
     * @param args the command line arguments
     * @throws IOException
     */

    // public static void main(String[] args) throws InterruptedException,
    // IOException {
    // System.out.println(System.getProperty("os.name"));
    // Instant start = Instant.now();

    // String output = getSGCWeeklyActivityReport(LocalDate.parse("20220222",
    // DateTimeFormatter.BASIC_ISO_DATE),
    // LocalDate.parse("20220228", DateTimeFormatter.BASIC_ISO_DATE), null);

    // executorService.shutdown();

    // Instant end = Instant.now();
    // Duration timeElapsed = Duration.between(start,
    // end);

    // long hours = timeElapsed.toHours();
    // long minutes = timeElapsed.toMinutesPart();
    // long secounds = timeElapsed.toSecondsPart();
    // System.out.println(String.format("DONE (%02d:%02d:%02d)", hours, minutes,
    // secounds));
    // }

    public static void initializeClanIdMap() {
        LOGGER.debug("Initializing Clan Map");
        clanIdMap.forEach((platform, clanIds) -> {
            clanIds.forEach((clanId) -> {
                try {
                    Clan clan = new Clan(clanId);
                    getClanInfo(clan);

                    switch (platform) {
                        case "PC":
                            pcClanIdMap.put(clan.getCallsign(), clan.getClanId());
                            break;
                        case "Xbox":
                            xbClanIdMap.put(clan.getCallsign(), clan.getClanId());
                            break;
                        case "PSN":
                            psClanIdMap.put(clan.getCallsign(), clan.getClanId());
                            break;
                    }
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            });
        });
        LOGGER.debug("Clan Map Initialized");
    }

    /**
     * @return the pcclanidmap
     */
    public static HashMap<String, String> getPcClanIdMap() {
        return pcClanIdMap;
    }

    /**
     * @return the xbclanidmap
     */
    public static HashMap<String, String> getXbClanIdMap() {
        return xbClanIdMap;
    }

    /**
     * @return the psclanidmap
     */
    public static HashMap<String, String> getPsClanIdMap() {
        return psClanIdMap;
    }

    public static Clan getClanInformation(String clanId) throws Exception {
        Clan clan = new Clan(clanId);

        getClanInfo(clan);
        getClanMembers(clan);

        return clan;
    }

    public static Clan getClanRaidReport(Clan clan) throws Exception {
        LOGGER.trace("Processing " + clan.getName());
        clan.getMembers().forEach((memberId, member) -> {
            LOGGER.info("Processing " + member.getDisplayName());
            try {
                getMemberCharacters(member);
                getMemberRaidInfo(member);
                getUserWeeklyClears(member, LocalDate.now().minusDays(6), LocalDate.now());
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            LOGGER.trace("Finished Processing " + member.getDisplayName());
        });
        return clan;
    }

    public static Clan getClanRaidReport(Clan clan,
            InteractionOriginalResponseUpdater interactionOriginalResponseUpdater) throws Exception {
        LOGGER.trace("Processing " + clan.getName());
        final AtomicInteger count = new AtomicInteger(0);

        List<Callable<Object>> tasks = new ArrayList<>();
        clan.getMembers().forEach((memberId, member) -> {
            try {
                tasks.add(() -> {
                    getClanMemberRaidReport(memberId, member);

                    interactionOriginalResponseUpdater
                            .setContent(String.format("Building a clan raid report for %s (%d/%d)", clan.getName(),
                                    count.incrementAndGet(), clan.getMembers().size()))
                            .update().join();
                    return member;
                });
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        });
        executorService.invokeAll(tasks);
        LOGGER.trace("Finished Processing " + clan.getName());
        return clan;
    }

    private static void getClanMemberRaidReport(String memberId, Member member) {
        LOGGER.trace("Processing " + member.getDisplayName());
        try {
            getMemberCharacters(member);
            getMemberRaidInfo(member);
            getUserWeeklyClears(member, LocalDate.now().minusDays(6), LocalDate.now());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        LOGGER.trace("Finished Processing " + member.getDisplayName());
    }

    public static void getClanInfo(Clan clan) throws IOException {
        URL url = new URL(String.format("https://www.bungie.net/Platform/GroupV2/%s/", clan.getClanId()));

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.addRequestProperty("X-API-Key", apiKey);
        conn.addRequestProperty("Accept", "Application/Json");
        conn.connect();

        // Getting the response code
        int responsecode = conn.getResponseCode();

        if (responsecode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            JsonObject json = JsonParser.parseString(content.toString()).getAsJsonObject();
            clan.setName(json.getAsJsonObject("Response")
                    .getAsJsonObject("detail").get("name").getAsString());
            clan.setCallsign(json.getAsJsonObject("Response")
                    .getAsJsonObject("detail").getAsJsonObject("clanInfo").get("clanCallsign").getAsString());
            in.close();
        }
        conn.disconnect();
    }

    public static void getClanMembers(Clan clan) throws IOException {
        URL url = new URL(String.format("https://www.bungie.net/Platform/GroupV2/%s/Members/", clan.getClanId()));

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.addRequestProperty("X-API-Key", apiKey);
        conn.addRequestProperty("Accept", "Application/Json");
        conn.connect();

        // Getting the response code
        int responsecode = conn.getResponseCode();

        if (responsecode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            // JsonObject json =
            // JsonParser.parseString(content.toString()).getAsJsonObject();
            JsonArray results = (JsonArray) JsonParser.parseString(content.toString()).getAsJsonObject()
                    .getAsJsonObject("Response").get("results");
            results.forEach((entry) -> {
                try {
                    JsonObject userInfo = entry.getAsJsonObject().getAsJsonObject("destinyUserInfo");
                    String membershipType = userInfo.get("membershipType").getAsString();
                    String membershipId = userInfo.get("membershipId").getAsString();
                    String displayName = userInfo.get("displayName").getAsString();
                    String bungieGlobalDisplayName = "";
                    String bungieGlobalDisplayNameCode = "";
                    try {
                        bungieGlobalDisplayName = userInfo.get("bungieGlobalDisplayName").getAsString();
                        bungieGlobalDisplayNameCode = userInfo.get("bungieGlobalDisplayNameCode").getAsString();
                    } catch (NullPointerException ex) {
                        LOGGER.info(displayName + " has yet to register for a bungieGlobalDisplayName");
                    }
                    clan.getMembers().put(membershipId, new Member(membershipId, displayName, membershipType,
                            bungieGlobalDisplayName, bungieGlobalDisplayNameCode, clan.getClanId()));
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            });
            in.close();
        }
        conn.disconnect();
    }

    public static void getMemberCharacters(Member member) throws IOException {
        URL url = new URL(String.format("https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Stats",
                member.getMemberType(), member.getUID()));

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.addRequestProperty("X-API-Key", apiKey);
        conn.addRequestProperty("Accept", "Application/Json");
        conn.connect();

        // Getting the response code
        int responsecode = conn.getResponseCode();

        if (responsecode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            // JsonObject json =
            // JsonParser.parseString(content.toString()).getAsJsonObject();
            JsonArray results = (JsonArray) JsonParser.parseString(content.toString()).getAsJsonObject()
                    .getAsJsonObject("Response").get("characters");
            results.forEach((entry) -> {
                try {
                    if (entry.getAsJsonObject().get("deleted").getAsBoolean() == false) {
                        String characterId = entry.getAsJsonObject().get("characterId").getAsString();
                        member.getCharacters().put(characterId, new Character(characterId));
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            });
            in.close();
        }
        conn.disconnect();
    }

    public static void getMemberRaidInfo(Member member) throws IOException {
        List<String> validRaidHashes = Raid.getAllValidRaidHashes();
        member.getCharacters().forEach((characterId, character) -> {
            try {
                URL url = new URL(String.format(
                        "https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Character/%s/Stats/AggregateActivityStats",
                        member.getMemberType(), member.getUID(), character.getUID()));

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.addRequestProperty("X-API-Key", apiKey);
                conn.addRequestProperty("Accept", "Application/Json");
                conn.connect();

                // Getting the response code
                int responsecode = conn.getResponseCode();

                if (responsecode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuffer content = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    // JsonObject json =
                    // JsonParser.parseString(content.toString()).getAsJsonObject();
                    JsonArray results = (JsonArray) JsonParser.parseString(content.toString()).getAsJsonObject()
                            .getAsJsonObject("Response").get("activities");
                    List<JsonObject> raids = IntStream.range(0, results.size())
                            .mapToObj(index -> (JsonObject) results.get(index)).filter((result) -> {
                                // String asString = result.get("activityHash").getAsString();
                                return validRaidHashes.contains(result.get("activityHash").getAsString());
                            }).collect(Collectors.toList());
                    raids.forEach((entry) -> {
                        Raid raid = Raid.getRaid(entry.get("activityHash").getAsString());
                        if (raid != null) {
                            String clears = entry.get("values").getAsJsonObject().get("activityCompletions")
                                    .getAsJsonObject().get("basic").getAsJsonObject().get("value").getAsString();
                            character.getRaidActivities().get(raid).addClears(Double.parseDouble(clears));
                        }
                    });

                    in.close();
                }
                conn.disconnect();

            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        });
    }

    public static String getClanRaidReportAsCsv(Clan clan) throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",");
        Raid.getRaidsOrdered().forEach((Raid raid) -> {
            stringBuilder.append(raid.name).append(",");
        });
        stringBuilder.append("\"Clears in the past 7 Days\"").append("\n");

        clan.getMembers().forEach((id, member) -> {
            HashMap<Raid, Integer> raidClears = member.getRaidClears();
            stringBuilder.append("\"").append(member.getDisplayName()).append("\",")
                    .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",");
            Raid.getRaidsOrdered().forEach((Raid raid) -> {
                stringBuilder.append("\"").append(raidClears.get(raid)).append("\",");
            });
            stringBuilder.append(member.getTotalWeeklyRaidClears()).append("\"\n");

        });
        return stringBuilder.toString();
    }

    public static byte[] getClanRaidReportAsCsvByteArray(Clan clan) throws IOException {
        return getClanRaidReportAsCsv(clan).getBytes();
    }

    public static String getUserReport(String bungieId) throws Exception {
        Member user = getMemberInformationWithCharacters(bungieId);
        final StringBuilder response = new StringBuilder();

        if (user != null) {
            getMemberRaidInfo(user);
            HashMap<Raid, Integer> raidClears = user.getRaidClears();
            Raid.getRaidsOrdered().forEach((Raid raid) -> {
                response.append(raid.name).append(": ").append(raidClears.get(raid)).append("\n");
            });
            response.append("\nTOTAL: ").append(user.getTotalRaidClears());
        }
        return response.toString();
    }

    public static Member getMemberInformationWithCharacters(String bungieId) throws Exception {
        String[] splitBungieId = bungieId.split("#");

        Member user = null;
        if (splitBungieId.length == 2) {
            AtomicInteger page = new AtomicInteger(0);
            boolean morePages = true;
            final HashMap<String, Member> searchResults = new HashMap<>();

            while (morePages) {
                URL url = new URL(String.format("https://www.bungie.net/Platform/User/Search/Prefix/%s/%d/",
                        splitBungieId[0].trim().replace(" ", "%20"), page.getAndIncrement()));

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.addRequestProperty("X-API-Key", apiKey);
                conn.addRequestProperty("Accept", "Application/Json");
                conn.connect();

                // Getting the response code
                int responsecode = conn.getResponseCode();
                if (responsecode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuffer content = new StringBuffer();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }

                    morePages = JsonParser.parseString(content.toString()).getAsJsonObject().getAsJsonObject("Response")
                            .get("hasMore").getAsBoolean();

                    JsonArray results = (JsonArray) JsonParser.parseString(content.toString()).getAsJsonObject()
                            .getAsJsonObject("Response").get("searchResults");

                    results.forEach((entry) -> {
                        try {
                            JsonArray userInfo = (JsonArray) entry.getAsJsonObject().get("destinyMemberships");
                            String membershipType = userInfo.get(0).getAsJsonObject().get("membershipType")
                                    .getAsString();
                            String membershipId = userInfo.get(0).getAsJsonObject().get("membershipId").getAsString();
                            String displayName = userInfo.get(0).getAsJsonObject().get("displayName").getAsString();
                            String bungieGlobalDisplayName = "";
                            String bungieGlobalDisplayNameCode = "";
                            try {
                                bungieGlobalDisplayName = userInfo.get(0).getAsJsonObject()
                                        .get("bungieGlobalDisplayName").getAsString();
                                bungieGlobalDisplayNameCode = userInfo.get(0).getAsJsonObject()
                                        .get("bungieGlobalDisplayNameCode").getAsString();
                            } catch (NullPointerException ex) {

                            }
                            searchResults.put(membershipId, new Member(membershipId, displayName, membershipType,
                                    bungieGlobalDisplayName, bungieGlobalDisplayNameCode, "NULL"));

                        } catch (Exception ex) {
                            LOGGER.error(ex.getMessage(), ex);
                        }
                    });

                    in.close();
                } else {
                    morePages = false;
                }
                conn.disconnect();
            }
            for (Member member : searchResults.values()) {
                if (member.getBungieGlobalDisplayName().equalsIgnoreCase(splitBungieId[0].trim())
                        && Integer.parseInt(member.getBungieGlobalDisplayNameCode()) == Integer
                                .parseInt(splitBungieId[1].trim())) {
                    user = member;
                }
            }
            if (user != null) {
                getMemberCharacters(user);
            }
        }
        return user;
    }

    public static String getUserWeeklyClears(String bungieId, LocalDate startDate, LocalDate endDate) throws Exception {
        Member user = getMemberInformationWithCharacters(bungieId);
        final StringBuilder response = new StringBuilder();

        if (user != null) {
            getUserWeeklyClears(user, startDate, endDate);
            HashMap<Raid, Integer> raidClears = user.getWeeklyRaidClears();
            Raid.getRaidsOrdered().forEach((Raid raid) -> {
                response.append(raid.name).append(": ").append(raidClears.get(raid)).append("\n");
            });
            response.append("\nTOTAL: ").append(user.getTotalWeeklyRaidClears());
        }
        return response.toString();
    }

    public static Member getUserWeeklyClears(Member member, LocalDate startDate, LocalDate endDate) {
        LOGGER.info("Processing Weekly Clears for " + member.getCombinedBungieGlobalDisplayName());
        List<String> validRaidHashes = Raid.getAllValidRaidHashes();
        member.getCharacters().forEach((characterId, character) -> {
            try {
                boolean next = false;
                for (int page = 0; !next; page++) {
                    URL url = new URL(String.format(
                            "https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Character/%s/Stats/Activities/?page=%d&mode=4&count=250",
                            member.getMemberType(), member.getUID(), character.getUID(), page));

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.addRequestProperty("X-API-Key", apiKey);
                    conn.addRequestProperty("Accept", "Application/Json");

                    LOGGER.trace(String.format("Makking HTTP call #%d for %s:%s", page + 1,
                            member.getCombinedBungieGlobalDisplayName(), character.getUID()));
                    conn.connect();

                    // Getting the response code
                    int responsecode = conn.getResponseCode();

                    if (responsecode == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuffer content = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        JsonArray results = (JsonArray) JsonParser.parseString(content.toString()).getAsJsonObject()
                                .getAsJsonObject("Response").get("activities");
                        if (results != null) {
                            next = results.size() < 250;
                            List<JsonObject> raids = IntStream.range(0, results.size())
                                    .mapToObj(index -> (JsonObject) results.get(index)).filter((result) -> {
                                        return validRaidHashes.contains(result.get("activityDetails").getAsJsonObject()
                                                .get("directorActivityHash").getAsString());
                                    }).collect(Collectors.toList());
                            raids.forEach((entry) -> {
                                Raid raid = Raid.getRaid(entry.get("activityDetails").getAsJsonObject()
                                        .get("directorActivityHash").getAsString());
                                if (raid != null) {
                                    boolean completed = entry.get("values").getAsJsonObject().get("completed")
                                            .getAsJsonObject().get("basic").getAsJsonObject().get("displayValue")
                                            .getAsString().equalsIgnoreCase("Yes");
                                    if (completed) {
                                        String dateCompletedStr = entry.getAsJsonPrimitive("period").getAsString();
                                        LocalDate dateCompleted = LocalDate.parse(dateCompletedStr,
                                                DateTimeFormatter.ISO_DATE_TIME);
                                        if ((dateCompleted.isEqual(startDate) || dateCompleted.isAfter(startDate))
                                                && (dateCompleted.isBefore(endDate)
                                                        || dateCompleted.isEqual(endDate))) {
                                            character.getRaidActivities().get(raid).addWeeklyClears(1);
                                        }
                                    }
                                }
                            });
                        } else {
                            next = true;
                        }
                        in.close();
                    } else {
                        next = true;
                    }
                    conn.disconnect();
                }
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        });
        return member;
    }

    public static RaidCarnageReport getRaidCarnageReport(String carnageReportId) throws IOException {
        URL url = new URL(String.format("https://stats.bungie.net/Platform/Destiny2/Stats/PostGameCarnageReport/%s/",
                carnageReportId));

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.addRequestProperty("X-API-Key", apiKey);
        conn.addRequestProperty("Accept", "Application/Json");
        conn.connect();

        // Getting the response code
        int responsecode = conn.getResponseCode();
        RaidCarnageReport raidCarnageReport = null;
        if (responsecode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            JsonObject response = JsonParser.parseString(content.toString()).getAsJsonObject()
                    .getAsJsonObject("Response");
            JsonObject activityDetails = response.getAsJsonObject("activityDetails");
            JsonArray entries = response.get("entries").getAsJsonArray();

            String dateCompletedStr = response.get("period").getAsString();
            LocalDate dateCompleted = LocalDate.parse(dateCompletedStr, DateTimeFormatter.ISO_DATE_TIME);

            final RaidCarnageReport tempRaidCarnageReport = new RaidCarnageReport(
                    Raid.getRaid(activityDetails.get("directorActivityHash").getAsString()), dateCompleted);
            entries.forEach((entry) -> {
                boolean completed = entry.getAsJsonObject().get("values").getAsJsonObject().get("completed")
                        .getAsJsonObject().get("basic").getAsJsonObject().get("value").getAsDouble() == 1.0;

                tempRaidCarnageReport.getPlayers().add(new RaidCarnageReportPlayer(
                        entry.getAsJsonObject().get("player").getAsJsonObject().get("destinyUserInfo").getAsJsonObject()
                                .get("bungieGlobalDisplayName").getAsString(),
                        entry.getAsJsonObject().get("player").getAsJsonObject().get("destinyUserInfo").getAsJsonObject()
                                .get("bungieGlobalDisplayNameCode").getAsString(),
                        entry.getAsJsonObject().get("player").getAsJsonObject().get("destinyUserInfo").getAsJsonObject()
                                .get("membershipType").getAsInt(),
                        entry.getAsJsonObject().get("player").getAsJsonObject().get("characterClass").getAsString(),
                        completed,
                        entry.getAsJsonObject().get("values").getAsJsonObject().get("deaths").getAsJsonObject()
                                .get("basic").getAsJsonObject().get("value").getAsDouble(),
                        entry.getAsJsonObject().get("values").getAsJsonObject().get("assists").getAsJsonObject()
                                .get("basic").getAsJsonObject().get("value").getAsDouble(),
                        entry.getAsJsonObject().get("values").getAsJsonObject().get("kills").getAsJsonObject()
                                .get("basic").getAsJsonObject().get("value").getAsDouble(),
                        entry.getAsJsonObject().get("values").getAsJsonObject().get("opponentsDefeated")
                                .getAsJsonObject().get("basic").getAsJsonObject().get("value").getAsDouble(),
                        entry.getAsJsonObject().get("values").getAsJsonObject().get("efficiency").getAsJsonObject()
                                .get("basic").getAsJsonObject().get("value").getAsDouble(),
                        entry.getAsJsonObject().get("values").getAsJsonObject().get("killsDeathsAssists")
                                .getAsJsonObject().get("basic").getAsJsonObject().get("value").getAsDouble(),
                        entry.getAsJsonObject().get("values").getAsJsonObject().get("activityDurationSeconds")
                                .getAsJsonObject().get("basic").getAsJsonObject().get("value").getAsDouble()));
            });
            in.close();
            raidCarnageReport = tempRaidCarnageReport;
        }
        conn.disconnect();
        return raidCarnageReport;

    }

    public static HashMap<String, String> getSGCWeeklyActivityReport(LocalDate startDate, LocalDate endDate,
            InteractionOriginalResponseUpdater interactionOriginalResponseUpdater, TextChannel textChannel,
            User discordUser)
            throws IOException {
        LOGGER.info("Starting SGC Activity Report");

        AtomicLong TOTAL_PGCR_COUNT = new AtomicLong(0);
        AtomicLong SCORED_PGCR_COUNT = new AtomicLong(0);
        if (interactionOriginalResponseUpdater != null) {
            interactionOriginalResponseUpdater.setContent(String
                    .format("Building a SGC activity report from %s to %s\nThis will take a while.",
                            startDate,
                            endDate))
                    .update().join();
        }
        List<Clan> clanList = initializeClanList();
        HashMap<String, Member> sgcClanMembersMap = new HashMap<>();

        for (int i = 0; i < clanList.size(); i++) {
            Clan clan = clanList.get(i);
            clan.getMembers().forEach((id, member) -> {
                sgcClanMembersMap.put(id, member);
            });

            LOGGER.info("Starting to process " + clan.getCallsign());
            clan.getMembers().forEach((memberId, member) -> {
                if (member.hasNewBungieName()) {
                    try {
                        LOGGER.info("Starting to process " + member.getDisplayName());
                        TOTAL_PGCR_COUNT.addAndGet(
                                getMembersClearedActivities(member, startDate, endDate,
                                        sgcClanMembersMap));
                        SCORED_PGCR_COUNT.addAndGet(member.getWeeklySGCActivity().get("COUNT"));
                        LOGGER.debug("Finished processing " + member.getDisplayName());
                    } catch (IOException ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                }
            });
            try {
                sendClanSGCActivityMessage(startDate, endDate, clan, textChannel, discordUser);
            } finally {
                System.gc();
                LOGGER.info("Finished processing " + clan.getCallsign());
            }

        }

        LOGGER.info("Finished processing All Clans for SGC Activity Report");

        HashMap<String, String> potwActivityReportAsCsv = getFullActivityReportAsCsv(clanList);
        LOGGER.info("SGC Activity Report Complete");
        return potwActivityReportAsCsv;
    }

    public static List<Clan> initializeClanList() {
        LOGGER.debug("Initializing Clan List for SGC Activity Report");
        ArrayList<Clan> map = new ArrayList<>();
        clanIdMap.forEach((platform, clanIds) -> {
            clanIds.forEach((clanId) -> {
                try {
                    Clan clan = new Clan(clanId);
                    getClanInfo(clan);
                    getClanMembers(clan);
                    map.add(clan);
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            });
        });
        LOGGER.debug("Clan List Initialized");
        return map;
    }

    public static int getMembersClearedActivities(Member member, LocalDate startDate, LocalDate endDate,
            HashMap<String, Member> sgcClanMembersMap) throws IOException {
        LOGGER.debug(String.format("Getting Cleared Activities for %s", member.getCombinedBungieGlobalDisplayName()));
        AtomicInteger PGCR_COUNT = new AtomicInteger(0);
        getMemberCharacters(member);
        member.getCharacters().forEach((characteruid, character) -> {
            try {
                boolean next = false;
                List<GenericActivity> genericActivitiesToProcess = new ArrayList<>();
                for (int page = 0; !next; page++) {
                    URL url = new URL(String.format(
                            "https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Character/%s/Stats/Activities/?page=%d&mode=0&count=250",
                            member.getMemberType(), member.getUID(), character.getUID(), page));

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.addRequestProperty("X-API-Key", apiKey);
                    conn.addRequestProperty("Accept", "Application/Json");

                    LOGGER.trace(String.format("Makking HTTP call #%d for %s:%s", page + 1,
                            member.getCombinedBungieGlobalDisplayName(), character.getUID()));
                    conn.connect();

                    // Getting the response code
                    int responsecode = conn.getResponseCode();

                    if (responsecode == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuffer content = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        JsonArray results = (JsonArray) JsonParser.parseString(content.toString()).getAsJsonObject()
                                .getAsJsonObject("Response").get("activities");
                        if (results != null) {
                            AtomicInteger recordsAfterEndDate = new AtomicInteger(0);

                            List<JsonObject> clearedActivities = IntStream.range(0, results.size())
                                    .mapToObj(index -> (JsonObject) results.get(index)).filter((result) -> {
                                        int mode = result.getAsJsonObject("activityDetails")
                                                .getAsJsonPrimitive("mode").getAsInt();

                                        if (Mode.invalidModesForPOTW().contains(mode)) {
                                            return false;
                                        }
                                        String activityDateStr = result.getAsJsonPrimitive("period").getAsString();
                                        LocalDate dateCompleted = LocalDate.parse(activityDateStr,
                                                DateTimeFormatter.ISO_DATE_TIME);
                                        if (dateCompleted.isBefore(startDate) || dateCompleted.isAfter(endDate)) {
                                            return false;
                                        }
                                        if (dateCompleted.isAfter(endDate)) {
                                            recordsAfterEndDate.incrementAndGet();
                                        }
                                        boolean completed = result.getAsJsonObject().getAsJsonObject("values")
                                                .getAsJsonObject("completed")
                                                .getAsJsonObject("basic").getAsJsonPrimitive("value")
                                                .getAsDouble() == 1.0;
                                        return completed;
                                    }).collect(Collectors.toList());
                            next = (results.size() < 250) || (recordsAfterEndDate.get() == results.size());

                            clearedActivities.forEach((activity) -> {
                                String instanceId = activity.getAsJsonObject().getAsJsonObject("activityDetails")
                                        .getAsJsonPrimitive("instanceId").toString().replace("\"", "");
                                double team = activity.getAsJsonObject().getAsJsonObject("values")
                                        .getAsJsonObject("team")
                                        .getAsJsonObject("basic").getAsJsonPrimitive("value")
                                        .getAsDouble();
                                GenericActivity genericActivity = new GenericActivity(instanceId);
                                genericActivity.setTeam(team);
                                genericActivitiesToProcess.add(genericActivity);
                            });
                        } else {
                            next = true;
                        }
                        in.close();
                    } else {
                        next = true;
                    }
                    conn.disconnect();
                }

                List<Callable<Object>> tasks = new ArrayList<>();
                PGCR_COUNT.set(genericActivitiesToProcess.size());
                genericActivitiesToProcess.forEach((activityWithSGCMembers) -> {
                    tasks.add(() -> {
                        try {
                            getSGCMemberCarnageReport(member, activityWithSGCMembers, sgcClanMembersMap);
                            if (activityWithSGCMembers.earnsPoints()) {
                                character.addClearedActivitiesWithSGCMembers(activityWithSGCMembers);
                            }
                        } catch (Exception ex) {
                            LOGGER.error(ex.getMessage(), ex);
                        }
                        return activityWithSGCMembers;
                    });
                });

                try {
                    executorService.invokeAll(tasks);
                } finally {
                    System.gc();
                    LOGGER.debug(
                            "Finished processing Cleared Activities for" + member.getCombinedBungieGlobalDisplayName());
                }

            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        });
        return PGCR_COUNT.get();
    }

    public static void getSGCMemberCarnageReport(Member member, GenericActivity activityWithSGCMembers,
            HashMap<String, Member> sgcClanMembersMap) throws IOException {
        LOGGER.debug(String.format("Processing PCGR %s for %s", activityWithSGCMembers.getUID(),
                member.getCombinedBungieGlobalDisplayName()));
        URL url = new URL(String.format("https://stats.bungie.net/Platform/Destiny2/Stats/PostGameCarnageReport/%s/",
                activityWithSGCMembers.getUID()));

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.addRequestProperty("X-API-Key", apiKey);
        conn.addRequestProperty("Accept", "Application/Json");
        conn.connect();

        // Getting the response code
        int responsecode = conn.getResponseCode();
        if (responsecode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            JsonObject response = JsonParser.parseString(content.toString()).getAsJsonObject()
                    .getAsJsonObject("Response");
            JsonArray entries = response.get("entries").getAsJsonArray();
            AtomicBoolean allSGCActivity = new AtomicBoolean(true);
            entries.forEach((entry) -> {
                String playerId = entry.getAsJsonObject().getAsJsonObject("player").getAsJsonObject("destinyUserInfo")
                        .getAsJsonPrimitive("membershipId").getAsString();
                boolean completed = entry.getAsJsonObject().getAsJsonObject("values").getAsJsonObject("completed")
                        .getAsJsonObject("basic").getAsJsonPrimitive("value").getAsDouble() == 1.0;
                double team = 0.0;
                try {
                    team = entry.getAsJsonObject().getAsJsonObject("values")
                            .getAsJsonObject("team")
                            .getAsJsonObject("basic").getAsJsonPrimitive("value")
                            .getAsDouble();
                } catch (NullPointerException ex) {

                }

                if (activityWithSGCMembers.getTeam() == team) {
                    if (!member.getUID().equals(playerId)) {
                        if (completed) {
                            if (sgcClanMembersMap.get(playerId) != null) {
                                activityWithSGCMembers.addExtraSGCClan(sgcClanMembersMap.get(playerId).getClanId());
                            } else {
                                allSGCActivity.set(false);
                            }
                        }
                    }
                }
            });
            in.close();
            activityWithSGCMembers.setAllSGCActivity(allSGCActivity.get());
        }
        conn.disconnect();
    }

    public static String getClanActivityReportAsCsv(Clan clan) {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",").append("\"Clan\",").append("\"Points\"")
                .append("\n");

        clan.getMembers()
                .forEach((memberId, member) -> {
                    stringBuilder.append("\"").append(member.getDisplayName()).append("\",")
                            .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",")
                            .append("\"").append(clan.getCallsign()).append("\",")
                            .append("\"").append(member.getWeeklySGCActivity().get("SCORE")).append("\"\n");
                });

        return stringBuilder.toString();
    }

    public static HashMap<String, String> getFullActivityReportAsCsv(List<Clan> clanList) {
        final StringBuilder pcReportBuilder = new StringBuilder();
        final StringBuilder xbReportBuilder = new StringBuilder();
        final StringBuilder psnReportBuilder = new StringBuilder();

        HashMap<String, StringBuilder> platformToReportBuilderMap = new HashMap<>();
        platformToReportBuilderMap.put("PC", pcReportBuilder);
        platformToReportBuilderMap.put("Xbox", xbReportBuilder);
        platformToReportBuilderMap.put("PSN", psnReportBuilder);

        HashMap<String, String> clanIdToPlatformMap = new HashMap<>();

        clanIdMap.forEach((platform, clanIdList) -> {
            clanIdList.forEach((clanId) -> {
                clanIdToPlatformMap.put(clanId, platform);
            });
        });

        psnReportBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",").append("\"Clan\",")
                .append("\"Points\"")
                .append("\n");

        clanList.forEach((clan) -> {
            clan.getMembers().forEach((memberId, member) -> {
                if (member.hasNewBungieName()) {
                    platformToReportBuilderMap.get(clanIdToPlatformMap.get(clan.getClanId())).append("\"")
                            .append(member.getDisplayName()).append("\",")
                            .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",")
                            .append("\"").append(clan.getCallsign()).append("\",")
                            .append("\"").append(member.getWeeklySGCActivity().get("SCORE")).append("\"\n");
                }
            });
        });
        HashMap<String, String> output = new HashMap<>();
        platformToReportBuilderMap.forEach((platform, reportBuilder) -> {
            output.put(platform, reportBuilder.toString());
        });
        return output;
    }

    private static ReentrantLock sendClanSGCActivityMessageLock = new ReentrantLock();

    public static void sendClanSGCActivityMessage(LocalDate startDate, LocalDate endDate,
            Clan clan, TextChannel textChannel, User discordUser) {
        try {
            sendClanSGCActivityMessageLock.lock();
            String clanActivityReportAsCsv = getClanActivityReportAsCsv(clan);
            new MessageBuilder().addEmbed(new EmbedBuilder()
                    .setAuthor(discordUser)
                    .setTitle(String.format(
                            "%s Activity Report",
                            clan.getCallsign()))
                    .setDescription(String.format(
                            "%s to %s",
                            startDate.toString(),
                            endDate.toString()))
                    .setFooter("#AreYouShrouded")
                    .setThumbnail(RaidReportTool.class.getClassLoader()
                            .getResourceAsStream(
                                    "thumbnail.jpg"))
                    .setColor(Color.ORANGE))
                    .addAttachment(clanActivityReportAsCsv.getBytes(),
                            String.format("%s_Weekly_Activity_Report_%s_to_%s.csv",
                                    clan.getCallsign(),
                                    startDate.toString(),
                                    endDate.toString()))
                    .send(textChannel);

        } finally {
            sendClanSGCActivityMessageLock.unlock();
        }
    }

}
