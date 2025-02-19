package sgc.manual.fallout;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.Clan;
import sgc.bungie.api.processor.RaidReportTool;

public class ManualFalloutCollectibleReportRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualFalloutCollectibleReportRunner.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);
    private static List<String> filteredClanList = Arrays.asList();

    public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {

        LOGGER.info("Starting Fallout Collectible Report");

        HashMap<String, String> collectibles = new HashMap<>();
        collectibles.put("1763610692", "Whisper of the Worm");
        collectibles.put("3613141427", "Microcosm");
        collectibles.put("1988948484", "Divinity");
        collectibles.put("360254771", "Outbreak Perfected");
        collectibles.put("4027219968", "Gjallarhorn");
        collectibles.put("3324472233", "Dead Man's Tale");
        collectibles.put("360554695", "Dead Messenger");
        collectibles.put("2176629195", "Choir of One");
        collectibles.put("2629609052", "Vexcalibur");
        collectibles.put("3826612761", "Wish-Keeper");
        collectibles.put("1161231112", "Revision Zero");
        collectibles.put("3860168553", "Solipsism");
        collectibles.put("1026253929", "Relativism");
        collectibles.put("2371517663", "Stoicism");

        Clan clan = RaidReportTool.initializeClan("5064777");
        List<Callable<Object>> tasks = new ArrayList<>();

        if (filteredClanList.contains(clan.getClanId()) || filteredClanList.isEmpty()) {
            clan.getMembers().forEach((memberId, member) -> {
                tasks.add(() -> {
                    if (member.hasNewBungieName()) {
                        try {
                            LOGGER.debug("Starting to process " + member.getDisplayName());
                            member.getCollectibles().putAll(
                                    RaidReportTool.getMembersCollections(member,
                                            List.copyOf(collectibles.keySet())));
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
        }

        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",").append("\"Clan\",");
        collectibles.values().forEach(s -> stringBuilder.append("\"").append(s).append("\","));
        stringBuilder.append("\n");

        StringBuilder csvPart = new StringBuilder();
        if (filteredClanList.contains(clan.getClanId()) || filteredClanList.isEmpty())
            clan.getMembers()
                    .forEach((memberId, member) -> {
                        if (member.hasNewBungieName()) {
                            csvPart.append("\"").append(member.getDisplayName()).append("\",")
                                    .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",")
                                    .append("\"").append(clan.getCallsign()).append("\",");
                            collectibles.keySet().forEach(
                                    s -> csvPart.append("\"").append(member.getCollectibles().get(s))
                                            .append("\","));
                            csvPart.append("\n");
                        }
                    });
        stringBuilder.append(csvPart.toString());

        Path outputPath = Paths.get("target",
                clan.getCallsign() + "_Collectible_Report.csv");
        Files.write(outputPath, stringBuilder.toString().getBytes(StandardCharsets.UTF_8));

        LOGGER.info("Fallout Collectible Report Complete");
    }
}
