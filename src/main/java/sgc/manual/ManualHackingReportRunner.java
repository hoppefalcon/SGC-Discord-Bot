package sgc.manual;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.Clan;
import sgc.bungie.api.processor.Member;
import sgc.bungie.api.processor.RaidReportTool;

public class ManualHackingReportRunner {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCActivityReportRunner.class);

        public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {

                List<Clan> clanList = RaidReportTool.initializeClanList();
                HashMap<Member, ArrayList<Integer>> cheaters = new HashMap<>();
                LineNumberReader lines = new LineNumberReader(new FileReader("target\\d2hackingreport.txt"));
                for (String line = lines.readLine(); line != null; line = lines.readLine()) {
                        for (Clan clan : clanList) {
                                for (Member member : clan.getMembers().values()) {
                                        if (line.contains(member.getCombinedBungieGlobalDisplayName())) {
                                                ArrayList<Integer> linenums = cheaters.get(member);
                                                if (linenums == null) {
                                                        linenums = new ArrayList<>();
                                                }
                                                linenums.add(lines.getLineNumber());
                                                cheaters.put(member, linenums);
                                        }
                                }
                        }
                }
                System.out.println("-----------------------------------------------------------------");
                System.out.println("START OF REPORT");
                System.out.println("-----------------------------------------------------------------");
                cheaters.keySet().forEach(member -> {
                        System.out.printf("%s -> %s\n", member.getCombinedBungieGlobalDisplayName(),
                                        cheaters.get(member).toString());
                });
                System.out.println("-----------------------------------------------------------------");
                System.out.println("END OF REPORT");
                System.out.println("-----------------------------------------------------------------");
        }
}
