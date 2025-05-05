/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.bungie.api.processor;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sgc.discord.infographics.GoogleDriveUtil;
import sgc.types.Platform;
import sgc.types.SGC_Clan;

/**
 * @author chris hoppe
 */

public class RaidReportTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaidReportTool.class);

    private static String apiKey = System.getenv("BUNGIE_TOKEN");

    private static final HashMap<String, String> pcClanIdMap = new HashMap<>();
    private static final HashMap<String, String> xbClanIdMap = new HashMap<>();
    private static final HashMap<String, String> psClanIdMap = new HashMap<>();

    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    public static ReentrantLock resourceLock = new ReentrantLock();

    private static final int MAX_RETRIES = 5; // Maximum times to retry a Bungie API Call
    private static final int TIME_DELAY = 30000; // Time Delay in Milliseconds between each Bungie API retry

    /**
     * Initializes the clan ID maps.
     */
    public static void initializeClanIdMap() {
        LOGGER.debug("Initializing Clan Map");

        // Clear the existing maps
        pcClanIdMap.clear();
        xbClanIdMap.clear();
        psClanIdMap.clear();

        // Iterate over each SGC_Clan enum value
        for (SGC_Clan clan : SGC_Clan.values()) {
            try {
                // Add the clan ID to the appropriate map based on the primary platform
                String clanName = clan.name();
                String bungieId = clan.Bungie_ID;
                Platform primaryPlatform = clan.Primary_Platform;

                switch (primaryPlatform) {
                    case PC:
                        pcClanIdMap.put(clanName, bungieId);
                        break;
                    case XBOX:
                        xbClanIdMap.put(clanName, bungieId);
                        break;
                    case PSN:
                        psClanIdMap.put(clanName, bungieId);
                        break;
                }
            } catch (Exception e) {
                LOGGER.error("Error Initializing Clan Map", e);
            }
        }
        LOGGER.debug("Clan Map Initialized");
    }

    /**
     * Retrieves the PC clan ID map.
     *
     * @return the PC clan ID map
     */
    public static HashMap<String, String> getPcClanIdMap() {
        return pcClanIdMap;
    }

    /**
     * Retrieves the Xbox clan ID map.
     *
     * @return the Xbox clan ID map
     */
    public static HashMap<String, String> getXbClanIdMap() {
        return xbClanIdMap;
    }

    /**
     * Retrieves the PlayStation clan ID map.
     *
     * @return the PlayStation clan ID map
     */
    public static HashMap<String, String> getPsClanIdMap() {
        return psClanIdMap;
    }

    /**
     * Retrieves the clan information for the given clan ID.
     *
     * @param clanId the clan ID
     * @return the Clan object containing the clan information
     * @throws Exception if an error occurs while retrieving the clan information
     */
    public static Clan getClanInformation(String clanId) throws Exception {
        Clan clan = new Clan(clanId, getClanPlatform(clanId));

        getClanInfo(clan);
        getClanMembers(clan);

        return clan;
    }

    /**
     * Retrieves the primary platform for the clan with the given clan ID.
     *
     * @param clanId the clan ID
     * @return the primary platform of the clan
     */
    private static Platform getClanPlatform(String clanId) {
        return SGC_Clan.getClansPrimaryPlatform(clanId);
    }

    /**
     * Retrieves the raid report for the given clan.
     *
     * @param clan                               the clan for which to retrieve the
     *                                           raid report
     * @param interactionOriginalResponseUpdater the updater for the interaction
     *                                           response
     * @return the clan with the updated raid report
     * @throws Exception if an error occurs while retrieving the raid report
     */
    public static Clan getClanRaidReport(Clan clan,
            InteractionOriginalResponseUpdater interactionOriginalResponseUpdater)
            throws Exception {
        LOGGER.trace("Processing " + clan.getName());
        final AtomicInteger count = new AtomicInteger(0);

        List<Callable<Object>> tasks = new ArrayList<>();
        clan.getMembers().forEach((memberId, member) -> {
            tasks.add(() -> {

                try {
                    getClanMemberRaidReport(member);

                    interactionOriginalResponseUpdater
                            .setContent(String.format("Building a clan raid report for %s (%d/%d)",
                                    clan.getName(), count.incrementAndGet(), clan.getMembers().size()))
                            .update().join();
                } catch (Exception ex) {
                    LOGGER.error("Error processing Clan Raid Report for " + clan.getName(), ex);
                }
                return member;
            });
        });
        executorService.invokeAll(tasks);
        LOGGER.trace("Finished Processing " + clan.getName());
        return clan;
    }

    /**
     * Retrieves the raid report for the given clan member.
     *
     * @param member the clan member for which to retrieve the raid report
     */
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

    /**
     * Retrieves the clan information for the given clan.
     *
     * @param clan the clan for which to retrieve the information
     * @throws IOException        if an I/O error occurs while making the request
     * @throws URISyntaxException if the URI syntax is invalid
     */
    public static void getClanInfo(Clan clan) throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://www.bungie.net/Platform/GroupV2/%s/", clan.getClanId())).toURL();

        HttpURLConnection conn = getBungieAPIResponse(url,
                String.format("getClanInfo: %s <%s>", clan.getCallsign(), clan.getClanId()));
        if (conn != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                JsonObject json = JsonParser.parseString(content.toString()).getAsJsonObject();
                JsonObject response = json.getAsJsonObject("Response");
                JsonObject detail = response.getAsJsonObject("detail");

                clan.setName(detail.get("name").getAsString());
                clan.setCallsign(detail.getAsJsonObject("clanInfo").get("clanCallsign").getAsString());
                in.close();
            }

            conn.disconnect();
        }
    }

    /**
     * Retrieves the members of the clan.
     *
     * @param clan the clan for which to retrieve the members
     * @throws IOException          if an I/O error occurs while making the request
     * @throws URISyntaxException   if the URI syntax is invalid
     * @throws InterruptedException
     */
    public static void getClanMembers(Clan clan) throws IOException, URISyntaxException, InterruptedException {
        URL url = new URI(String.format("https://www.bungie.net/Platform/GroupV2/%s/Members/", clan.getClanId()))
                .toURL();

        HttpURLConnection conn = getBungieAPIResponse(url,
                String.format("getClanMembers: %s <%s>", clan.getCallsign(), clan.getClanId()));
        if (conn != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                JsonObject json = JsonParser.parseString(content.toString()).getAsJsonObject();
                JsonArray results = json.getAsJsonObject("Response").getAsJsonArray("results");
                results.forEach((entry) -> {

                    try {
                        JsonObject userInfo = entry.getAsJsonObject().getAsJsonObject("destinyUserInfo");
                        String membershipType = userInfo.get("membershipType").getAsString();
                        String membershipId = userInfo.get("membershipId").getAsString();
                        String displayName = userInfo.get("displayName").getAsString();

                        try {
                            String bungieGlobalDisplayName = userInfo.get("bungieGlobalDisplayName").getAsString();
                            String bungieGlobalDisplayNameCode = userInfo.get("bungieGlobalDisplayNameCode")
                                    .getAsString();

                            clan.getMembers().put(membershipId,
                                    new Member(membershipId, displayName, membershipType,
                                            bungieGlobalDisplayName, bungieGlobalDisplayNameCode, clan));
                        } catch (NullPointerException ex) {
                            LOGGER.info(displayName + " has yet to register for a bungieGlobalDisplayName");
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Error processing JSON result from " + url.toExternalForm(), ex);
                    }

                });
            }

            conn.disconnect();
        }
    }

    /**
     * Retrieves all active characters of a member.
     *
     * @param member the member for which to retrieve the active characters
     * @throws IOException        if an I/O error occurs while making the request
     * @throws URISyntaxException if the URI syntax is invalid
     */
    public static void getAllMembersCharacters(Member member) throws IOException, URISyntaxException {
        getMembersActiveCharacters(member);
        getMembersDeletedCharacters(member);
    }

    /**
     * Retrieves the active characters of a member.
     *
     * @param member the member for which to retrieve the active characters
     * @throws IOException        if an I/O error occurs while making the request
     * @throws URISyntaxException if the URI syntax is invalid
     */
    public static void getMembersActiveCharacters(Member member) throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://www.bungie.net/Platform/Destiny2/%s/Profile/%s/?components=Characters",
                member.getMemberType(), member.getUID())).toURL();
        HttpURLConnection conn = getBungieAPIResponse(url,
                String.format("getMembersActiveCharacters: %s", member.getCombinedBungieGlobalDisplayName()));
        if (conn != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                JsonObject json = JsonParser.parseString(content.toString()).getAsJsonObject();
                JsonObject results = json.getAsJsonObject("Response").getAsJsonObject("characters")
                        .getAsJsonObject("data");
                results.keySet().forEach((key) -> {
                    try {
                        JsonElement entry = results.get(key);
                        String characterId = entry.getAsJsonObject().get("characterId").getAsString();
                        DestinyClassType classType = DestinyClassType.getByValue(
                                entry.getAsJsonObject().getAsJsonPrimitive("classType").getAsInt());

                        String dateLastPlayed = entry.getAsJsonObject().get("dateLastPlayed").getAsString();

                        if (member.getCharacters().get(characterId) == null) {
                            member.getCharacters().put(characterId,
                                    new Character(characterId, classType, dateLastPlayed));
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Error processing JSON result from " + url.toExternalForm(), ex);
                    }
                });
                in.close();
            }
            conn.disconnect();
        }
    }

    /**
     * Retrieves and processes the deleted characters for a given member.
     *
     * This method sends a request to the Bungie API to retrieve information about
     * the deleted characters
     * associated with the specified member. It parses the JSON response and adds
     * new character entries
     * to the member's character collection if they are not already present.
     *
     * @param member The member object representing the player.
     * @throws IOException        If an I/O error occurs while making the API
     *                            request.
     * @throws URISyntaxException If there is an error in the URI syntax for the API
     *                            URL.
     */
    public static void getMembersDeletedCharacters(Member member) throws IOException, URISyntaxException {
        // Construct the API URL
        URL url = new URI(String.format("https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Stats",
                member.getMemberType(), member.getUID())).toURL();

        HttpURLConnection conn = null;
        BufferedReader in = null;

        try {
            // Open connection and set request properties
            conn = getBungieAPIResponse(url,
                    String.format("getMembersDeletedCharacters: %s", member.getCombinedBungieGlobalDisplayName()));

            // Read the response
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            // Parse the JSON response
            JsonObject response = JsonParser.parseString(content.toString()).getAsJsonObject()
                    .getAsJsonObject("Response");
            JsonArray characters = response.getAsJsonArray("characters");

            // Process each character entry
            characters.forEach(entry -> {
                try {
                    String characterId = entry.getAsJsonObject().get("characterId").getAsString();

                    // Add new character entry if not already present
                    if (member.getCharacters().get(characterId) == null) {
                        member.getCharacters().put(characterId, new Character(characterId, null, null));
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            });

        } finally {
            // Close resources in the finally block
            if (in != null) {
                in.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static void getMemberRaidInfo(Member member) throws IOException {
        List<String> validRaidHashes = Raid.getAllValidRaidHashes();
        member.getCharacters().forEach((characterId, character) -> {
            try {
                URL url = new URI(String.format(
                        "https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Character/%s/Stats/AggregateActivityStats",
                        member.getMemberType(), member.getUID(), character.getUID())).toURL();
                HttpURLConnection conn = getBungieAPIResponse(url,
                        String.format("getMemberRaidInfo : %s[%s]", member.getCombinedBungieGlobalDisplayName(),
                                characterId));
                if (conn != null) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuffer content = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
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
                    conn.disconnect();
                }
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

    public static String getUserDungeonReport(String bungieId) throws Exception {
        Member user = getMemberInformationWithCharacters(bungieId, false);
        final StringBuilder response = new StringBuilder();

        if (user != null) {
            getMemberDungeonInfo(user);
            HashMap<Dungeon, Integer> dungeonClears = user.getDungeonClears();
            Dungeon.getDungeonsOrdered().forEach((Dungeon dungeon) -> {
                response.append(dungeon.name).append(": ").append(dungeonClears.get(dungeon)).append("\n");
            });
            response.append("\nTOTAL: ").append(user.getTotalRaidClears());
        }
        return response.toString();
    }

    public static Member getMemberInformation(String bungieId)
            throws Exception {
        String[] splitBungieId = bungieId.split("#");

        Member user = null;
        if (splitBungieId.length == 2) {
            AtomicInteger page = new AtomicInteger(0);
            final HashMap<String, Member> searchResults = new HashMap<>();

            URL url = new URI(
                    String.format("https://www.bungie.net/Platform/Destiny2/SearchDestinyPlayerByBungieName/-1/",
                            page.getAndIncrement()))
                    .toURL();

            String jsonInputString = String.format("{\"displayName\" : \"%s\", displayNameCode: \"%s\"}",
                    splitBungieId[0], splitBungieId[1]);

            HttpURLConnection conn = getBungieAPIResponseWithJsonInput(url,
                    String.format("Get Member Information: %s", bungieId),
                    jsonInputString);

            if (conn != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuffer content = new StringBuffer();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                JsonArray results = (JsonArray) JsonParser.parseString(content.toString()).getAsJsonObject()
                        .get("Response");

                results.forEach((entry) -> {
                    try {
                        JsonArray applicableMembershipTypes = entry.getAsJsonObject()
                                .getAsJsonArray("applicableMembershipTypes");
                        if (applicableMembershipTypes.size() > 0) {
                            String membershipType = entry.getAsJsonObject().get("membershipType")
                                    .getAsString();
                            String membershipId = entry.getAsJsonObject().get("membershipId").getAsString();
                            String displayName = entry.getAsJsonObject().get("displayName").getAsString();
                            String bungieGlobalDisplayName = "";
                            String bungieGlobalDisplayNameCode = "";
                            try {
                                bungieGlobalDisplayName = entry.getAsJsonObject()
                                        .get("bungieGlobalDisplayName").getAsString();
                                bungieGlobalDisplayNameCode = entry.getAsJsonObject()
                                        .get("bungieGlobalDisplayNameCode").getAsString();
                            } catch (NullPointerException ex) {

                            }
                            searchResults.put(membershipId, new Member(membershipId, displayName, membershipType,
                                    bungieGlobalDisplayName, bungieGlobalDisplayNameCode, null));
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Error processing JSON result from "
                                + url.toExternalForm(), ex);
                    }
                });

                in.close();
            }
            conn.disconnect();

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
        }
        return user;
    }

    public static Member getMemberInformationWithCharacters(String bungieId, boolean OnlyActiveCharacters)
            throws Exception {
        Member user = getMemberInformation(bungieId);

        if (user != null) {
            getMembersActiveCharacters(user);
            if (!OnlyActiveCharacters) {
                getMembersDeletedCharacters(user);
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
                    URL url = new URI(String.format(
                            "https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Character/%s/Stats/Activities/?page=%d&mode=4&count=250",
                            member.getMemberType(), member.getUID(), character.getUID(), page)).toURL();

                    HttpURLConnection conn = getBungieAPIResponse(url,
                            String.format("getUserWeeklyClears: %s", member.getCombinedBungieGlobalDisplayName()));

                    if (conn != null) {
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
                        conn.disconnect();
                    } else {
                        next = true;
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Error Processing Weekly Clears for " + member.getCombinedBungieGlobalDisplayName(), ex);
            }
        });
        return member;
    }

    public static RaidCarnageReport getRaidCarnageReport(String carnageReportId)
            throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://stats.bungie.net/Platform/Destiny2/Stats/PostGameCarnageReport/%s/",
                carnageReportId)).toURL();

        HttpURLConnection conn = getBungieAPIResponse(url, String.format("getRaidCarnageReport: %s", carnageReportId));
        RaidCarnageReport raidCarnageReport = null;

        if (conn != null) {
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

            conn.disconnect();
        }
        return raidCarnageReport;

    }

    public static HashMap<Platform, String> getSGCWeeklyActivityReport(LocalDate startDate, LocalDate endDate,
            InteractionOriginalResponseUpdater interactionOriginalResponseUpdater, TextChannel textChannel,
            User discordUser)
            throws IOException, InterruptedException {
        try {
            resourceLock.lock();
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
                                                sgcClanMembersMap, 0, false));

                                SCORED_PGCR_COUNT.addAndGet(member.getWeeklySGCActivity().get("COUNT"));
                                LOGGER.debug("Finished processing " + member.getDisplayName());
                            } catch (Throwable ex) {
                                LOGGER.error("Error processing " + member.getDisplayName(), ex);
                            }
                        }
                        return null;
                    });
                });

                try {
                    executorService.invokeAll(tasks);
                    if (textChannel != null && discordUser != null) {
                        sendClanSGCActivityMessage(startDate, endDate, clan, textChannel, discordUser);
                    }
                } finally {

                    LOGGER.info("Finished processing " + clan.getCallsign());
                }

            }

            LOGGER.info("Finished processing All Clans for SGC Activity Report");

            HashMap<Platform, String> potwActivityReportAsCsv = getPlatformActivityReportsAsCsv(clanList);
            LOGGER.info("SGC Activity Report Complete");
            return potwActivityReportAsCsv;
        } finally {
            resourceLock.unlock();
        }
    }

    public static HashMap<Platform, String> getSGCAnnualActivityReport(LocalDate startDate, LocalDate endDate,
            InteractionOriginalResponseUpdater interactionOriginalResponseUpdater, TextChannel textChannel,
            User discordUser)
            throws IOException, InterruptedException {
        LOGGER.info("Starting SGC Annual Activity Report");

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
        HashMap<Member, HashMap<String, Integer>> annualValues = new HashMap<>();
        List<LocalDate> weeksList = new ArrayList<>();
        LocalDate currentStartDate = startDate;
        while (currentStartDate.isBefore(endDate)) {
            weeksList.add(currentStartDate);
            currentStartDate = currentStartDate.plusDays(7);
        }

        List<Callable<Object>> tasks = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        final int totalMembers = sgcClanMembersMap.size();
        for (int i = 0; i < clanList.size(); i++) {
            Clan clan = clanList.get(i);

            clan.getMembers().forEach((memberId, member) -> {
                tasks.add(() -> {
                    if (member.hasNewBungieName()) {
                        for (int k = 0; k < weeksList.size(); k++) {

                            try {
                                LOGGER.debug("Starting to process " + member.getDisplayName());
                                TOTAL_PGCR_COUNT.addAndGet(
                                        getMembersClearedActivities(member, weeksList.get(k),
                                                weeksList.get(k).plusDays(6),
                                                sgcClanMembersMap, 0, true));

                                SCORED_PGCR_COUNT.addAndGet(member.getWeeklySGCActivity().get("COUNT"));
                                appendWeeklyResultsToAnnual(member, annualValues);
                                LOGGER.info(String.format("Finished processing %s week %d/%d", member.getDisplayName(),
                                        k + 1, weeksList.size()));
                            } catch (Throwable ex) {
                                LOGGER.error("Error processing " + member.getDisplayName(), ex);
                            } finally {
                                member.zeroOut();
                            }
                        }
                    }
                    LOGGER.info(String.format("Finished processing %d/%d", completed.incrementAndGet(), totalMembers));
                    return null;
                });
            });
        }
        try {
            executorService.invokeAll(tasks);
        } finally {

        }

        LOGGER.info("Finished processing All Clans for SGC Activity Report");

        HashMap<Platform, String> potwAnnualActivityReportAsCsv = getPlatformAnnualActivityReportsAsCsv(clanList,
                annualValues);
        LOGGER.info("SGC Annual Activity Report Complete");
        return potwAnnualActivityReportAsCsv;
    }

    private static void appendWeeklyResultsToAnnual(Member member,
            HashMap<Member, HashMap<String, Integer>> annualValues) {
        HashMap<Mode, Integer> totalActivitiesWithSGCMembersByMode = member
                .getTotalActivitiesWithSGCMembersByMode();
        Map<Mode, Integer> potwModeCompletions = member.getPOTWModeCompletions();
        Map<Raid, Integer> potwRaidCompletions = member.getPOTWRaidCompletions();
        Map<Dungeon, Integer> potwDungeonCompletions = member.getPOTWDungeonCompletions();
        Character titanCharacter = member.getCharacterByDestinyClassType(DestinyClassType.TITAN);
        Character hunterCharacter = member.getCharacterByDestinyClassType(DestinyClassType.HUNTER);
        Character warlockCharacter = member.getCharacterByDestinyClassType(DestinyClassType.WARLOCK);
        int titanClears = titanCharacter != null ? titanCharacter.getActivitiesWithSGCMembersCount() : 0;

        int hunterClears = hunterCharacter != null ? hunterCharacter.getActivitiesWithSGCMembersCount() : 0;

        int warlockClears = warlockCharacter != null ? warlockCharacter.getActivitiesWithSGCMembersCount()
                : 0;
        HashMap<String, Integer> values = annualValues.get(member);
        if (values == null) {
            values = new HashMap<>();
        }

        if (member.hasNewBungieName()) {
            // Base CPOTW
            setAnnualValue(values, "Community POTW Points", member.getWeeklySGCActivity().get("SCORE"));

            setAnnualValue(values, "Titan Clears", titanClears);
            setAnnualValue(values, "Hunter Clears", hunterClears);
            setAnnualValue(values, "Warlock Clears", warlockClears);
            for (Mode mode : Mode.validModesForCPOTW()) {
                setAnnualValue(values, mode.getName(), totalActivitiesWithSGCMembersByMode.get(mode));
            }
            // POTW Modes
            for (Mode mode : Mode.validModesForPOTW()) {
                setAnnualValue(values, mode.getName() + " [POTW]", potwModeCompletions.get(mode));
            }
            // POTW Raids
            for (Raid raid : Raid.getRaidsOrdered()) {
                setAnnualValue(values, raid.getName() + " [POTW]", potwRaidCompletions.get(raid));
            }
            // POTW Dungeons
            for (Dungeon dungeon : Dungeon.getDungeonsOrdered()) {
                setAnnualValue(values, dungeon.getName() + " [POTW]", potwDungeonCompletions.get(dungeon));
            }
        }
        annualValues.put(member, values);
    }

    private static void setAnnualValue(HashMap<String, Integer> values, String key, Integer value) {
        Integer currentValue = values.get(key);
        if (currentValue == null) {
            currentValue = value;
        } else {
            currentValue += value;
        }
        values.put(key, currentValue);
    }

    public static String getClanInternalActivityReport(SGC_Clan sgc_clan, LocalDate startDate, LocalDate endDate,
            InteractionOriginalResponseUpdater interactionOriginalResponseUpdater)
            throws IOException, InterruptedException, URISyntaxException {
        LOGGER.info(String.format("Starting %s Internal Activity Report", sgc_clan.name()));

        if (interactionOriginalResponseUpdater != null) {
            interactionOriginalResponseUpdater.setContent(String
                    .format("Building a %s internal activity report from %s to %s\nThis will take a while.",
                            sgc_clan.name(),
                            startDate,
                            endDate))
                    .update().join();
        }
        Clan clan = initializeClan(sgc_clan);
        HashMap<String, Member> sgcClanMembersMap = initializeClanMembersMap(List.of(clan));

        List<Callable<Object>> tasks = new ArrayList<>();
        LOGGER.info("Starting to process " + clan.getCallsign());
        clan.getMembers().forEach((memberId, member) -> {
            tasks.add(() -> {

                if (member.hasNewBungieName()) {
                    try {
                        LOGGER.debug("Starting to process " + member.getDisplayName());
                        getMembersClearedActivities(member, startDate, endDate,
                                sgcClanMembersMap, 0, false);
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
        } finally {

            LOGGER.info("Finished processing " + clan.getCallsign());
        }
        return getClanInternalActivityReportAsCsv(clan);
    }

    public static List<Clan> initializeClanList() throws InterruptedException {
        LOGGER.debug("Initializing Clan List for SGC Activity Report");
        List<Callable<Object>> tasks = new ArrayList<>();
        ArrayList<Clan> clanList = new ArrayList<>();
        ReentrantLock clanListLock = new ReentrantLock();

        for (SGC_Clan sgc_clan : SGC_Clan.values()) {
            tasks.add(() -> {

                try {
                    Clan clan = new Clan(sgc_clan.Bungie_ID, sgc_clan.Primary_Platform);
                    getClanInfo(clan);
                    getClanMembers(clan);
                    clanListLock.lock();
                    clanList.add(clan);
                } catch (IOException e) {
                    LOGGER.error("Error Processing Clan " + sgc_clan.Bungie_ID, e);
                } finally {
                    clanListLock.unlock();
                }
                return null;
            });
        }

        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getMessage(), ex);
        } finally {
            LOGGER.debug("Full Clan List Initialized");
        }

        return clanList;
    }

    public static Clan initializeClan(SGC_Clan sgc_clan) throws IOException, URISyntaxException, InterruptedException {
        Clan clan = new Clan(sgc_clan.Bungie_ID, sgc_clan.Primary_Platform);
        getClanInfo(clan);
        getClanMembers(clan);
        return clan;
    }

    public static Clan initializeClan(String bungieID) throws IOException, URISyntaxException, InterruptedException {
        Clan clan = new Clan(bungieID, Platform.PC);
        getClanInfo(clan);
        getClanMembers(clan);
        return clan;
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
            HashMap<String, Member> sgcClanMembersMap, int mode, boolean potwOnly)
            throws IOException, URISyntaxException, InterruptedException {
        LOGGER.debug(String.format("Getting Cleared Activities for %s", member.getCombinedBungieGlobalDisplayName()));
        AtomicInteger PGCR_COUNT = new AtomicInteger(0);
        getMembersActiveCharacters(member);
        member.getCharacters().forEach((characteruid, character) -> {
            try {
                boolean next = false;
                List<GenericActivity> genericActivitiesToProcess = new ArrayList<>();

                for (int page = 0; !next; page++) {
                    URL url = new URI(String.format(
                            "https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Character/%s/Stats/Activities/?page=%d&mode=%d&count=250",
                            member.getMemberType(), member.getUID(), character.getUID(), page, mode)).toURL();

                    HttpURLConnection conn = getBungieAPIResponse(url, String.format(
                            "getMembersClearedActivities: %s <%d>", member.getCombinedBungieGlobalDisplayName(), page));

                    if (conn != null) {
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
                                int activityMode = result.getAsJsonObject().getAsJsonObject("activityDetails")
                                        .getAsJsonPrimitive("mode").getAsInt();

                                if (Mode.validModeValuesForCPOTW().contains(activityMode)) {
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
                                            if (!potwOnly) {
                                                String instanceId = result.getAsJsonObject()
                                                        .getAsJsonObject("activityDetails")
                                                        .getAsJsonPrimitive("instanceId").toString().replace("\"", "");
                                                double team = 0.0;
                                                try {
                                                    team = result.getAsJsonObject().getAsJsonObject("values")
                                                            .getAsJsonObject("team")
                                                            .getAsJsonObject("basic").getAsJsonPrimitive("value")
                                                            .getAsDouble();
                                                    GenericActivity genericActivity = new GenericActivity(instanceId,
                                                            Mode.getFromValue(activityMode),
                                                            member.getClan().getClanPlatform());
                                                    genericActivity.setTeam(team);
                                                    genericActivitiesToProcess.add(genericActivity);
                                                } catch (Throwable ex) {
                                                    LOGGER.error(ex.getMessage(), ex);
                                                }
                                            }

                                            // POTW Calculations

                                            if (Mode.getFromValue(activityMode).equals(Mode.RAID)) {
                                                Raid raid = Raid.getRaid(result.getAsJsonObject()
                                                        .getAsJsonObject("activityDetails")
                                                        .getAsJsonPrimitive("directorActivityHash")
                                                        .getAsString());
                                                if (raid == null) {
                                                    LOGGER.error(result.getAsJsonObject()
                                                            .getAsJsonObject("activityDetails")
                                                            .getAsJsonPrimitive("directorActivityHash")
                                                            .getAsString()
                                                            + " is returning as Mode.RAID but is not associated to a raid.");
                                                } else {
                                                    character.addCompletedRaid(raid);
                                                }
                                            } else if (Mode.getFromValue(activityMode).equals(Mode.DUNGEON)) {
                                                Dungeon dungeon = Dungeon.getDungeon(result.getAsJsonObject()
                                                        .getAsJsonObject("activityDetails")
                                                        .getAsJsonPrimitive("directorActivityHash")
                                                        .getAsString());
                                                if (dungeon == null) {
                                                    LOGGER.error(result.getAsJsonObject()
                                                            .getAsJsonObject("activityDetails")
                                                            .getAsJsonPrimitive("directorActivityHash")
                                                            .getAsString()
                                                            + " is returning as Mode.DUNGEON but is not associated to a dungeon.");
                                                } else {
                                                    character.addCompletedDungeon(dungeon);
                                                }
                                            } else {
                                                character.addCompletedMode(Mode.getFromValue(activityMode));
                                            }
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
                        conn.disconnect();
                    } else {
                        next = true;
                    }
                }

                PGCR_COUNT.addAndGet(genericActivitiesToProcess.size());
                if (!potwOnly) {
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
                }

                LOGGER.debug(
                        "Finished Processing Cleared Activities for "
                                + member.getCombinedBungieGlobalDisplayName());

            } catch (Exception ex) {
                LOGGER.error(
                        "Error Processing Cleared Activities for "
                                + member.getCombinedBungieGlobalDisplayName(),
                        ex);
            }
        });

        LOGGER.debug(String.format("PGCR Count for %s is %d", member.getCombinedBungieGlobalDisplayName(),
                PGCR_COUNT.get()));
        return PGCR_COUNT.get();
    }

    public static void getSGCMemberCarnageReport(Member member, GenericActivity activityWithSGCMembers,
            HashMap<String, Member> sgcClanMembersMap) throws IOException, URISyntaxException {
        LOGGER.debug(String.format("Processing PGCR %s for %s", activityWithSGCMembers.getUID(),
                member.getCombinedBungieGlobalDisplayName()));
        URL url = new URI(String.format("https://stats.bungie.net/Platform/Destiny2/Stats/PostGameCarnageReport/%s/",
                activityWithSGCMembers.getUID())).toURL();

        HttpURLConnection conn = getBungieAPIResponse(url,
                String.format("getSGCMemberCarnageReport: %s", member.getCombinedBungieGlobalDisplayName()));

        if (conn != null) {
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

            conn.disconnect();
        }
    }

    public static String getClanActivityReportAsCsv(Clan clan) {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(getActivityReportCsvHeader());

        stringBuilder.append(getClanActivityCsvPart(clan));

        return stringBuilder.toString();
    }

    public static String getClanInternalActivityReportAsCsv(Clan clan) {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(getInternalActivityReportCsvHeader());

        stringBuilder.append(getClanInternalActivityCsvPart(clan));

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

                    HashMap<Mode, Integer> totalActivitiesWithSGCMembersByMode = member
                            .getTotalActivitiesWithSGCMembersByMode();
                    Map<Mode, Integer> potwModeCompletions = member.getPOTWModeCompletions();
                    Map<Raid, Integer> potwRaidCompletions = member.getPOTWRaidCompletions();
                    Map<Dungeon, Integer> potwDungeonCompletions = member.getPOTWDungeonCompletions();

                    if (member.hasNewBungieName()) {
                        // Base CPOTW
                        csvPart.append("\"").append(member.getDisplayName()).append("\",")
                                .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",")
                                .append("\"").append(clan.getCallsign()).append("\",")
                                .append("\"").append(member.getWeeklySGCActivity().get("SCORE")).append("\",")
                                .append("\"").append(titanClears).append("\",")
                                .append("\"").append(hunterClears).append("\",")
                                .append("\"").append(warlockClears).append("\",");
                        for (Mode mode : Mode.validModesForCPOTW()) {
                            csvPart.append("\"").append(totalActivitiesWithSGCMembersByMode.get(mode)).append("\",");
                        }

                        // POTW Modes
                        for (Mode mode : Mode.validModesForPOTW()) {
                            if (mode != Mode.RAID || mode != Mode.DUNGEON) {
                                csvPart.append("\"").append(potwModeCompletions.get(mode)).append("\",");
                            }
                        }
                        // POTW Raids
                        for (Raid raid : Raid.getRaidsOrdered()) {
                            csvPart.append("\"").append(potwRaidCompletions.get(raid)).append("\",");
                        }
                        // POTW Dungeons
                        for (Dungeon dungeon : Dungeon.getDungeonsOrdered()) {
                            csvPart.append("\"").append(potwDungeonCompletions.get(dungeon)).append("\",");
                        }
                        // New Line
                        csvPart.append("\n");
                    }
                });

        return csvPart.toString();
    }

    private static String getClanInternalActivityCsvPart(Clan clan) {
        final StringBuilder csvPart = new StringBuilder();
        List<Mode> validModesForCPOTW = Mode.validModesForCPOTW();
        clan.getMembers()
                .forEach((memberId, member) -> {
                    Character titanCharacter = member.getCharacterByDestinyClassType(DestinyClassType.TITAN);
                    Character hunterCharacter = member.getCharacterByDestinyClassType(DestinyClassType.HUNTER);
                    Character warlockCharacter = member.getCharacterByDestinyClassType(DestinyClassType.WARLOCK);

                    int titanClears = titanCharacter != null ? titanCharacter.getActivitiesWithSGCMembersCount() : 0;

                    int hunterClears = hunterCharacter != null ? hunterCharacter.getActivitiesWithSGCMembersCount() : 0;

                    int warlockClears = warlockCharacter != null ? warlockCharacter.getActivitiesWithSGCMembersCount()
                            : 0;

                    HashMap<Mode, Integer> totalActivitiesWithSGCMembersByMode = member
                            .getTotalActivitiesWithSGCMembersByMode();

                    if (member.hasNewBungieName()) {
                        csvPart.append("\"").append(member.getDisplayName()).append("\",")
                                .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",")
                                .append("\"").append(titanClears + hunterClears + warlockClears).append("\",");
                        for (Mode mode : validModesForCPOTW) {
                            csvPart.append("\"").append(totalActivitiesWithSGCMembersByMode.get(mode)).append("\",");
                        }
                        csvPart.append("\n");
                    }
                });

        return csvPart.toString();
    }

    public static HashMap<Platform, String> getPlatformActivityReportsAsCsv(List<Clan> clanList) {
        HashMap<Platform, StringBuilder> platformToReportBuilderMap = new HashMap<>();
        platformToReportBuilderMap.put(Platform.PC, new StringBuilder());
        platformToReportBuilderMap.put(Platform.XBOX, new StringBuilder());
        platformToReportBuilderMap.put(Platform.PSN, new StringBuilder());

        platformToReportBuilderMap.values().forEach((strBuilder) -> {
            strBuilder.append(getActivityReportCsvHeader());
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

    private static HashMap<Platform, String> getPlatformAnnualActivityReportsAsCsv(List<Clan> clanList,
            HashMap<Member, HashMap<String, Integer>> annualValues) {
        HashMap<Platform, StringBuilder> platformToReportBuilderMap = new HashMap<>();
        platformToReportBuilderMap.put(Platform.PC, new StringBuilder());
        platformToReportBuilderMap.put(Platform.XBOX, new StringBuilder());
        platformToReportBuilderMap.put(Platform.PSN, new StringBuilder());

        platformToReportBuilderMap.values().forEach((strBuilder) -> {
            strBuilder.append(getActivityReportCsvHeader());
        });

        clanList.forEach((clan) -> {
            platformToReportBuilderMap.get(clan.getClanPlatform())
                    .append(getClanAnnualActivityCsvPart(clan, annualValues));
        });
        HashMap<Platform, String> output = new HashMap<>();
        platformToReportBuilderMap.forEach((platform, reportBuilder) -> {
            output.put(platform, reportBuilder.toString());
        });
        return output;
    }

    private static Object getClanAnnualActivityCsvPart(Clan clan,
            HashMap<Member, HashMap<String, Integer>> annualValues) {
        final StringBuilder csvPart = new StringBuilder();
        clan.getMembers()
                .forEach((memberId, member) -> {
                    HashMap<String, Integer> values = annualValues.get(member);

                    if (member.hasNewBungieName()) {
                        // Base CPOTW
                        csvPart.append("\"").append(member.getDisplayName()).append("\",")
                                .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",")
                                .append("\"").append(clan.getCallsign()).append("\",")
                                .append("\"").append(member.getWeeklySGCActivity().get("SCORE")).append("\",")
                                .append("\"").append(values.get("Titan Clears")).append("\",")
                                .append("\"").append(values.get("Hunter Clears")).append("\",")
                                .append("\"").append(values.get("Warlock Clears")).append("\",");
                        for (Mode mode : Mode.validModesForCPOTW()) {
                            csvPart.append("\"").append(values.get(mode.getName())).append("\",");
                        }

                        // POTW Modes
                        for (Mode mode : Mode.validModesForPOTW()) {
                            if (mode != Mode.RAID || mode != Mode.DUNGEON) {
                                csvPart.append("\"").append(values.get(mode.getName() + " [POTW]")).append("\",");
                            }
                        }
                        // POTW Raids
                        for (Raid raid : Raid.getRaidsOrdered()) {
                            csvPart.append("\"").append(values.get(raid.getName() + " [POTW]")).append("\",");
                        }
                        // POTW Dungeons
                        for (Dungeon dungeon : Dungeon.getDungeonsOrdered()) {
                            csvPart.append("\"").append(values.get(dungeon.getName() + " [POTW]")).append("\",");
                        }
                        // New Line
                        csvPart.append("\n");
                    }
                });

        return csvPart.toString();
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
            throws IOException, InterruptedException, URISyntaxException {
        LOGGER.info("Starting User SGC Activity Report");

        List<Clan> clanList = initializeClanList();
        HashMap<String, Member> sgcClanMembersMap = initializeClanMembersMap(clanList);

        Member member = getMemberFromMap(userBungieId, sgcClanMembersMap);

        if (member != null) {
            try {
                LOGGER.debug("Starting to process " + member.getDisplayName());
                getMembersClearedActivities(member, startDate, endDate,
                        sgcClanMembersMap, 0, false);
                LOGGER.debug("Finished processing " + member.getDisplayName());
            } catch (IOException ex) {
                LOGGER.error("Error processing " + member.getDisplayName(), ex);
            }
        }

        return member;
    }

    private static Member getUserPOTWActivityReport(String userBungieId, LocalDate startDate,
            LocalDate endDate)
            throws IOException, InterruptedException, URISyntaxException {
        LOGGER.info("Starting User SGC Activity Report");

        List<Clan> clanList = initializeClanList();
        HashMap<String, Member> sgcClanMembersMap = initializeClanMembersMap(clanList);

        Member member = getMemberFromMap(userBungieId, sgcClanMembersMap);

        if (member != null) {
            try {
                LOGGER.debug("Starting to process " + member.getDisplayName());
                getMembersClearedActivities(member, startDate, endDate,
                        sgcClanMembersMap, 0, true);
                LOGGER.debug("Finished processing " + member.getDisplayName());
            } catch (IOException ex) {
                LOGGER.error("Error processing " + member.getDisplayName(), ex);
            }
        }

        return member;
    }

    public static int getUserPOTWScore(String userBungieId, LocalDate startDate,
            LocalDate endDate) throws IOException, InterruptedException, URISyntaxException {
        Member member = getUserPOTWActivityReport(userBungieId, startDate,
                endDate);
        if (member == null) {
            return -1;
        }
        Map<String, Integer> potwWeights = GoogleDriveUtil.getPOTWWeights();

        Map<Mode, Integer> potwModeCompletions = member.getPOTWModeCompletions();
        Map<Raid, Integer> potwRaidCompletions = member.getPOTWRaidCompletions();
        Map<Dungeon, Integer> potwDungeonCompletions = member.getPOTWDungeonCompletions();

        int total = 0;
        for (Mode mode : potwModeCompletions.keySet()) {
            try {
                total += potwWeights.get(mode.name) * potwModeCompletions.get(mode);
            } catch (Exception ex) {
                LOGGER.error("Error processing the following mode: " + mode.name, ex);
            }
        }
        for (Raid raid : potwRaidCompletions.keySet()) {
            try {
                total += potwWeights.get(raid.name) * potwRaidCompletions.get(raid);
            } catch (Exception ex) {
                LOGGER.error("Error processing the following mode: " + raid.name, ex);
            }
        }
        for (Dungeon dungeon : potwDungeonCompletions.keySet()) {
            total += potwWeights.get(dungeon.name) * potwDungeonCompletions.get(dungeon);
        }
        return total;
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

    public static boolean isValidDateFormat(String dateInput) {
        return dateInput.matches("[0-9]{4}[0-9]{2}[0-9]{2}");
    }

    private static String getActivityReportCsvHeader() {
        final StringBuilder csvPart = new StringBuilder();

        // Base CPOTW
        csvPart.append("\"Gamertag\",").append("\"BungieDisplayName\",").append("\"Clan\",")
                .append("\"Community POTW Points\",").append("\"Titan Clears\",").append("\"Hunter Clears\",")
                .append("\"Warlock Clears\",");

        List<Mode> validModesForCPOTW = Mode.validModesForCPOTW();
        for (Mode mode : validModesForCPOTW) {
            csvPart.append("\"").append(mode.getName()).append("\",");
        }

        // POTW Modes
        for (Mode mode : Mode.validModesForPOTW()) {
            if (mode != Mode.RAID || mode != Mode.DUNGEON) {
                csvPart.append("\"").append(mode.getName() + " [POTW]").append("\",");
            }
        }
        // POTW Raids
        for (Raid raid : Raid.getRaidsOrdered()) {
            csvPart.append("\"").append(raid.getName() + " [POTW]").append("\",");
        }
        // POTW Dungeons
        for (Dungeon dungeon : Dungeon.getDungeonsOrdered()) {
            csvPart.append("\"").append(dungeon.getName() + " [POTW]").append("\",");
        }

        // New Line
        return csvPart.append("\n").toString();
    }

    private static String getInternalActivityReportCsvHeader() {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",")
                .append("\"Total Activities with Clan Members\",");

        List<Mode> validModesForCPOTW = Mode.validModesForCPOTW();
        for (Mode mode : validModesForCPOTW) {
            stringBuilder.append("\"").append(mode.getName()).append("\",");
        }

        return stringBuilder.append("\n").toString();
    }

    public static HashMap<SGC_Clan, HashMap<String, Instant>> getAllMembersLastDatePlayed() {
        HashMap<SGC_Clan, HashMap<String, Instant>> members = new HashMap<>();
        try {
            for (SGC_Clan sgc_clan : SGC_Clan.values()) {
                LOGGER.trace("Processing " + sgc_clan.name());
                members.put(sgc_clan, new HashMap<>());

                Clan clan = getClanInformation(sgc_clan.Bungie_ID);

                List<Callable<Object>> tasks = new ArrayList<>();
                clan.getMembers().forEach((memberId, member) -> {
                    try {
                        tasks.add(() -> {

                            LOGGER.trace("Processing " + member.getCombinedBungieGlobalDisplayName());
                            getMembersActiveCharacters(member);
                            List<Instant> lastPlayedDates = new ArrayList<>();
                            for (Character character : member.getCharacters().values()) {
                                lastPlayedDates.add(ZonedDateTime.parse(character.getDateLastPlayed())
                                        .withZoneSameInstant(ZoneId.of("US/Eastern")).toInstant());
                            }
                            members.get(sgc_clan).put(member.getDisplayName(), Collections.max(lastPlayedDates));
                            return member;
                        });
                    } catch (Exception ex) {
                        LOGGER.error("Error processing Clan Last Played Report for " + clan.getName(), ex);
                    }
                });
                executorService.invokeAll(tasks);
                LOGGER.trace("Finished Processing " + sgc_clan.name());
            }
        } catch (Exception ex) {
            LOGGER.error("Error processing getAllMembersLastDatePlayed", ex);
        }
        return members;
    }

    public static HashMap<String, Boolean> getMembersCollections(Member member, List<String> collectibleHashIntegers)
            throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://www.bungie.net/Platform/Destiny2/%s/Profile/%s/?components=800",
                member.getMemberType(), member.getUID())).toURL();

        HashMap<String, Boolean> response = new HashMap<>();

        HttpURLConnection conn = getBungieAPIResponse(url,
                String.format("getMembersCollections: %s", member.getCombinedBungieGlobalDisplayName()));

        if (conn != null) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            JsonObject results = JsonParser.parseString(content.toString()).getAsJsonObject()
                    .getAsJsonObject("Response").getAsJsonObject("profileCollectibles").getAsJsonObject("data")
                    .getAsJsonObject("collectibles");
            collectibleHashIntegers.forEach((hash) -> {
                try {
                    JsonObject collectible = results.getAsJsonObject(hash);
                    int state = collectible.get("state").getAsInt();
                    if (state % 2 == 0) {
                        response.put(hash, true);
                    } else {
                        response.put(hash, false);
                    }
                } catch (Exception ex) {
                    LOGGER.error("Error processing JSON result from "
                            + url.toExternalForm(), ex);
                    response.put(hash, null);
                }
            });
            in.close();

            conn.disconnect();
        }
        return response;
    }

    public static HashMap<String, Boolean> getMembersTriumphs(Member member, List<String> triumphHashIntegers)
            throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://www.bungie.net/Platform/Destiny2/%s/Profile/%s/?components=900",
                member.getMemberType(), member.getUID())).toURL();

        HashMap<String, Boolean> response = new HashMap<>();
        HttpURLConnection conn = getBungieAPIResponse(url,
                String.format("Get %s Triumphs", member.getCombinedBungieGlobalDisplayName()));

        if (conn != null) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            JsonObject results = JsonParser.parseString(content.toString()).getAsJsonObject()
                    .getAsJsonObject("Response").getAsJsonObject("profileRecords").getAsJsonObject("data")
                    .getAsJsonObject("records");
            triumphHashIntegers.forEach((hash) -> {
                try {
                    JsonObject triumph = results.getAsJsonObject(hash).get("objectives").getAsJsonArray().get(0)
                            .getAsJsonObject();
                    response.put(hash, triumph.get("complete").getAsBoolean());
                } catch (Exception ex) {
                    LOGGER.error("Error processing JSON result from "
                            + url.toExternalForm(), ex);
                    response.put(hash, null);
                }
            });
            in.close();
            conn.disconnect();
        }
        return response;
    }

    public static HashMap<String, Integer> getMembersMetrics(Member member, List<String> metricsHashIntegers)
            throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://www.bungie.net/Platform/Destiny2/%s/Profile/%s/?components=1100",
                member.getMemberType(), member.getUID())).toURL();
        // 1765255052
        HashMap<String, Integer> response = new HashMap<>();

        HttpURLConnection conn = getBungieAPIResponse(url,
                String.format("getMembersMetrics: %s", member.getCombinedBungieGlobalDisplayName()));

        if (conn != null) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                JsonObject results = JsonParser.parseString(content.toString()).getAsJsonObject()
                        .getAsJsonObject("Response").getAsJsonObject("metrics").getAsJsonObject("data")
                        .getAsJsonObject("metrics");
                metricsHashIntegers.forEach((hash) -> {
                    try {
                        JsonObject metric = results.getAsJsonObject(hash).getAsJsonObject("objectiveProgress");
                        response.put(hash, metric.get("progress").getAsInt());
                    } catch (Exception ex) {
                        LOGGER.error("Error processing JSON result from "
                                + url.toExternalForm(), ex);
                        response.put(hash, null);
                    }
                });
                in.close();
            } catch (Exception ex) {
                LOGGER.error("Error processing JSON result from "
                        + url.toExternalForm(), ex);
                metricsHashIntegers.forEach(hash -> {
                    response.put(hash, null);
                });
            }

            conn.disconnect();
        }
        return response;
    }

    public static Clan getClanDungeonReport(Clan clan,
            InteractionOriginalResponseUpdater interactionOriginalResponseUpdater)
            throws Exception {
        LOGGER.trace("Processing " + clan.getName());
        final AtomicInteger count = new AtomicInteger(0);

        List<Callable<Object>> tasks = new ArrayList<>();
        clan.getMembers().forEach((memberId, member) -> {
            tasks.add(() -> {

                try {
                    getClanMemberDungeonReport(member);
                    if (interactionOriginalResponseUpdater != null)
                        interactionOriginalResponseUpdater
                                .setContent(String.format("Building a clan dungeon report for %s (%d/%d)",
                                        clan.getName(), count.incrementAndGet(), clan.getMembers().size()))
                                .update().join();
                } catch (Exception ex) {
                    LOGGER.error("Error processing Clan Dungeon Report for " + clan.getName(), ex);
                }
                return member;
            });
        });
        executorService.invokeAll(tasks);
        LOGGER.trace("Finished Processing " + clan.getName());
        return clan;
    }

    public static String getClanDungeonReportAsCsv(Clan clan) throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",");
        Dungeon.getDungeonsOrdered().forEach((Dungeon dungeon) -> {
            stringBuilder.append(dungeon.name).append(",");
        });

        stringBuilder.append("\n");

        clan.getMembers().forEach((id, member) -> {
            HashMap<Dungeon, Integer> dungeonClears = member.getDungeonClears();
            stringBuilder.append("\"").append(member.getDisplayName()).append("\",")
                    .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",");
            Dungeon.getDungeonsOrdered().forEach((Dungeon raid) -> {
                stringBuilder.append("\"").append(dungeonClears.get(raid)).append("\",");
            });
            stringBuilder.append("\n");

        });
        return stringBuilder.toString();
    }

    private static void getClanMemberDungeonReport(Member member) {
        LOGGER.trace("Processing " + member.getDisplayName());
        try {
            getAllMembersCharacters(member);
            getMemberDungeonInfo(member);
        } catch (Exception e) {
            LOGGER.error("Error processing Clan Member Dungeon Report for " + member.getDisplayName(), e);
        }
        LOGGER.trace("Finished Processing " + member.getDisplayName());
    }

    private static void getClanMemberWeeklyDungeonReport(Member member) {
        LOGGER.trace("Processing " + member.getDisplayName());
        try {
            getAllMembersCharacters(member);
            getMemberDungeonInfo(member);
            getUserWeeklyClears(member, LocalDate.now().minusDays(6), LocalDate.now());
        } catch (Exception e) {
            LOGGER.error("Error processing Clan Member Dungeon Report for " + member.getDisplayName(), e);
        }
        LOGGER.trace("Finished Processing " + member.getDisplayName());
    }

    public static void getMemberDungeonInfo(Member member) throws IOException {
        List<String> validDungeonHashes = Dungeon.getAllValidDungeonHashes();
        member.getCharacters().forEach((characterId, character) -> {
            try {
                URL url = new URI(String.format(
                        "https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Character/%s/Stats/AggregateActivityStats",
                        member.getMemberType(), member.getUID(), character.getUID())).toURL();

                HttpURLConnection conn = getBungieAPIResponse(url,
                        String.format("getMemberDungeonInfo: %s", member.getCombinedBungieGlobalDisplayName()));

                if (conn != null) {
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
                    List<JsonObject> dungeons = IntStream.range(0, results.size())
                            .mapToObj(index -> (JsonObject) results.get(index)).filter((result) -> {
                                // String asString = result.get("activityHash").getAsString();
                                return validDungeonHashes.contains(result.get("activityHash").getAsString());
                            }).collect(Collectors.toList());
                    dungeons.forEach((entry) -> {
                        Dungeon dungeon = Dungeon.getDungeon(entry.get("activityHash").getAsString());
                        if (dungeon != null) {
                            String clears = entry.get("values").getAsJsonObject().get("activityCompletions")
                                    .getAsJsonObject().get("basic").getAsJsonObject().get("value").getAsString();
                            character.getDungeonActivities().get(dungeon).addClears(Double.parseDouble(clears));
                        }
                    });

                    in.close();
                    conn.disconnect();
                }
            } catch (Exception ex) {
                LOGGER.error("Error processing JSON result for characterID: "
                        + characterId, ex);
            }
        });
    }

    public static HashMap<String, HashMap<Mode, Boolean>> getActivitytoCodeWalk(String bungieId, LocalDate startDate,
            LocalDate endDate) throws Exception {
        HashMap<String, String> hashToNameMap = new HashMap<>();
        Member member = getMemberInformationWithCharacters(bungieId, true);
        HashMap<String, HashMap<Mode, Boolean>> output = new HashMap<>();
        member.getCharacters().forEach((characteruid, character) -> {
            try {
                boolean next = false;
                for (int page = 0; !next; page++) {
                    URL url = new URI(String.format(
                            "https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Character/%s/Stats/Activities/?page=%d&mode=%d&count=250",
                            member.getMemberType(), member.getUID(), character.getUID(), page, 0))
                            .toURL();

                    HttpURLConnection conn = getBungieAPIResponse(url,
                            String.format("getActivitytoCodeWalk: %s <%d>", bungieId, page + 1));

                    if (conn != null) {
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

                            List<Callable<Object>> tasks = new ArrayList<>();
                            results.forEach((result) -> {
                                tasks.add(() -> {

                                    int activityMode = result.getAsJsonObject().getAsJsonObject("activityDetails")
                                            .getAsJsonPrimitive("mode").getAsInt();

                                    if (Mode.validModeValuesForCPOTW().contains(activityMode)) {
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
                                                try {
                                                    String directorActivityHash = result.getAsJsonObject()
                                                            .getAsJsonObject("activityDetails")
                                                            .getAsJsonPrimitive("directorActivityHash")
                                                            .getAsString();
                                                    if (!hashToNameMap.containsKey(directorActivityHash)) {
                                                        hashToNameMap.put(directorActivityHash,
                                                                getActivityName(directorActivityHash));
                                                    }
                                                    output.putIfAbsent(hashToNameMap.get(directorActivityHash),
                                                            createEmptyCodeMap());
                                                    output.get(hashToNameMap.get(directorActivityHash))
                                                            .put(Mode.getFromValue(activityMode), true);
                                                } catch (Throwable ex) {
                                                    LOGGER.error(ex.getMessage(), ex);
                                                }
                                            }
                                        } else if (dateCompleted.isAfter(endDate)) {
                                            recordsAfterEndDate.incrementAndGet();
                                        }
                                    }
                                    return null;
                                });
                            });

                            executorService.invokeAll(tasks);
                            LOGGER.info(String.format(
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

            } catch (Exception ex) {
                LOGGER.error(
                        "Error Processing Cleared Activities for "
                                + member.getCombinedBungieGlobalDisplayName(),
                        ex);
            }
        });
        return output;
    }

    private static HashMap<Mode, Boolean> createEmptyCodeMap() {
        HashMap<Mode, Boolean> map = new HashMap<>();
        Mode.validModesForPOTW().forEach(mode -> map.put(mode, false));
        return map;
    }

    private static String getActivityName(String directorActivityHash) {
        String activityName = null;
        try {
            URL url = new URI(
                    String.format("https://www.bungie.net/Platform/Destiny2/Manifest/DestinyActivityDefinition/%s/",
                            directorActivityHash))
                    .toURL();
            HttpURLConnection conn = getBungieAPIResponse(url,
                    String.format("getActivityName: %s", directorActivityHash));

            if (conn != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                activityName = JsonParser.parseString(content.toString()).getAsJsonObject()
                        .getAsJsonObject("Response").getAsJsonObject("originalDisplayProperties")
                        .getAsJsonPrimitive("name").getAsString();
                in.close();

                conn.disconnect();
            }
        } catch (Exception ex) {
            LOGGER.error(
                    "Error Processing Activity Name for for directorActivityHash: "
                            + directorActivityHash,
                    ex);
        }
        return activityName;
    }

    public static String getMemberMissingRedeemableCollectables(String bungieId) throws Exception {
        Member member = getMemberInformationWithCharacters(bungieId, true);
        HashMap<String, Boolean> membersCollections = getMembersCollections(member,
                RedeemableCollectable.getAllCollectableHashes());
        ArrayList<String> missingCollectableHashes = new ArrayList<>();
        membersCollections.forEach((hash, flag) -> {
            if (!flag) {
                missingCollectableHashes.add(hash);
            }
        });
        return getRedeemableCollectableList(missingCollectableHashes);
    }

    public static String getRedeemableCollectableList(List<String> collectableHashes) {
        StringBuilder sb = new StringBuilder();
        collectableHashes.forEach(hash -> {
            RedeemableCollectable redeemableCollectable = RedeemableCollectable.getRedeemableCollectable(hash);
            String collectibleName = getCollectibleName(redeemableCollectable.getCollectableHash());
            sb.append(collectibleName).append(" | ").append(RedeemableCollectable.BungieRedeemURL)
                    .append(redeemableCollectable.getCode()).append("\n");
        });
        return sb.toString();
    }

    public static String getNonCollectableRedeemables() {
        StringBuilder sb = new StringBuilder();
        List<String> allNonCollectableHashes = RedeemableCollectable.getAllNonCollectableHashes();
        allNonCollectableHashes.forEach(hash -> {
            RedeemableCollectable redeemableCollectable = RedeemableCollectable.getRedeemableCollectable(hash);
            String collectibleName = getItemName(redeemableCollectable.getCollectableHash());
            sb.append(collectibleName).append(" | ").append(RedeemableCollectable.BungieRedeemURL)
                    .append(redeemableCollectable.getCode()).append("\n");
        });
        return sb.toString();
    }

    private static String getCollectibleName(String collectibleHash) {
        String collectableName = null;
        try {
            URL url = new URI(
                    String.format("https://www.bungie.net/Platform/Destiny2/Manifest/DestinyCollectibleDefinition/%s/",
                            collectibleHash))
                    .toURL();
            HttpURLConnection conn = getBungieAPIResponse(url,
                    String.format("getCollectibleName: %s", collectibleHash));

            if (conn != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                collectableName = JsonParser.parseString(content.toString()).getAsJsonObject()
                        .getAsJsonObject("Response").getAsJsonObject("displayProperties")
                        .getAsJsonPrimitive("name").getAsString();
                in.close();

                conn.disconnect();
            }
        } catch (Exception ex) {
            LOGGER.error(
                    "Error Processing Activity Name for for directorActivityHash: "
                            + collectibleHash,
                    ex);
        }
        return collectableName;
    }

    private static String getItemName(String itemHash) {
        String collectableName = null;
        try {
            URL url = new URI(
                    String.format(
                            "https://www.bungie.net/Platform/Destiny2/Manifest/DestinyInventoryItemDefinition/%s/",
                            itemHash))
                    .toURL();
            HttpURLConnection conn = getBungieAPIResponse(url, String.format("getItemName: %s", itemHash));

            if (conn != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                collectableName = JsonParser.parseString(content.toString()).getAsJsonObject()
                        .getAsJsonObject("Response").getAsJsonObject("displayProperties")
                        .getAsJsonPrimitive("name").getAsString();
                in.close();

                conn.disconnect();
            }
        } catch (Exception ex) {
            LOGGER.error(
                    "Error Processing Activity Name for for directorActivityHash: "
                            + itemHash,
                    ex);
        }
        return collectableName;
    }

    @SuppressWarnings("unchecked")
    public static HashMap<Mode, HashMap<String, ArrayList>> getLatestModeToActivityMap()
            throws IOException, URISyntaxException {
        String activityManifestLocation = getLatestActivityManifestLocation();
        String activityModeManifestLocation = getLatestActivityModeManifestLocation();
        JsonObject rawActivityManifest = fetchManifestFromBungie(activityManifestLocation);
        JsonObject rawActivityModeManifest = fetchManifestFromBungie(activityModeManifestLocation);
        Set<String> activitySet = rawActivityManifest.keySet();
        HashMap<Mode, HashMap<String, ArrayList>> modeToActivities = new HashMap<>();

        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger badCount = new AtomicInteger(0);
        activitySet.forEach(activityHash -> {
            try {
                String activityName = rawActivityManifest.getAsJsonObject(activityHash)
                        .getAsJsonObject("displayProperties").get("name").getAsString();

                String activityTypeHash = "";
                try {
                    activityTypeHash = rawActivityManifest.getAsJsonObject(activityHash)
                            .get("directActivityModeHash").getAsString();
                } catch (Exception e) {
                    // TODO: handle exception
                }
                if (activityTypeHash.isEmpty()) {
                    try {
                        activityTypeHash = rawActivityManifest.getAsJsonObject(activityHash)
                                .get("activityTypeHash").getAsString();
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
                // displayProperties -> name
                // activityTypeHash
                if (!activityName.isEmpty() && !activityHash.isEmpty()) {
                    String modeType = rawActivityModeManifest.getAsJsonObject(activityTypeHash).get("modeType")
                            .getAsString();
                    Mode mode = Mode.getFromValue(Integer.parseInt(modeType));
                    if (Mode.validModesForCPOTW().contains(mode)) {
                        modeToActivities.putIfAbsent(mode, new HashMap());
                        modeToActivities.get(mode).putIfAbsent(
                                activityName, new ArrayList<>());
                        modeToActivities.get(mode).get(activityName)
                                .add(activityHash);
                        count.addAndGet(1);
                    }

                }
            } catch (Exception ex) {
                badCount.addAndGet(1);
                System.out.println(String.format("%s | %s", activityHash,
                        rawActivityManifest.getAsJsonObject(activityHash)
                                .getAsJsonObject("displayProperties").get("name").getAsString()));
            }
        });

        return modeToActivities;
    }

    private static JsonObject fetchManifestFromBungie(String manifestLocation)
            throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://www.bungie.net%s", manifestLocation)).toURL();
        JsonObject json = null;

        HttpURLConnection conn = getBungieAPIResponse(url,
                String.format("fetchManifestFromBungie: %s", manifestLocation));

        if (conn != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                json = JsonParser.parseString(content.toString()).getAsJsonObject();
                in.close();
            }

            conn.disconnect();
        }
        return json;
    }

    private static String getLatestActivityManifestLocation() throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://www.bungie.net/Platform/Destiny2/Manifest/")).toURL();

        HttpURLConnection conn = getBungieAPIResponse(url, "getLatestActivityManifestLocation");
        String manifestLocation = "";

        if (conn != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                manifestLocation = JsonParser.parseString(content.toString()).getAsJsonObject()
                        .getAsJsonObject("Response")
                        .getAsJsonObject("jsonWorldComponentContentPaths")
                        .getAsJsonObject("en")
                        .get("DestinyActivityDefinition")
                        .getAsString();
                in.close();
            }

            conn.disconnect();
        }
        return manifestLocation;
    }

    private static String getLatestActivityModeManifestLocation() throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://www.bungie.net/Platform/Destiny2/Manifest/")).toURL();

        HttpURLConnection conn = getBungieAPIResponse(url, "getLatestActivityModeManifestLocation");
        String manifestLocation = "";

        if (conn != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                manifestLocation = JsonParser.parseString(content.toString()).getAsJsonObject()
                        .getAsJsonObject("Response")
                        .getAsJsonObject("jsonWorldComponentContentPaths")
                        .getAsJsonObject("en")
                        .get("DestinyActivityModeDefinition")
                        .getAsString();
                in.close();
            }

            conn.disconnect();
        }
        return manifestLocation;
    }

    public static String getLatestGlobalVendorInventory() throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://www.bungie.net/Platform/Destiny2/Vendors/")).toURL();

        HttpURLConnection conn = getBungieAPIResponse(url, "getLatestGlobalVendorInventory");
        String manifestLocation = "";

        if (conn != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                manifestLocation = JsonParser.parseString(content.toString()).getAsJsonObject()
                        .getAsJsonObject("Response")
                        .getAsJsonObject("jsonWorldComponentContentPaths")
                        .getAsJsonObject("en")
                        .get("DestinyActivityModeDefinition")
                        .getAsString();
                in.close();
            }

            conn.disconnect();
        }
        return manifestLocation;
    }

    public static String getFireteamBalance(String bungieId) throws Exception {
        Member member = getMemberInformation(bungieId);
        final StringBuilder response = new StringBuilder();
        HashMap<Member, Double> fireteamMmrMap = new HashMap<>();
        LinkedHashMap<Member, Double> sortedFireteamMmrMap = new LinkedHashMap<>();
        HashMap<Member, Integer> fireteamBalanceMap = new HashMap<>();
        ArrayList<Double> mmrs = new ArrayList<>();
        ArrayList<Member> fireteamMembers = null;
        ArrayList<Member> privateFireteamMembers = new ArrayList<>();
        if (member != null) {
            fireteamMembers = getFireteamMembers(member);
            fireteamMembers.forEach(fireteamMember -> {
                try {
                    Double memberMmr = calculateMemberMmr(fireteamMember);
                    fireteamMmrMap.put(fireteamMember,
                            memberMmr);
                    if (memberMmr != null) {
                        mmrs.add(memberMmr);
                    } else {
                        privateFireteamMembers.add(fireteamMember);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }
        mmrs.sort((mmr1, mmr2) -> (mmr2).compareTo(mmr1));
        for (int i = 0; i < mmrs.size(); i++) {
            Member found = null;
            for (Entry<Member, Double> entry : fireteamMmrMap.entrySet()) {
                if (mmrs.get(i).equals(entry.getValue()) && found == null) {
                    sortedFireteamMmrMap.put(entry.getKey(), entry.getValue());
                    found = entry.getKey();
                }
            }

            fireteamMmrMap.remove(found);
        }
        int team = 1;
        for (Entry<Member, Double> entry : sortedFireteamMmrMap.entrySet()) {
            fireteamBalanceMap.put(entry.getKey(), team);
            if (team == 1) {
                team = 2;
            } else {
                team = 1;
            }
        }

        if (sortedFireteamMmrMap.size() % 2 == 1) {
            fireteamBalanceMap.put(sortedFireteamMmrMap.lastEntry().getKey(), 2);
        }

        StringBuilder teamOne = new StringBuilder();
        teamOne.append("\nTeam 1\n-----\n");
        StringBuilder teamTwo = new StringBuilder();
        teamTwo.append("\nTeam 2\n-----\n");
        response.append("Fireteam Sorted by MMR\n---------\n");

        for (Entry<Member, Double> entry : sortedFireteamMmrMap.entrySet()) {
            response.append(String.format("%s#%s | %.2f\n", entry.getKey().getBungieGlobalDisplayName(),
                    entry.getKey().getBungieGlobalDisplayNameCode(), sortedFireteamMmrMap.get(entry.getKey())));

            if (fireteamBalanceMap.get(entry.getKey()) == 1) {
                teamOne.append(String.format("%s#%s\n", entry.getKey().getBungieGlobalDisplayName(),
                        entry.getKey().getBungieGlobalDisplayNameCode()));
            } else {
                teamTwo.append(String.format("%s#%s\n", entry.getKey().getBungieGlobalDisplayName(),
                        entry.getKey().getBungieGlobalDisplayNameCode()));
            }
        }

        StringBuilder privateMembers = new StringBuilder();
        privateMembers.append("\nFireteam Members with Privacy Settings\n-----\n");
        for (Member pMember : privateFireteamMembers) {
            privateMembers.append(String.format("%s#%s\n", pMember.getBungieGlobalDisplayName(),
                    pMember.getBungieGlobalDisplayNameCode()));
        }
        response.append(teamOne).append(teamTwo);
        if (privateFireteamMembers.size() > 0) {
            response.append(privateMembers);
        }
        return response.toString();
    }

    private static ArrayList<Member> getFireteamMembers(Member member) throws URISyntaxException, IOException {
        ArrayList<Member> fireteamMembers = new ArrayList<>();

        URL url = new URI(String.format("https://www.bungie.net/Platform/Destiny2/%s/Profile/%s/?components=1000",
                member.getMemberType(), member.getUID())).toURL();

        HttpURLConnection conn = getBungieAPIResponse(url,
                String.format("getFireteamMembers: %s", member.getCombinedBungieGlobalDisplayName()));

        if (conn != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                JsonArray results = JsonParser.parseString(content.toString()).getAsJsonObject()
                        .getAsJsonObject("Response")
                        .getAsJsonObject("profileTransitoryData")
                        .getAsJsonObject("data")
                        .getAsJsonArray("partyMembers");

                results.forEach((entry) -> {

                    try {
                        String membershipId = entry.getAsJsonObject().get("membershipId").getAsString();
                        Member fireteamMember = getMemberByDestinyMembershipId(membershipId);
                        if (fireteamMember != null) {
                            fireteamMembers.add(fireteamMember);
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Error processing JSON result from " + url.toExternalForm(), ex);
                    }

                });
                in.close();
            } catch (Exception ex) {
                LOGGER.error("Error processing JSON result from " + url.toExternalForm(), ex);
            }

            conn.disconnect();
        }
        return fireteamMembers;
    }

    private static Member getMemberByDestinyMembershipId(String destinyMembershipId)
            throws URISyntaxException, IOException {
        Member member = null;
        URL url = new URI(String.format("https://www.bungie.net/Platform/User/GetMembershipsById/%s/0/",
                destinyMembershipId)).toURL();

        HttpURLConnection conn = getBungieAPIResponse(url,
                String.format("getMemberByDestinyMembershipId: %s", destinyMembershipId));

        if (conn != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                JsonArray results = JsonParser.parseString(content.toString()).getAsJsonObject()
                        .getAsJsonObject("Response")
                        .getAsJsonArray("destinyMemberships");
                JsonElement entry = results.get(0);
                try {
                    String bungieGlobalDisplayName = entry.getAsJsonObject().get("bungieGlobalDisplayName")
                            .getAsString();
                    String bungieGlobalDisplayNameCode = entry.getAsJsonObject().get("bungieGlobalDisplayNameCode")
                            .getAsString();
                    member = getMemberInformation(bungieGlobalDisplayName + "#" + bungieGlobalDisplayNameCode);

                } catch (Exception ex) {
                    LOGGER.error("Error processing JSON result from " + url.toExternalForm(), ex);
                }
                in.close();
            }

            conn.disconnect();
        }
        return member;
    }

    private static Double calculateMemberMmr(Member member) throws IOException, URISyntaxException {
        Double memberSeasonalCrucibleKDA = getMemberSeasonalCrucibleKDA(member);
        Double memberCareerCrucibleWinLossRatio = null;
        Double memberCareerCrucibleKDA = null;

        URL url = new URI(
                String.format("https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Stats/?groups=1",
                        member.getMemberType(), member.getUID()))
                .toURL();
        HttpURLConnection conn = getBungieAPIResponse(url,
                String.format("calculateMemberMmr: %s", member.getCombinedBungieGlobalDisplayName()));

        if (conn != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                JsonObject results = JsonParser.parseString(content.toString()).getAsJsonObject()
                        .getAsJsonObject("Response")
                        .getAsJsonObject("mergedAllCharacters")
                        .getAsJsonObject("results")
                        .getAsJsonObject("allPvP")
                        .getAsJsonObject("allTime");

                memberCareerCrucibleWinLossRatio = results.getAsJsonObject().getAsJsonObject("winLossRatio")
                        .getAsJsonObject("basic").get("value").getAsDouble();
                memberCareerCrucibleKDA = results.getAsJsonObject().getAsJsonObject("killsDeathsAssists")
                        .getAsJsonObject("basic").get("value").getAsDouble();

                in.close();
            }

            conn.disconnect();
        }
        if (memberSeasonalCrucibleKDA != null && memberCareerCrucibleWinLossRatio != null
                && memberCareerCrucibleKDA != null)
            return ((memberSeasonalCrucibleKDA + memberCareerCrucibleKDA) / 2.0) * memberCareerCrucibleWinLossRatio;
        else
            return null;
    }

    private static Double getMemberSeasonalCrucibleKDA(Member member) throws IOException, URISyntaxException {
        HashMap<String, Integer> membersMetrics = getMembersMetrics(member, Arrays.asList("871184140"));
        if (membersMetrics.get("871184140") == null) {
            return null;
        }
        return membersMetrics.get("871184140") / 100.0;
    }

    public static Pair<String, String> getRandomPrivateCrucibleOptions() {
        Map<String, Integer> crucubleMapWeights = GoogleDriveUtil.getCrucubleMapWeights();
        Map<String, Integer> crucubleModeWeights = GoogleDriveUtil.getCrucubleModeWeights();
        try {
            String randomMap = (String) getRandomSelectionWithWeights(crucubleMapWeights);
            String randomMode = (String) getRandomSelectionWithWeights(crucubleModeWeights);
            return Pair.of(randomMap, randomMode);
        } catch (Exception ex) {
            LOGGER.error("Error generating Random Private Crucible Options", ex);
            return null;
        }
    }

    private static Object getRandomSelectionWithWeights(Map<? extends Object, Integer> items) {
        int completeWeight = 0;
        for (Integer weight : items.values())
            completeWeight += weight;
        int r = (int) Math.round(Math.random() * completeWeight);
        int countWeight = 0;
        for (Object item : items.keySet()) {
            countWeight += items.get(item);
            if (countWeight >= r)
                return item;
        }
        throw new RuntimeException("Should never be shown. I never got a random pick.");
    }

    public static Pair<String, String> getRandomPrivateGambitOptions() {
        Map<String, Integer> gambitMapWeights = GoogleDriveUtil.getGambitMapWeights();
        try {
            String randomMap = (String) getRandomSelectionWithWeights(gambitMapWeights);

            Map<String, Integer> gambitCombatantWeights = GoogleDriveUtil.getGambitMapCombatantsWithWeights(randomMap);
            String randomCombatant = (String) getRandomSelectionWithWeights(gambitCombatantWeights);
            return Pair.of(randomMap, randomCombatant);
        } catch (Exception ex) {
            LOGGER.error("Error generating Random Private Gambit Options", ex);
            return null;
        }
    }

    private static HttpURLConnection getBungieAPIResponse(URL url, String connectionReason)
            throws IOException, URISyntaxException {
        HttpURLConnection conn;
        int attemptNumber = 1;
        while (attemptNumber <= MAX_RETRIES) {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.addRequestProperty("X-API-Key", apiKey);
            conn.addRequestProperty("Accept", "Application/Json");
            conn.connect();

            // Getting the response code
            int responsecode = conn.getResponseCode();
            if (responsecode == 200) {
                return conn;
            } else {
                LOGGER.error(String.format("%s | Response Code: %d | Attempt %d of %d",
                        connectionReason, responsecode, attemptNumber++, MAX_RETRIES));
                try {
                    Thread.sleep(TIME_DELAY);
                } catch (InterruptedException e) {
                    LOGGER.error("Thread Interrupted", e);
                }
            }
        }
        return null;
    }

    private static HttpURLConnection getBungieAPIResponseWithJsonInput(URL url, String connectionReason,
            String jsonInputString)
            throws IOException, URISyntaxException {
        HttpURLConnection conn;
        int attemptNumber = 1;
        while (attemptNumber <= MAX_RETRIES) {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.addRequestProperty("X-API-Key", apiKey);
            conn.addRequestProperty("Content-Type", "Application/Json");
            conn.addRequestProperty("Accept", "Application/Json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Getting the response code
            int responsecode = conn.getResponseCode();
            if (responsecode == 200) {
                return conn;
            } else {
                LOGGER.error(String.format("%s | Response Code: %d | Attempt %d of %d",
                        connectionReason, responsecode, attemptNumber++, MAX_RETRIES));
                try {
                    Thread.sleep(TIME_DELAY);
                } catch (InterruptedException e) {
                    LOGGER.error("Thread Interrupted", e);
                }
            }
        }
        return null;
    }
}