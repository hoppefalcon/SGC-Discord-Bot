/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.sherpa.sheets;

/**
 * @author chris hoppe
 */
public class RaidActivity {

    private final Raid raid;
    private int totalClears = 0;
    private int weeklyClears = 0;

    public RaidActivity(Raid raid) {
        this.raid = raid;
    }

    public void addClears(int newClears) {
        totalClears += newClears;
    }

    public void addClears(double newClears) {
        totalClears += newClears;
    }

    public Raid getRaid() {
        return raid;
    }

    public int getTotalClears() {
        return totalClears;
    }

    public void addWeeklyClears(int newClears) {
        weeklyClears += newClears;
    }

    public void addWeeklyClears(double newClears) {
        weeklyClears += newClears;
    }

    /**
     * @return the weeklyClears
     */
    public int getWeeklyClears() {
        return weeklyClears;
    }

}
