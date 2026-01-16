package sgc.types;

public enum CollectableType {
    EMBLEM("Emblem"), TRANSMAT_EFFECT("Transmat Effect"), EMOTE("Emote"), SHADDER("Shadder");

    private final String type;

    private CollectableType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
