package ew.engine.cards;

public enum CardType {
    ATTACK, PRODUCTION,
    DECISIVE_ATTACK, DECISIVE_PRODUCTION,
    TOKEN_ATTACK, TOKEN_PRODUCTION,

    ZONE, COMMANDER;

    /** 1 is an attack type, 2 is a production type, and 0 is neither. */
    public byte getType() {
        return switch (this.ordinal()) {
            case 0, 2, 4 -> 1;
            case 1, 3, 5 -> 2;
            default -> 0;
        };
    }
}
