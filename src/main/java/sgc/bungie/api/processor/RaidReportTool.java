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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

import sgc.Platform;
import sgc.SGC_Clan;;

/**
 * @author chris hoppe
 */

public class RaidReportTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaidReportTool.class);

    private static String apiKey = System.getenv("BUNGIE_TOKEN");

    private static final HashMap<String, String> pcClanIdMap = new HashMap<>();
    private static final HashMap<String, String> xbClanIdMap = new HashMap<>();
    private static final HashMap<String, String> psClanIdMap = new HashMap<>();

    private static ExecutorService executorService = Executors.newFixedThreadPool(15);

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

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.addRequestProperty("X-API-Key", apiKey);
        conn.addRequestProperty("Accept", "Application/Json");
        conn.connect();

        // Getting the response code
        int responseCode = conn.getResponseCode();

        if (responseCode == 200) {
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
            }
        }
        conn.disconnect();
    }

    /**
     * Retrieves the members of the clan.
     *
     * @param clan the clan for which to retrieve the members
     * @throws IOException        if an I/O error occurs while making the request
     * @throws URISyntaxException if the URI syntax is invalid
     */
    public static void getClanMembers(Clan clan) throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://www.bungie.net/Platform/GroupV2/%s/Members/", clan.getClanId()))
                .toURL();

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.addRequestProperty("X-API-Key", apiKey);
        conn.addRequestProperty("Accept", "Application/Json");
        conn.connect();

        // Getting the response code
        int responseCode = conn.getResponseCode();

        if (responseCode == 200) {
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

                            clan.getMembers().put(membershipId, new Member(membershipId, displayName, membershipType,
                                    bungieGlobalDisplayName, bungieGlobalDisplayNameCode, clan));
                        } catch (NullPointerException ex) {
                            LOGGER.info(displayName + " has yet to register for a bungieGlobalDisplayName");
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Error processing JSON result from " + url.toExternalForm(), ex);
                    }
                });
            }
        }
        conn.disconnect();
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

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.addRequestProperty("X-API-Key", apiKey);
        conn.addRequestProperty("Accept", "Application/Json");
        conn.connect();

        // Getting the response code
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
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
            }
        }
        conn.disconnect();
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
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.addRequestProperty("X-API-Key", apiKey);
            conn.addRequestProperty("Accept", "Application/Json");
            conn.connect();

            // Check the response code
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
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
            }
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
                URL url = new URI(String.format("https://www.bungie.net/Platform/User/Search/GlobalName/%d/",
                        page.getAndIncrement())).toURL();

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
                    URL url = new URI(String.format(
                            "https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Character/%s/Stats/Activities/?page=%d&mode=4&count=250",
                            member.getMemberType(), member.getUID(), character.getUID(), page)).toURL();

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

    public static RaidCarnageReport getRaidCarnageReport(String carnageReportId)
            throws IOException, URISyntaxException {
        URL url = new URI(String.format("https://stats.bungie.net/Platform/Destiny2/Stats/PostGameCarnageReport/%s/",
                carnageReportId)).toURL();

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
                                            sgcClanMembersMap, 0));

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

        HashMap<Platform, String> potwActivityReportAsCsv = getPlatformActivityReportsAsCsv(clanList);
        LOGGER.info("SGC Activity Report Complete");
        return potwActivityReportAsCsv;
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
                                sgcClanMembersMap, 0);
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
            System.gc();
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

    public static Clan initializeClan(SGC_Clan sgc_clan) throws IOException, URISyntaxException {
        Clan clan = new Clan(sgc_clan.Bungie_ID, sgc_clan.Primary_Platform);
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
            HashMap<String, Member> sgcClanMembersMap, int mode) throws IOException, URISyntaxException {
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
                                                    Mode.getFromValue(activityMode),
                                                    member.getClan().getClanPlatform());
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
            HashMap<String, Member> sgcClanMembersMap) throws IOException, URISyntaxException {
        LOGGER.debug(String.format("Processing PGCR %s for %s", activityWithSGCMembers.getUID(),
                member.getCombinedBungieGlobalDisplayName()));
        URL url = new URI(String.format("https://stats.bungie.net/Platform/Destiny2/Stats/PostGameCarnageReport/%s/",
                activityWithSGCMembers.getUID())).toURL();

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
                                .append("\"").append(clan.getCallsign()).append("\",")
                                .append("\"").append(member.getWeeklySGCActivity().get("SCORE")).append("\",")
                                .append("\"").append(titanClears).append("\",")
                                .append("\"").append(hunterClears).append("\",")
                                .append("\"").append(warlockClears).append("\",");
                        for (Mode mode : validModesForCPOTW) {
                            csvPart.append("\"").append(totalActivitiesWithSGCMembersByMode.get(mode)).append("\",");
                        }
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
                        sgcClanMembersMap, 0);
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

    public static boolean isValidDateFormat(String dateInput) {
        return dateInput.matches("[0-9]{4}[0-9]{2}[0-9]{2}");
    }

    private static String getActivityReportCsvHeader() {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",").append("\"Clan\",")
                .append("\"Community POTW Points\",").append("\"Titan Clears\",").append("\"Hunter Clears\",")
                .append("\"Warlock Clears\",");

        List<Mode> validModesForCPOTW = Mode.validModesForCPOTW();
        for (Mode mode : validModesForCPOTW) {
            stringBuilder.append("\"").append(mode.getName()).append("\",");
        }

        return stringBuilder.append("\n").toString();
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
        }
        conn.disconnect();
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
                }
                conn.disconnect();

            } catch (Exception ex) {
                LOGGER.error("Error processing JSON result for characterID: "
                        + characterId, ex);
            }
        });
    }
}