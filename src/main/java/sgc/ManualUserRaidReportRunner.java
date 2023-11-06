package sgc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;

public class ManualUserRaidReportRunner {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualUserRaidReportRunner.class);

        public static void main(String[] args) throws Exception {
                try {
                        String userBungieId = "Falacy#7560";
                        String userWeeklyClears = RaidReportTool.getUserRaidReport(userBungieId);
                        System.out.println(userWeeklyClears);
                } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                }
        }
}
