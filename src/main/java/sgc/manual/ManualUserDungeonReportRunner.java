package sgc.manual;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;

public class ManualUserDungeonReportRunner {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualUserDungeonReportRunner.class);

        public static void main(String[] args) throws Exception {
                try {
                        String userBungieId = "Hoppefalcon#7599";
                        String userWeeklyClears = RaidReportTool.getUserDungeonReport(userBungieId);
                        System.out.println(userWeeklyClears);
                } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                }
        }
}
