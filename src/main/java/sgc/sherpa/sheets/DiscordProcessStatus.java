/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.sherpa.sheets;

/**
 * @author chris hoppe
 */
public enum DiscordProcessStatus {
    WAITING(":hourglass:"),
    PROCESSING(":pencil:"),
    DONE(":checkered_flag:"),
    ERROR(":triangular_flag_on_post:");

    private final String emojiStr;

    private DiscordProcessStatus(String emojiStr) {
        this.emojiStr = emojiStr;
    }

    public String getEmojiStr() {
        return emojiStr;
    }

    @Override
    public String toString() {
        return emojiStr;
    }

}