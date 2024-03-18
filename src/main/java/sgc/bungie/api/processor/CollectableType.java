package sgc.bungie.api.processor;

public enum CollectableType {
    EMBLEM("Emblem"), TRANSMAT_EFFECT("Transmat Effect"), EMOTE("Emote");

    private final String type;

    private CollectableType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
