package sgc.manual;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.YearMonth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;

public class ManualUserPOTWReportRunner {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCActivityReportRunner.class);

        public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {

                int year = 2025;
                // LocalDate startDate = YearMonth.of(year, 1).atDay(1);
                // LocalDate endDate = YearMonth.of(year, 12).atEndOfMonth();
                LocalDate startDate = YearMonth.of(year, 12).atDay(2);
                LocalDate endDate = YearMonth.of(year, 12).atDay(8);
                String userBungieId = "Riller#5914";
                System.out.printf("%s CPOTW Score: %d", userBungieId,
                                RaidReportTool.getUserCommunityActivityReport(userBungieId, startDate, endDate)
                                                .getWeeklySGCActivity()
                                                .get("SCORE"));

        }
}
