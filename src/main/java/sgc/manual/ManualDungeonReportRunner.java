package sgc.manual;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.Clan;
import sgc.bungie.api.processor.RaidReportTool;
import sgc.types.SGC_Clan;

public class ManualDungeonReportRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCCollectibleReportRunner.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);

    public static void main(String[] args) throws Exception {
        List<Clan> clanList = RaidReportTool.initializeClanList();

        LOGGER.info("Starting SGC Dungeon Report");
        Clan sol = null;
        for (Clan clan : clanList) {
            if (clan.getClanId().equals(SGC_Clan.SOL.Bungie_ID)) {
                sol = clan;
            }
        }
        RaidReportTool.getClanDungeonReport(sol, null);

        Path outputPath = Paths.get("target", "SGC_Dungeon_Report.csv");
        Files.write(outputPath,
                RaidReportTool.getClanDungeonReportAsCsv(sol).toString().getBytes(StandardCharsets.UTF_8));

        LOGGER.info("SGC Dungeon Report Complete");
    }
}
