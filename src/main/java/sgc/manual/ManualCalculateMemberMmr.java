package sgc.manual;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.Member;
import sgc.bungie.api.processor.RaidReportTool;

public class ManualCalculateMemberMmr {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualFireteamBlanaceRunner.class);

        private static List<String> userBungieIdList = Arrays.asList("BeastxSynner#9483", "MoJo JoJo#7071",
                        "ΛNOiD#8458", "hoppefalcon#7599", "Mister Wrecked#2123", "phonicphantom#7028",
                        "Diffizzle#3289", "Knitehawk#6423", "ZkMushroom#4735", "PureChiLL#2572", "M1_AbramsTank#2068",
                        "KoolDudeIsHere#5568", "tacoes#2929", "GamR-_-Grrrl#7960", "PaleGreySky#2955", "ElvesDoom#1777",
                        "devilito666#3256", "Athena kenwick#9637");
        private static HashMap<Member, Double> fireteamMmrMap = new HashMap<>();
        private static LinkedHashMap<Member, Double> sortedFireteamMmrMap = new LinkedHashMap<>();
        private static ArrayList<Double> mmrs = new ArrayList<>();

        public static void main(String[] args) throws Exception {
                userBungieIdList.forEach(userBungieId -> {
                        try {
                                Member member = RaidReportTool.getMemberInformation(userBungieId);
                                Double memberMmr = RaidReportTool.calculateMemberMmr(member);
                                if (memberMmr != null) {
                                        fireteamMmrMap.put(member,
                                                        memberMmr);
                                        mmrs.add(memberMmr);
                                }
                                System.out.println(String.format("%s | %.2f",
                                                member.getCombinedBungieGlobalDisplayName(), memberMmr));
                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                        }
                });

                System.out.println("\n\nSorted by MMR\n------------------");
                mmrs.sort((mmr1, mmr2) -> (mmr2).compareTo(mmr1));
                for (int i = 0; i < mmrs.size(); i++) {
                        Member found = null;
                        for (Entry<Member, Double> entry : fireteamMmrMap.entrySet()) {
                                if (mmrs.get(i).equals(entry.getValue()) && found == null) {
                                        sortedFireteamMmrMap.put(entry.getKey(), entry.getValue());
                                        found = entry.getKey();
                                }
                        }

                        fireteamMmrMap.remove(found);
                }
                for (Entry<Member, Double> entry : sortedFireteamMmrMap.entrySet()) {
                        System.out.println(String.format("%s | %.2f",
                                        entry.getKey().getCombinedBungieGlobalDisplayName(), entry.getValue()));
                }

        }
}
