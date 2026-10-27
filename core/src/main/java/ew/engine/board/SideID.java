package ew.engine.board;

/** One of the two commanders. */
public enum SideID {
    NEUTRAL, // Neither
    ONE,
    TWO,
    OMNI; // Perspective that shows everything, including cards in deck and their position, etc

    public SideID opponentOf() {
        return switch(this) {
            case ONE -> SideID.TWO;
            case TWO -> SideID.ONE;
            case NEUTRAL, OMNI -> throw new IllegalArgumentException("Cannot get the opponent of SideID " + this);
        };
    }
}
