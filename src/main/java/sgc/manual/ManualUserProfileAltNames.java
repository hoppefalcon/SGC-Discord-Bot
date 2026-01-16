package sgc.manual;

import java.io.IOException;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;

public class ManualUserProfileAltNames {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCActivityReportRunner.class);

        public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {
                try {
                        System.out.println(RaidReportTool.getMembersAltNames("hoppefalcon#7599"));
                } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
        }
}
