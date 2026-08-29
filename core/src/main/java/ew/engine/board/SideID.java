package ew.engine.board;

/** One of the two commanders. */
public enum SideID {
    ONE,
    TWO,
    NEUTRAL;

    public SideID opponent() {
        return switch(this) {
            case ONE -> SideID.TWO;
            case TWO -> SideID.ONE;
            case NEUTRAL -> SideID.NEUTRAL;
        };
    }
}
