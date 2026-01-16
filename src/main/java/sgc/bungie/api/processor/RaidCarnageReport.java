package sgc.bungie.api.processor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import sgc.types.Raid;

public class RaidCarnageReport {
    private final List<RaidCarnageReportPlayer> players = new ArrayList<>();
    private final Raid raid;
    private final LocalDate dateCompleted;

    /**
     * @param raid
     */
    public RaidCarnageReport(Raid raid, LocalDate dateCompleted) {
        this.raid = raid;
        this.dateCompleted = dateCompleted;
    }

    /**
     * @return the players
     */
    public List<RaidCarnageReportPlayer> getPlayers() {
        return players;
    }

    /**
     * @return the raid
     */
    public Raid getRaid() {
        return raid;
    }

    /**
     * @return the dateCompleted
     */
    public LocalDate getDateCompleted() {
        return dateCompleted;
    }

    public String getCSV() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BUNGIE ID").append(",");
        stringBuilder.append("PLATFORM").append(",");
        stringBuilder.append("CLASS").append(",");
        stringBuilder.append("COMPLETED").append(",");
        stringBuilder.append("KILLS").append(",");
        stringBuilder.append("ASSISTS").append(",");
        stringBuilder.append("OPPONENTS DEFESTED").append(",");
        stringBuilder.append("DEATHS").append(",");
        stringBuilder.append("K/D").append(",");
        stringBuilder.append("KA/D").append(",");
        stringBuilder.append("TIME").append(",");
        stringBuilder.append("\n");
        players.forEach(player -> {
            stringBuilder.append(player.getCsvOutput());
        });
        return stringBuilder.toString();
    }
}
