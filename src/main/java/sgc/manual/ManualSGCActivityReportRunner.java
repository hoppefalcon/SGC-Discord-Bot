package sgc.manual;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;

public class ManualSGCActivityReportRunner {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCActivityReportRunner.class);

        public static void main(String[] args) throws InterruptedException, IOException {

                int year = 2025;
                // LocalDate startDate = YearMonth.of(year, 1).atDay(1);
                // LocalDate endDate = YearMonth.of(year, 12).atEndOfMonth();
                LocalDate startDate = YearMonth.of(year, 10).atDay(21);
                LocalDate endDate = YearMonth.of(year, 10).atDay(28);
                LOGGER.info(String.format("Starting %s to %s SGC Activity Report", startDate.toString(),
                                endDate.toString()));

                String potwActivityReportAsCsv = RaidReportTool
                                .getSGCWeeklyActivityReport(startDate,
                                                endDate,
                                                null,
                                                null,
                                                null);

                LOGGER.info(String.format("Finished %s to %s SGC Activity Report", startDate.toString(),
                                endDate.toString()));

                Path path = Paths.get("target", "SGC_Annual_CPOTW.csv");
                Files.write(path,
                                potwActivityReportAsCsv.getBytes(StandardCharsets.UTF_8));

                LOGGER.info("SGC Annual Activity Report Complete");
        }
}