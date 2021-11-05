/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.sherpa.sheets;

/**
 * @author chris hoppe
 */
public class Activity {

    private final Raid raid;
    private int totalClears = 0;
    private int xlFireteamClears = 0;

    public Activity(Raid raid) {
        this.raid = raid;
    }

    public void addClears(int newClears) {
        totalClears += newClears;
    }

    public void addClears(double newClears) {
        totalClears += newClears;
    }

    public void addXlFireteamClears(int newClears) {
        xlFireteamClears += newClears;
    }

    public void addXlFireteamClears(double newClears) {
        xlFireteamClears += newClears;
    }

    public Raid getRaid() {
        return raid;
    }

    public int getTotalClears() {
        return totalClears;
    }

    public int getXlClears() {
        return xlFireteamClears;
    }

}
