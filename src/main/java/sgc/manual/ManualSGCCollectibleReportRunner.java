package sgc.manual;

import java.io.IOException;
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
import sgc.types.SGC_Clan;

public class ManualSGCCollectibleReportRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCCollectibleReportRunner.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);
    private static List<String> filteredClanList = Arrays.asList(SGC_Clan.VII.Bungie_ID);

    public static void main(String[] args) throws InterruptedException, IOException {
        List<Clan> clanList = RaidReportTool.initializeClanList();

        LOGGER.info("Starting SGC Collectible Report");

        HashMap<String, String> collectibles = new HashMap<>();
        collectibles.put("199171385", "One Thousand Voices");
        collectibles.put("1988948484", "Divinity");
        collectibles.put("753200559", "Eyes of Tomorrow");
        collectibles.put("2817568609", "Collective Obligation");
        collectibles.put("192937277", "Touch of Malice");
        collectibles.put("2553509474", "Conditional Finality");
        collectibles.put("203521123", "Necrochasm");
        collectibles.put("3411864064", "Euphony");
        collectibles.put("1660030044", "Wish-Ender");
        collectibles.put("1258579677", "Xenophage");
        collectibles.put("4027219968", "Gjallarhorn");
        collectibles.put("467760883", "Heartshadow");
        collectibles.put("3558330464", "Hierarchy of Needs");
        collectibles.put("161963863", "The Navigator");
        collectibles.put("3275654322", "Buried Bloodline");
        collectibles.put("1643809765", "Ice Breaker");
        collectibles.put("3935854305", "The Lament");
        collectibles.put("2289185883", "Still Hunt");
        collectibles.put("2843753795", "Final Warning");
        collectibles.put("3324472233", "Dead Man's Tale");
        collectibles.put("888224289", "Deathbringer");
        collectibles.put("4226434173", "Deterministic Chaos");
        collectibles.put("1660030045", "Malfeasance");
        collectibles.put("4028619089", "Parasite");
        collectibles.put("360554695", "Dead Messenger");
        collectibles.put("2629609053", "Winterbite");
        collectibles.put("2629609052", "Vexcalibur");
        collectibles.put("328283190", "Edge of Concurrence");
        collectibles.put("3810283242", "Edge of Action");
        collectibles.put("1089205875", "Edge of Intent");
        collectibles.put("2176629195", "Choir of One");
        collectibles.put("1763610692", "Whisper of the Worm");
        collectibles.put("360254771", "Outbreak Perfected");
        collectibles.put("1028725073", "Forerunner");

        for (int i = 0; i < clanList.size(); i++) {
            Clan clan = clanList.get(i);
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

        }
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",").append("\"Clan\",");
        collectibles.values().forEach(s -> stringBuilder.append("\"").append(s).append("\","));
        stringBuilder.append("\n");

        clanList.forEach(clan -> {
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
        });

        Path outputPath = Paths.get("target", "SGC_Collectible_Report.csv");
        Files.write(outputPath, stringBuilder.toString().getBytes(StandardCharsets.UTF_8));

        LOGGER.info("SGC Collectible Report Complete");
    }
}
