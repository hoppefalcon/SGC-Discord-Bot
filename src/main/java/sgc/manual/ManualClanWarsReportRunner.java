package sgc.manual;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;
import sgc.types.SGC_Clan;

public class ManualClanWarsReportRunner {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualClanWarsReportRunner.class);
        private static List<String> filteredClanList = Arrays.asList(SGC_Clan.VII.Bungie_ID, SGC_Clan.VII.Bungie_ID);
        // private static List<String> filteredClanList =
        // Arrays.asList(SGC_Clan.SOL.Bungie_ID);

        public static void main(String[] args) throws InterruptedException, IOException {
                LOGGER.info("SGC ManualClanWarsReportRunner Started");

                int year = 2025;
                LocalDate startDate = YearMonth.of(year, 8).atDay(31);
                LocalDate endDate = YearMonth.of(year, 9).atDay(6);
                LOGGER.info(String.format("Starting %s to %s SGC Activity Report", startDate.toString(),
                                endDate.toString()));

                String potwActivityReportAsCsv = RaidReportTool
                                .getSGCWeeklyClanWars2025Report(startDate,
                                                endDate,
                                                null,
                                                null,
                                                null, filteredClanList);

                LOGGER.info(String.format("Finished %s to %s SGC Activity Report", startDate.toString(),
                                endDate.toString()));

                Path path = Paths.get("target", "SGC_Weekly_Clan_Wars_2025.csv");
                Files.write(path,
                                potwActivityReportAsCsv.getBytes(StandardCharsets.UTF_8));

                LOGGER.info("SGC Annual Activity Report Complete");
        }
}