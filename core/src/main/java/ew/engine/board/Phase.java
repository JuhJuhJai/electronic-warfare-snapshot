package ew.engine.board;

/** The four phases of a turn, in order (CR6.0.1). */
public enum Phase {
    SETUP,
    DRAW,
    BUILD,
    COMBAT,
    AFTERMATH,
    TUTORIAL;

    public int getPhaseNum() { return ordinal(); }
    public Phase next() {
        return switch(this) {
            case SETUP -> Phase.DRAW;
            case DRAW -> Phase.BUILD;
            case BUILD -> Phase.COMBAT;
            case COMBAT -> Phase.AFTERMATH;
            case AFTERMATH -> Phase.DRAW;
            default -> this;
        };
    }
}
