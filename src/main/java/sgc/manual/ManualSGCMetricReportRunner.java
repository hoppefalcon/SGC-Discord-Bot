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

public class ManualSGCMetricReportRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCMetricReportRunner.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);

    public static void main(String[] args) throws InterruptedException, IOException {
        List<Clan> clanList = RaidReportTool.initializeClanList();

        LOGGER.info("Starting SGC Metric Report");

        HashMap<String, String> metrics = new HashMap<>();
        metrics.put("1765255052", "Flawless Trials Ticket");

        for (int i = 0; i < clanList.size(); i++) {
            Clan clan = clanList.get(i);

            List<Callable<Object>> tasks = new ArrayList<>();
            LOGGER.info("Starting to process " + clan.getCallsign());
            clan.getMembers().forEach((memberId, member) -> {
                tasks.add(() -> {
                    if (member.hasNewBungieName()) {
                        try {
                            LOGGER.debug("Starting to process " + member.getDisplayName());
                            member.getMetrics().putAll(
                                    RaidReportTool.getMembersMetrics(member, List.copyOf(metrics.keySet())));
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
        metrics.values().forEach(s -> stringBuilder.append("\"").append(s).append("\","));
        stringBuilder.append("\n");

        clanList.forEach(clan -> {
            StringBuilder csvPart = new StringBuilder();
            clan.getMembers()
                    .forEach((memberId, member) -> {
                        if (member.hasNewBungieName()) {
                            csvPart.append("\"").append(member.getDisplayName()).append("\",")
                                    .append("\"").append(member.getCombinedBungieGlobalDisplayName()).append("\",")
                                    .append("\"").append(clan.getCallsign()).append("\",");
                            metrics.keySet().forEach(
                                    s -> csvPart.append("\"").append(member.getMetrics().get(s)).append("\","));
                            csvPart.append("\n");
                        }
                    });
            stringBuilder.append(csvPart.toString());
        });

        Path outputPath = Paths.get("target", "SGC_Metric_Report.csv");
        Files.write(outputPath, stringBuilder.toString().getBytes(StandardCharsets.UTF_8));

        LOGGER.info("SGC Metric Report Complete");
    }
}
