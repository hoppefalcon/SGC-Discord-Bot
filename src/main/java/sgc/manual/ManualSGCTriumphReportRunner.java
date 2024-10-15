package sgc.manual;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.Clan;
import sgc.bungie.api.processor.RaidReportTool;

public class ManualSGCTriumphReportRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCTriumphReportRunner.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);

    public static void main(String[] args) throws InterruptedException, IOException {
        List<Clan> clanList = RaidReportTool.initializeClanList();

        LOGGER.info("Starting SGC Triumph Report");

        HashMap<String, String> triumphs = new HashMap<>();
        triumphs.put("448634049", "Grandmaster");
        triumphs.put("2401440738", "Crota's End");
        triumphs.put("2216324956", "Root Of Nightmares");
        triumphs.put("3990841777", "King's Fall");
        triumphs.put("426817434", "Vow of the Disciple");
        triumphs.put("426817418", "Vault of Glass");
        triumphs.put("88999500", "Deep Stone Crypt");
        triumphs.put("1711561705", "Garden of Salvation");
        triumphs.put("2899839432", "Last Wish");

        for (int i = 0; i < clanList.size(); i++) {
            Clan clan = clanList.get(i);

            List<Callable<Object>> tasks = new ArrayList<>();
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
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\"Gamertag\",").append("\"BungieDisplayName\",").append("\"Clan\",");
        triumphs.values().forEach(s -> stringBuilder.append("\"").append(s).append("\","));
        stringBuilder.append("\n");

        clanList.forEach(clan -> {
            StringBuilder csvPart = new StringBuilder();
            clan.getMembers()
                    .forEach((memberId, member) -> {
                        if (member.hasNewBungieName()) {
                            csvPart.append("\"").append(member.getDisplayName()).append("\",")
                                    .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",")
                                    .append("\"").append(clan.getCallsign()).append("\",");
                            triumphs.keySet().forEach(
                                    s -> csvPart.append("\"").append(member.getCollectibles().get(s)).append("\","));
                            csvPart.append("\n");
                        }
                    });
            stringBuilder.append(csvPart.toString());
        });

        Path outputPath = Paths.get("target", "SGC_Triumph_Report.csv");
        Files.write(outputPath, stringBuilder.toString().getBytes(StandardCharsets.UTF_8));

        LOGGER.info("SGC Triumph Report Complete");
    }
}
