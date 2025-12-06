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

public class ManualSGCTriumphReportRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCTriumphReportRunner.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);
    private static List<String> filteredClanList = Arrays.asList(SGC_Clan.VII.Bungie_ID);

    public static void main(String[] args) throws InterruptedException, IOException {
        List<Clan> clanList = RaidReportTool.initializeClanList();

        LOGGER.info("Starting SGC Triumph Report");

        HashMap<String, String> triumphs = new HashMap<>();
        triumphs.put("2236876295", "Sundered Doctrine Solo");
        triumphs.put("591040974", "Vesper's Host Solo");
        triumphs.put("2905044529", "Warlord's Ruin Solo");
        triumphs.put("3584441401", "Ghosts of the Deep Solo");
        triumphs.put("1151761978", "Spire of the Watcher Solo");
        triumphs.put("755549938", "Duality Solo");
        triumphs.put("678858776", "Grasp of Avarice Solo");
        triumphs.put("3002642730", "Prophecy Solo");
        triumphs.put("3841336511", "Pit of Heresy Solo");
        triumphs.put("3899996566", "The Shattered Throne Solo");

        for (int i = 0; i < clanList.size(); i++) {
            Clan clan = clanList.get(i);

            List<Callable<Object>> tasks = new ArrayList<>();
            if (filteredClanList.contains(clan.getClanId()) || filteredClanList.isEmpty()) {
                LOGGER.info("Starting to process " + clan.getCallsign());
                clan.getMembers().forEach((memberId, member) -> {
                    tasks.add(() -> {
                        if (member.hasNewBungieName()) {
                            try {
                                LOGGER.debug("Starting to process " + member.getDisplayName());
                                member.getCollectibles().putAll(
                                        RaidReportTool.getMembersTriumphs(member, List.copyOf(triumphs.keySet())));
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
        triumphs.values().forEach(s -> stringBuilder.append("\"").append(s).append("\","));
        stringBuilder.append("\n");

        clanList.forEach(clan -> {
            if (filteredClanList.contains(clan.getClanId()) || filteredClanList.isEmpty()) {
                StringBuilder csvPart = new StringBuilder();
                clan.getMembers()
                        .forEach((memberId, member) -> {
                            if (member.hasNewBungieName()) {
                                csvPart.append("\"").append(member.getDisplayName()).append("\",")
                                        .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",")
                                        .append("\"").append(clan.getCallsign()).append("\",");
                                triumphs.keySet().forEach(
                                        s -> csvPart.append("\"").append(member.getCollectibles().get(s))
                                                .append("\","));
                                csvPart.append("\n");
                            }
                        });
                stringBuilder.append(csvPart.toString());
            }
        });

        Path outputPath = Paths.get("target", "SGC_Triumph_Report.csv");
        Files.write(outputPath, stringBuilder.toString().getBytes(StandardCharsets.UTF_8));

        LOGGER.info("SGC Triumph Report Complete");
    }
}
