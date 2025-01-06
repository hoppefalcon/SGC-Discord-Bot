package sgc.manual;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;

public class ManualFireteamBlanaceRunner {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualFireteamBlanaceRunner.class);

        public static void main(String[] args) throws Exception {
                try {
                        String userBungieId = "Mister Wrecked#2123";
                        String fireteamBalance = RaidReportTool.getFireteamBalance(userBungieId);
                        System.out.println(fireteamBalance);
                } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                }
        }
}
