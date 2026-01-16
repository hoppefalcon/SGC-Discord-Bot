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

public class ManualClanAltNamesReportRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualClanAltNamesReportRunner.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);
    private static List<String> filteredClanList = Arrays.asList();

    public static void main(String[] args) throws InterruptedException, IOException {
        List<Clan> clanList = RaidReportTool.initializeClanList();

        LOGGER.info("Starting Clan Alt Name Report");

        Path outputPath = Paths.get("target", "Alt_Name_Report.csv");
        try {
            Files.write(outputPath, RaidReportTool
                    .getClanMembersAltNamesCSVByteArray(RaidReportTool.getClanInformation(SGC_Clan.VII.Bungie_ID)));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        LOGGER.info("Clan Alt Name Report Complete");
    }
}
