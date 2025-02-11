package sgc.manual.fallout;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.Clan;
import sgc.bungie.api.processor.RaidReportTool;

public class ManualFalloutRaidReportRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualFalloutRaidReportRunner.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting Fallout Raid Report");
        Clan clan = RaidReportTool.initializeClan("5064777");
        RaidReportTool.getClanRaidReport(clan, null);

        Path outputPath = Paths.get("target", clan.getCallsign() + "_Raid_Report.csv");
        Files.write(outputPath,
                RaidReportTool.getClanRaidReportAsCsv(clan).toString().getBytes(StandardCharsets.UTF_8));

        LOGGER.info("Fallout Raid Report Complete");
    }
}
