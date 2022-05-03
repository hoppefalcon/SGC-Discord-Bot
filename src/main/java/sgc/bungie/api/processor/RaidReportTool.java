/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.bungie.api.processor;

import static java.util.Map.entry;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.slf4j.Logger;

import sgc.discord.bot.BotApplication;;

/**
 * @author chris hoppe
 */

public class RaidReportTool {

    private static final Logger LOGGER = BotApplication.getLogger();
    private static String apiKey = System.getenv("BUNGIE_TOKEN");

    private static final HashMap<String, String> pcClanIdMap = new HashMap<>();
    private static final HashMap<String, String> xbClanIdMap = new HashMap<>();
    private static final HashMap<String, String> psClanIdMap = new HashMap<>();

    private static Map<Platform, List<String>> clanIdMap = Map.ofEntries(
            entry(Platform.PC,
                    Arrays.asList("2801315", "3019103", "3100797", "3087185", "3076620",
                            "3063489", "3007121", "3008645", "3095868", "2820714", "3884528",
                            "3090996", "3949151", "3915247", "3070603", "3795604")),
            entry(Platform.XBOX,
                    Arrays.asList("4327464", "4327434", "4327418", "4327389", "4418635")),
            entry(Platform.PSN,
                    Arrays.asList("4327587", "4327584", "4327575", "4327536", "4327542")));

    private static ExecutorService executorService = Executors.newFixedThreadPool(15);

    /**
     * @param args the command line arguments
     * @throws Exception
     */

    // public static void main(String[] args) throws Exception {
    // LocalDate startDate = LocalDate.parse("20220501",
    // DateTimeFormatter.BASIC_ISO_DATE);
    // LocalDate endDate = LocalDate.parse("20220502",
    // DateTimeFormatter.BASIC_ISO_DATE);
    // String bungieId = "hoppefalcon#7599";
    // Member member = RaidReportTool.getUserCommunityActivityReport(bungieId,
    // startDate,
    // endDate);
    // System.out.println(String.format(
    // "Community Activity Points: %d",
    // member.getWeeklySGCActivity()
    // .get("SCORE")));
    // }

    public static void initializeClanIdMap() {
        LOGGER.debug("Initializing Clan Map");
        clanIdMap.forEach((platform, clanIds) -> {
            clanIds.forEach((clanId) -> {
                try {
                    Clan clan = new Clan(clanId, platform);
                    getClanInfo(clan);

                    switch (platform) {
                        case PC:
                            pcClanIdMap.put(clan.getCallsign(), clan.getClanId());
                            break;
                        case XBOX:
                            xbClanIdMap.put(clan.getCallsign(), clan.getClanId());
                            break;
                        case PSN:
                            psClanIdMap.put(clan.getCallsign(), clan.getClanId());
                            break;
                    }
                } catch (IOException e) {
                    LOGGER.error("Error Initializing Clan Map", e);
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
        Clan clan = new Clan(clanId, getClanPlatform(clanId));

        getClanInfo(clan);
        getClanMembers(clan);

        return clan;
    }

    private static Platform getClanPlatform(String clanId) {
        for (Platform platform : clanIdMap.keySet()) {
            if (clanIdMap.get(platform).contains(clanId)) {
                return platform;
            }
        }
        return null;
    }

    public static Clan getClanRaidReport(Clan clan,
            InteractionOriginalResponseUpdater interactionOriginalResponseUpdater) throws Exception {
        LOGGER.trace("Processing " + clan.getName());
        final AtomicInteger count = new AtomicInteger(0);

        List<Callable<Object>> tasks = new ArrayList<>();
        clan.getMembers().forEach((memberId, member) -> {
            try {
                tasks.add(() -> {
                    getClanMemberRaidReport(member);

                    interactionOriginalResponseUpdater
                            .setContent(String.format("Building a clan raid report for %s (%d/%d)", clan.getName(),
                                    count.incrementAndGet(), clan.getMembers().size()))
                            .update().join();
                    return member;
                });
            } catch (Exception ex) {
                LOGGER.error("Error processing Clan Raid Report for " + clan.getName(), ex);
            }
        });
        executorService.invokeAll(tasks);
        LOGGER.trace("Finished Processing " + clan.getName());
        return clan;
    }

    private static void getClanMemberRaidReport(Member member) {
        LOGGER.trace("Processing " + member.getDisplayName());
        try {
            getAllMembersCharacters(member);
            getMemberRaidInfo(member);
            getUserWeeklyClears(member, LocalDate.now().minusDays(6), LocalDate.now());
        } catch (Exception e) {
            LOGGER.error("Error processing Clan Member Raid Report for " + member.getDisplayName(), e);
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
                            bungieGlobalDisplayName, bungieGlobalDisplayNameCode, clan));
                } catch (Exception ex) {
                    LOGGER.error("Error processing JSON result from "
                            + url.toExternalForm(), ex);
                }
            });
            in.close();
        }
        conn.disconnect();
    }

