package sgc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;

public class ManualUserRaidReportRunner {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualUserRaidReportRunner.class);

        public static void main(String[] args) throws Exception {
                try {
                        LocalDate startDate = LocalDate.parse("20221206",
                                        DateTimeFormatter.BASIC_ISO_DATE);
                        LocalDate endDate = LocalDate.parse("20230418",
                                        DateTimeFormatter.BASIC_ISO_DATE);
                        String userBungieId = "hoppefalcon#7599";
                        String userWeeklyClears = RaidReportTool.getUserWeeklyClears(userBungieId,
                                        startDate,
                                        endDate);
                        System.out.println(userWeeklyClears);
                } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                }
        }
}
