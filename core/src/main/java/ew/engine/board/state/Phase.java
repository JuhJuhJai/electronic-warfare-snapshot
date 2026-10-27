package ew.engine.board.state;

/** The four phases of a turn, in order (CR6.0.1). */
public enum Phase {
    SETUP,
    DRAW,
    BUILD,
    COMBAT,
    AFTERMATH,
    TUTORIAL;

    public int toDeclared() { return ordinal(); }
    public Phase next() {
        return switch(this) {
            case DRAW -> Phase.BUILD;
            case BUILD -> Phase.COMBAT;
            case COMBAT -> Phase.AFTERMATH;
            case AFTERMATH, SETUP -> Phase.DRAW;
            case TUTORIAL -> TUTORIAL;
        };
    }
}
