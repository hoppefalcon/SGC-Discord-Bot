/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.sherpa.sheets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    private static List<String> pcClanIds = Arrays.asList("3087185", "3019103", "3063489", "3007121", "3008645",
            "3076620", "3090996", "3100797", "3095868", "2820714", "2801315", "3915247", "3949151", "3095835",
            "3070603", "3795604");
    private static List<String> xbClanIds = Arrays.asList("4327464", "4327434", "4327418", "4327389", "4418635");
    private static List<String> psClanIds = Arrays.asList("4327587", "4327584", "4327575", "4327536", "4327542");

    private static ExecutorService executorService = Executors.newFixedThreadPool(15);

    private static AtomicInteger PGCR_COUNT = new AtomicInteger(0);

    /**
     * @param args the command line arguments
     * @throws IOException
     */

    public static void main(String[] args) throws InterruptedException,
            IOException {
        System.out.println(System.getProperty("os.name"));
        Instant start = Instant.now();

        String output = getSGCWeeklyActivityReport(LocalDate.parse("20220222",
                DateTimeFormatter.BASIC_ISO_DATE),
                LocalDate.parse("20220228", DateTimeFormatter.BASIC_ISO_DATE));

        executorService.shutdown();

        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start,
                end);

        long hours = timeElapsed.toHours();
        long minutes = timeElapsed.toMinutesPart();
        long secounds = timeElapsed.toSecondsPart();
        System.out.println("Post Game Carnage Reports Processed: " +
                PGCR_COUNT.get());
        System.out.println(String.format("DONE (%02d:%02d:%02d)", hours, minutes,
                secounds));
    }

    public static void initializeClanIdMap() {

        pcClanIds.forEach((clanId) -> {
            try {
                Clan clan = new Clan(clanId);
                getClanInfo(clan);
                pcClanIdMap.put(clan.getName(), clan.getClanId());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });

        xbClanIds.forEach((clanId) -> {
            try {
                Clan clan = new Clan(clanId);
                getClanInfo(clan);
                xbClanIdMap.put(clan.getName(), clan.getClanId());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });

        psClanIds.forEach((clanId) -> {
            try {
                Clan clan = new Clan(clanId);
                getClanInfo(clan);
                psClanIdMap.put(clan.getName(), clan.getClanId());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }

    /**
     * @return the pcclanidmap
     */
    public static HashMap<String, String> getPcclanidmap() {
        return pcClanIdMap;
    }

    /**
     * @return the xbclanidmap
     */
    public static HashMap<String, String> getXbclanidmap() {
        return xbClanIdMap;
    }

    /**
     * @return the psclanidmap
     */
    public static HashMap<String, String> getPsclanidmap() {
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
                    // if (entry.getAsJsonObject().get("deleted").getAsBoolean() == false) {
                    String characterId = entry.getAsJsonObject().get("characterId").getAsString();
                    member.getCharacters().put(characterId, new Character(characterId));
                    // }
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

    public static String getSGCWeeklyActivityReport(LocalDate startDate, LocalDate endDate)
            throws InterruptedException, IOException {
        LOGGER.info("Starting SGC Member Participation Report");
        HashMap<String, Clan> clanMap = initializeClanMap();
        HashMap<String, Member> sgcClanMembersMap = new HashMap<>();

        clanMap.forEach((uid, clan) -> {
            clan.getMembers().forEach((id, member) -> {
                if (member.hasNewBungieName()) {
                    sgcClanMembersMap.put(id, member);
                }
            });
        });
        List<Callable<Object>> tasks = new ArrayList<>();
        sgcClanMembersMap.forEach((uid, member) -> {
            tasks.add(() -> {
                try {
                    getMembersClearedActivities(member, startDate, endDate, sgcClanMembersMap);
                } catch (IOException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
                return member;
            });
        });
        executorService.invokeAll(tasks);
        String potwActivityReportAsCsv = getPOTWActivityReportAsCsv(clanMap, sgcClanMembersMap);

        LOGGER.info("SGC Member Participation Report Complete");
        return potwActivityReportAsCsv;
    }

    public static HashMap<String, Clan> initializeClanMap() {
        LOGGER.debug("Initializing Clan Map");
        HashMap<String, Clan> map = new HashMap<>();
        pcClanIds.forEach((clanId) -> {
            try {
                Clan clan = new Clan(clanId);
                getClanInfo(clan);
                getClanMembers(clan);
                map.put(clan.getClanId(), clan);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });

        xbClanIds.forEach((clanId) -> {
            try {
                Clan clan = new Clan(clanId);
                getClanInfo(clan);
                getClanMembers(clan);
                map.put(clan.getClanId(), clan);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });

        psClanIds.forEach((clanId) -> {
            try {
                Clan clan = new Clan(clanId);
                getClanInfo(clan);
                getClanMembers(clan);
                map.put(clan.getClanId(), clan);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
        LOGGER.debug("Clan Map Initialized");
        return map;
    }

    public static void getMembersClearedActivities(Member member, LocalDate startDate, LocalDate endDate,
            HashMap<String, Member> sgcClanMembersMap) throws IOException {

        LOGGER.debug(String.format("Getting Cleared Activities for %s", member.getCombinedBungieGlobalDisplayName()));
        getMemberCharacters(member);
        member.getCharacters().forEach((characteruid, character) -> {
            try {
                boolean next = false;
                List<GenericActivity> genericActivitiesToProcess = new ArrayList<>();
                for (int page = 0; !next; page++) {
                    URL url = new URL(String.format(
                            "https://www.bungie.net/Platform/Destiny2/%s/Account/%s/Character/%s/Stats/Activities/?page=%d&count=250",
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
                            List<JsonObject> clearedActivities = IntStream.range(0, results.size())
                                    .mapToObj(index -> (JsonObject) results.get(index)).filter((result) -> {
                                        PGCR_COUNT.incrementAndGet();
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
                                        boolean completed = result.getAsJsonObject().getAsJsonObject("values")
                                                .getAsJsonObject("completed")
                                                .getAsJsonObject("basic").getAsJsonPrimitive("value")
                                                .getAsDouble() == 1.0;
                                        return completed;
                                    }).collect(Collectors.toList());

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

                genericActivitiesToProcess.forEach((activityWithSGCMembers) -> {
                    try {
                        getSGCMemberCarnageReport(member, activityWithSGCMembers, sgcClanMembersMap);
                        if (activityWithSGCMembers.earnsPoints()) {
                            character.addClearedActivitiesWithSGCMembers(activityWithSGCMembers);
                        }
                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                });

            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        });
    }

    public static void getSGCMemberCarnageReport(Member member, GenericActivity activityWithSGCMembers,
            HashMap<String, Member> sgcClanMembersMap) throws IOException {
        LOGGER.debug(String.format("Processing PCGRs for %s", member.getCombinedBungieGlobalDisplayName()));
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
                double team = entry.getAsJsonObject().getAsJsonObject("values")
                        .getAsJsonObject("team")
                        .getAsJsonObject("basic").getAsJsonPrimitive("value")
                        .getAsDouble();

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

    public static String getPOTWActivityReportAsCsv(HashMap<String, Clan> clanMap,
            HashMap<String, Member> sgcClanMembersMap) {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",").append("\"Clan\",").append("\"Points\"")
                .append("\n");

        sgcClanMembersMap.forEach((memberId, member) -> {
            stringBuilder.append("\"").append(member.getDisplayName()).append("\",")
                    .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",")
                    .append("\"").append(clanMap.get(member.getClanId()).getCallsign()).append("\",")
                    .append("\"").append(member.getClearedActivitiesWithSGCMembersPoints()).append("\"\n");
        });

        return stringBuilder.toString();
    }

}