    public static void getAllMembersCharacters(Member member) throws IOException {
        getMembersActiveCharacters(member);
        getMembersDeletedCharacters(member);
    }

    public static void getMembersActiveCharacters(Member member) throws IOException {
        URL url = new URL(String.format("https://www.bungie.net/Platform/Destiny2/%s/Profile/%s/?components=Characters",
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
            JsonObject results = JsonParser.parseString(content.toString()).getAsJsonObject()
                    .getAsJsonObject("Response").getAsJsonObject("characters").getAsJsonObject("data");
            results.keySet().forEach((key) -> {
                try {
                    JsonElement entry = results.get(key);
                    String characterId = entry.getAsJsonObject().get("characterId").getAsString();
                    DestinyClassType classType = DestinyClassType.getByValue(
                            entry.getAsJsonObject().getAsJsonPrimitive("classType").getAsInt());

                    if (member.getCharacters().get(characterId) == null) {
                        member.getCharacters().put(characterId, new Character(characterId, classType));
                    }
                } catch (Exception ex) {
                    LOGGER.error("Error processing JSON result from "
                            + url.toExternalForm(), ex);
                }
            });
            in.close();
        }
        conn.disconnect();
    }

    public static void getMembersDeletedCharacters(Member member) throws IOException {
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

            JsonArray results = (JsonArray) JsonParser.parseString(content.toString()).getAsJsonObject()
                    .getAsJsonObject("Response").get("characters");
            results.forEach((entry) -> {
                try {
                    String characterId = entry.getAsJsonObject().get("characterId").getAsString();

                    if (member.getCharacters().get(characterId) == null) {
                        member.getCharacters().put(characterId, new Character(characterId, null));
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
                LOGGER.error("Error processing JSON result for characterID: "
                        + characterId, ex);
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
            stringBuilder.append("\"").append(member.getTotalWeeklyRaidClears()).append("\"\n");

        });
        return stringBuilder.toString();
    }

    public static byte[] getClanRaidReportAsCsvByteArray(Clan clan) throws IOException {
        return getClanRaidReportAsCsv(clan).getBytes();
    }

    public static String getUserRaidReport(String bungieId) throws Exception {
        Member user = getMemberInformationWithCharacters(bungieId, false);
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

    public static Member getMemberInformationWithCharacters(String bungieId, boolean OnlyActiveCharacters)
            throws Exception {
        String[] splitBungieId = bungieId.split("#");

        Member user = null;
        if (splitBungieId.length == 2) {
            AtomicInteger page = new AtomicInteger(0);
            boolean morePages = true;
            final HashMap<String, Member> searchResults = new HashMap<>();

            while (morePages) {
                URL url = new URL(String.format("https://www.bungie.net/Platform/User/Search/GlobalName/%d/",
                        page.getAndIncrement()));

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.addRequestProperty("X-API-Key", apiKey);
                conn.addRequestProperty("Content-Type", "Application/Json");
                conn.addRequestProperty("Accept", "Application/Json");
                conn.setDoOutput(true);

                String jsonInputString = String.format("{\"displayNamePrefix\" : \"%s\"}",
                        splitBungieId[0].trim().replace(" ", "%20"));

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

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
                                    bungieGlobalDisplayName, bungieGlobalDisplayNameCode, null));

                        } catch (Exception ex) {
                            LOGGER.error("Error processing JSON result from "
                                    + url.toExternalForm(), ex);
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
            if (user == null) {
                user = getMemberFromClanList(bungieId);
            }
            if (user != null) {
                getMembersActiveCharacters(user);
                if (!OnlyActiveCharacters) {
                    getMembersDeletedCharacters(user);
                }
            }
        }
        return user;
    }

    public static String getUserWeeklyClears(String bungieId, LocalDate startDate, LocalDate endDate) throws Exception {
        Member user = getMemberInformationWithCharacters(bungieId, true);
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
                                        LocalDate dateCompleted = ZonedDateTime.parse(dateCompletedStr)
                                                .withZoneSameInstant(ZoneId.of("US/Eastern")).toLocalDate();
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
                LOGGER.error("Error Processing Weekly Clears for " + member.getCombinedBungieGlobalDisplayName(), ex);
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
            LocalDate dateCompleted = ZonedDateTime.parse(dateCompletedStr)
                    .withZoneSameInstant(ZoneId.of("US/Eastern")).toLocalDate();

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

    public static HashMap<Platform, String> getSGCWeeklyActivityReport(LocalDate startDate, LocalDate endDate,
            InteractionOriginalResponseUpdater interactionOriginalResponseUpdater, TextChannel textChannel,
            User discordUser)
            throws IOException, InterruptedException {
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
        HashMap<String, Member> sgcClanMembersMap = initializeClanMembersMap(clanList);

        for (int i = 0; i < clanList.size(); i++) {
            Clan clan = clanList.get(i);

            List<Callable<Object>> tasks = new ArrayList<>();
            LOGGER.info("Starting to process " + clan.getCallsign());
            clan.getMembers().forEach((memberId, member) -> {
                tasks.add(() -> {
                    if (member.hasNewBungieName()) {
                        try {
                            LOGGER.debug("Starting to process " + member.getDisplayName());
                            TOTAL_PGCR_COUNT.addAndGet(
                                    getMembersClearedActivities(member, startDate, endDate,
                                            sgcClanMembersMap));
                            SCORED_PGCR_COUNT.addAndGet(member.getWeeklySGCActivity().get("COUNT"));
                            LOGGER.debug("Finished processing " + member.getDisplayName());
                        } catch (IOException ex) {
                            LOGGER.error("Error processing " + member.getDisplayName(), ex);
                        }
                    }
                    return null;
                });
            });

            try {
                executorService.invokeAll(tasks);
                sendClanSGCActivityMessage(startDate, endDate, clan, textChannel, discordUser);
            } finally {
                System.gc();
                LOGGER.info("Finished processing " + clan.getCallsign());
            }

        }

        LOGGER.info("Finished processing All Clans for SGC Activity Report");

        HashMap<Platform, String> potwActivityReportAsCsv = getFullActivityReportAsCsv(clanList);
        LOGGER.info("SGC Activity Report Complete");
        return potwActivityReportAsCsv;
    }

    public static List<Clan> initializeClanList() throws InterruptedException {
        LOGGER.debug("Initializing Clan List for SGC Activity Report");
        List<Callable<Object>> tasks = new ArrayList<>();
        ArrayList<Clan> clanList = new ArrayList<>();
        ReentrantLock clanListLock = new ReentrantLock();

        clanIdMap.forEach((platform, clanIds) -> {
            clanIds.forEach((clanId) -> {
                tasks.add(() -> {
                    try {
                        Clan clan = new Clan(clanId, platform);
                        getClanInfo(clan);
                        getClanMembers(clan);
                        clanListLock.lock();
                        clanList.add(clan);
                    } catch (IOException e) {
                        LOGGER.error("Error Processing Clan " + clanId, e);
                    } finally {
                        clanListLock.unlock();
                    }
                    return null;
                });
            });
        });

        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getMessage(), ex);
        } finally {
            LOGGER.debug("Full Clan List Initialized");
            // LOGGER.debug(String.format("%s Clan List Initialized", platform.getName()));
        }

        return clanList;
    }

    public static HashMap<String, Member> initializeClanMembersMap(List<Clan> clanList) {
        HashMap<String, Member> sgcClanMembersMap = new HashMap<>();

        for (int i = 0; i < clanList.size(); i++) {
            Clan clan = clanList.get(i);
            clan.getMembers().forEach((id, member) -> {
                sgcClanMembersMap.put(id, member);
            });
        }

        return sgcClanMembersMap;
    }

    public static int getMembersClearedActivities(Member member, LocalDate startDate, LocalDate endDate,
            HashMap<String, Member> sgcClanMembersMap) throws IOException {
        LOGGER.debug(String.format("Getting Cleared Activities for %s", member.getCombinedBungieGlobalDisplayName()));
        AtomicInteger PGCR_COUNT = new AtomicInteger(0);
        getMembersActiveCharacters(member);
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

                    LOGGER.debug(String.format("Makking HTTP call #%d for %s:%s", page + 1,
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

                            results.forEach((result) -> {
                                int mode = result.getAsJsonObject().getAsJsonObject("activityDetails")
                                        .getAsJsonPrimitive("mode").getAsInt();
                                if (!Mode.invalidModesForPOTW().contains(mode)) {
                                    String activityDateStr = result.getAsJsonObject().getAsJsonPrimitive("period")
                                            .getAsString();
                                    LocalDate dateCompleted = ZonedDateTime.parse(activityDateStr)
                                            .withZoneSameInstant(ZoneId.of("US/Eastern")).toLocalDate();
                                    if ((dateCompleted.isAfter(startDate) && dateCompleted.isBefore(endDate))
                                            || dateCompleted.isEqual(startDate) || dateCompleted.isEqual(endDate)) {
                                        boolean completed = result.getAsJsonObject().getAsJsonObject("values")
                                                .getAsJsonObject("completed")
                                                .getAsJsonObject("basic").getAsJsonPrimitive("value")
                                                .getAsDouble() == 1.0;
                                        if (completed) {
                                            String instanceId = result.getAsJsonObject()
                                                    .getAsJsonObject("activityDetails")
                                                    .getAsJsonPrimitive("instanceId").toString().replace("\"", "");
                                            double team = 0.0;
                                            try {
                                                team = result.getAsJsonObject().getAsJsonObject("values")
                                                        .getAsJsonObject("team")
                                                        .getAsJsonObject("basic").getAsJsonPrimitive("value")
                                                        .getAsDouble();
                                            } catch (NullPointerException ex) {
                                            }
                                            GenericActivity genericActivity = new GenericActivity(instanceId,
                                                    Mode.getFromValue(mode), member.getClan().getClanPlatform());
                                            genericActivity.setTeam(team);
                                            genericActivitiesToProcess.add(genericActivity);
                                        }
                                    } else if (dateCompleted.isAfter(endDate)) {
                                        recordsAfterEndDate.incrementAndGet();
                                    }
                                }
                            });

                            LOGGER.debug(String.format(
                                    "Finished processing HTTP call #%d for %s:%s", page + 1,
                                    member.getCombinedBungieGlobalDisplayName(), character.getUID()));

                            next = (results.size() < 250) || (recordsAfterEndDate.get() == results.size());
                        } else {
                            next = true;
                        }
                        in.close();
                    } else {
                        next = true;
                    }
                    conn.disconnect();
                }

                PGCR_COUNT.addAndGet(genericActivitiesToProcess.size());
                genericActivitiesToProcess.forEach((activityWithSGCMembers) -> {
                    try {
                        LOGGER.debug(String.format(
                                "Processing Activity %s for %s (%s)",
                                activityWithSGCMembers.getUID(), member.getCombinedBungieGlobalDisplayName(),
                                character.getUID()));
                        getSGCMemberCarnageReport(member, activityWithSGCMembers, sgcClanMembersMap);
                        if (activityWithSGCMembers.earnsPoints()) {
                            character.addClearedActivitiesWithSGCMembers(activityWithSGCMembers);
                        }
                    } catch (Exception ex) {
                        LOGGER.error(String.format(
                                "Error Processing Activity %s for %s (%s)",
                                activityWithSGCMembers.getUID(), member.getCombinedBungieGlobalDisplayName(),
                                character.getUID()), ex);
                    }
                });

                LOGGER.debug(
                        "Finished Processing Cleared Activities for " + member.getCombinedBungieGlobalDisplayName());

            } catch (Exception ex) {
                LOGGER.error(
                        "Error Processing Cleared Activities for "
                                + member.getCombinedBungieGlobalDisplayName(),
                        ex);
            }
        });
        LOGGER.info(String.format("PGCR Count for %s is %d", member.getCombinedBungieGlobalDisplayName(),
                PGCR_COUNT.get()));
        return PGCR_COUNT.get();
    }

    public static void getSGCMemberCarnageReport(Member member, GenericActivity activityWithSGCMembers,
            HashMap<String, Member> sgcClanMembersMap) throws IOException {
        LOGGER.debug(String.format("Processing PGCR %s for %s", activityWithSGCMembers.getUID(),
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

                boolean completed = entry.getAsJsonObject().getAsJsonObject("values").getAsJsonObject("completed")
                        .getAsJsonObject("basic").getAsJsonPrimitive("value").getAsDouble() == 1.0;

                if (completed) {

                    String playerId = entry.getAsJsonObject().getAsJsonObject("player")
                            .getAsJsonObject("destinyUserInfo")
                            .getAsJsonPrimitive("membershipId").getAsString();

                    if (!member.getUID().equals(playerId)) {

                        double team = 0.0;

                        try {

                            team = entry.getAsJsonObject().getAsJsonObject("values")
                                    .getAsJsonObject("team")
                                    .getAsJsonObject("basic").getAsJsonPrimitive("value")
                                    .getAsDouble();

                        } catch (NullPointerException ex) {
                        }

                        if (activityWithSGCMembers.getTeam() == team) {
                            if (sgcClanMembersMap.get(playerId) != null) {
                                if (!sgcClanMembersMap.get(playerId).getClan().equals(member.getClan())) {
                                    activityWithSGCMembers.addOtherSGCClan(sgcClanMembersMap.get(playerId).getClan());
                                }
                                activityWithSGCMembers.addOtherSGCMember();
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

        stringBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",").append("\"Clan\",")
                .append("\"Community POTW Points\",").append("\"Titan Clears\",").append("\"Hunter Clears\",")
                .append("\"Warlock Clears\"").append("\n");

        stringBuilder.append(getClanActivityCsvPart(clan));

        return stringBuilder.toString();
    }

    private static String getClanActivityCsvPart(Clan clan) {
        final StringBuilder csvPart = new StringBuilder();

        clan.getMembers()
                .forEach((memberId, member) -> {
                    Character titanCharacter = member.getCharacterByDestinyClassType(DestinyClassType.TITAN);
                    Character hunterCharacter = member.getCharacterByDestinyClassType(DestinyClassType.HUNTER);
                    Character warlockCharacter = member.getCharacterByDestinyClassType(DestinyClassType.WARLOCK);

                    int titanClears = titanCharacter != null ? titanCharacter.getActivitiesWithSGCMembersCount() : 0;

                    int hunterClears = hunterCharacter != null ? hunterCharacter.getActivitiesWithSGCMembersCount() : 0;

                    int warlockClears = warlockCharacter != null ? warlockCharacter.getActivitiesWithSGCMembersCount()
                            : 0;

                    if (member.hasNewBungieName()) {
                        csvPart.append("\"").append(member.getDisplayName()).append("\",")
                                .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",")
                                .append("\"").append(clan.getCallsign()).append("\",")
                                .append("\"").append(member.getWeeklySGCActivity().get("SCORE")).append("\",")
                                .append("\"").append(titanClears).append("\",")
                                .append("\"").append(hunterClears).append("\",")
                                .append("\"").append(warlockClears).append("\"\n");
                    }
                });

        return csvPart.toString();
    }

    public static HashMap<Platform, String> getFullActivityReportAsCsv(List<Clan> clanList) {
        HashMap<Platform, StringBuilder> platformToReportBuilderMap = new HashMap<>();
        platformToReportBuilderMap.put(Platform.PC, new StringBuilder());
        platformToReportBuilderMap.put(Platform.XBOX, new StringBuilder());
        platformToReportBuilderMap.put(Platform.PSN, new StringBuilder());

        platformToReportBuilderMap.values().forEach((strBuilder) -> {
            strBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",").append("\"Clan\",")
                    .append("\"Community POTW Points\",").append("\"Titan Clears\",").append("\"Hunter Clears\",")
                    .append("\"Warlock Clears\"").append("\n");
        });

        clanList.forEach((clan) -> {
            platformToReportBuilderMap.get(clan.getClanPlatform())
                    .append(getClanActivityCsvPart(clan));
        });
        HashMap<Platform, String> output = new HashMap<>();
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
            new MessageBuilder()
                    .setContent(
                            String.format("%s Activity Report",
                                    clan.getCallsign()))
                    .addEmbed(
                            new EmbedBuilder()
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

    public static Member getUserCommunityActivityReport(String userBungieId, LocalDate startDate,
            LocalDate endDate)
            throws IOException, InterruptedException {
        LOGGER.info("Starting User SGC Activity Report");

        List<Clan> clanList = initializeClanList();
        HashMap<String, Member> sgcClanMembersMap = initializeClanMembersMap(clanList);

        Member member = getMemberFromMap(userBungieId, sgcClanMembersMap);

        if (member != null) {
            try {
                LOGGER.debug("Starting to process " + member.getDisplayName());
                getMembersClearedActivities(member, startDate, endDate,
                        sgcClanMembersMap);
                LOGGER.debug("Finished processing " + member.getDisplayName());
            } catch (IOException ex) {
                LOGGER.error("Error processing " + member.getDisplayName(), ex);
            }
        }

        return member;
    }

    private static Member getMemberFromMap(String userBungieId, HashMap<String, Member> sgcClanMembersMap) {
        for (Member member : sgcClanMembersMap.values()) {
            if (member.getCombinedBungieGlobalDisplayName().equalsIgnoreCase(userBungieId)) {
                return member;
            }
        }
        return null;
    }

    private static Member getMemberFromClanList(String userBungieId) throws InterruptedException {

        List<Clan> clanList = initializeClanList();
        HashMap<String, Member> sgcClanMembersMap = initializeClanMembersMap(clanList);

        return getMemberFromMap(userBungieId, sgcClanMembersMap);
    }

}