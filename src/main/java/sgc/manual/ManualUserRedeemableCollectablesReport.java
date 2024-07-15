package sgc;

import java.io.IOException;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;

public class ManualUserRedeemableCollectablesReport {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCActivityReportRunner.class);

        public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {
                try {
                        System.out.println("These Collectables have not been redeemed");
                        System.out.println(RaidReportTool.getMemberMissingRedeemableCollectables("hoppefalcon#7599"));
                        System.out.println("You may or may not have the following codes redeemed");
                        System.out.println(RaidReportTool.getNonCollectableRedeemables());
                } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
        }
}
