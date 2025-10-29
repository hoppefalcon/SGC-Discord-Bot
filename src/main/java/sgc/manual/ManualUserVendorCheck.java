package sgc.manual;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;

public class ManualUserVendorCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualUserVendorCheck.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);

    public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {
        RaidReportTool.getLatestGlobalVendorInventory();
    }
}
