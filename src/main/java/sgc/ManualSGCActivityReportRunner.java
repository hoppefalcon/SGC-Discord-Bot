package sgc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;

public class ManualSGCActivityReportRunner {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCActivityReportRunner.class);

        public static void main(String[] args) throws InterruptedException, IOException {

                int year = 2023;
                // LocalDate startDate = YearMonth.of(year, 1).atDay(1);
                // LocalDate endDate = YearMonth.of(year, 12).atEndOfMonth();
                LocalDate startDate = YearMonth.of(year, 9).atDay(12);
                LocalDate endDate = YearMonth.of(year, 9).atDay(18);
                LOGGER.info(String.format("Starting %s to %s SGC Activity Report", startDate.toString(),
                                endDate.toString()));

                HashMap<Platform, String> potwActivityReportAsCsv = RaidReportTool
                                .getSGCWeeklyActivityReport(startDate,
                                                endDate,
                                                null,
                                                null,
                                                null);

                LOGGER.info(String.format("Finished %s to %s SGC Activity Report", startDate.toString(),
                                endDate.toString()));

                Path xboxPath = Paths.get("target", Platform.XBOX.getName() +
                                "_CPOTW.csv");
                Files.write(xboxPath,
                                potwActivityReportAsCsv.get(Platform.XBOX).getBytes(StandardCharsets.UTF_8));
                Path pcPath = Paths.get("target", Platform.PC.getName() +
                                "_CPOTW.csv");
                Files.write(pcPath,
                                potwActivityReportAsCsv.get(Platform.PC).getBytes(StandardCharsets.UTF_8));
                Path psnPath = Paths.get("target", Platform.PSN.getName() +
                                "_CPOTW.csv");
                Files.write(psnPath,
                                potwActivityReportAsCsv.get(Platform.PSN).getBytes(StandardCharsets.UTF_8));

                LOGGER.info("SGC Annual Activity Report Complete");
        }
}
