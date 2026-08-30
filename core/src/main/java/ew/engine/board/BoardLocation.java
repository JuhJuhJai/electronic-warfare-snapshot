package ew.engine.board;

public enum BoardLocation {
    /** This position is used only for zones that are not usable in the game. */
    BOARD,

    FIELD,

    COMMANDER_ONE, // used for direct targets on commander one
    DECK_ONE,
    HAND_ONE,
    DECISIVE_PILE_ONE,
    DESTROYED_PILE_ONE,
    DISCARD_PILE_ONE,
    DISPLACED_PILE_ONE,

    COMMANDER_TWO, // used for direct targets on commander two
    DECK_TWO,
    HAND_TWO,
    DECISIVE_PILE_TWO,
    DESTROYED_PILE_TWO,
    DISCARD_PILE_TWO,
    DISPLACED_PILE_TWO;

    public int toDeclared() {
        return this.ordinal();
    }

    /** This position is used only for zones that are not usable in the game. */
    public static boolean inBoard(BoardLocation location) {
        return location == BOARD;
    }

    public static boolean inField(BoardLocation location) {
        return location == FIELD;
    }

    public static boolean inCommander(BoardLocation location) {
        return location == COMMANDER_ONE || location == COMMANDER_TWO;
    }

    public static boolean inHand(BoardLocation location) {
        return location == HAND_ONE || location == HAND_TWO;
    }

    public static boolean inDeck(BoardLocation location) {
        return location == DECK_ONE || location == DECK_TWO;
    }

    public static boolean inDecisivePile(BoardLocation location) {
        return location == DECISIVE_PILE_ONE || location == DECISIVE_PILE_TWO;
    }

    public static boolean inDestroyedPile(BoardLocation location) {
        return location == DESTROYED_PILE_ONE || location == DESTROYED_PILE_TWO;
    }

    public static boolean inDiscardPile(BoardLocation location) {
        return location == DISCARD_PILE_ONE || location == DISCARD_PILE_TWO;
    }

    public static boolean inDisplacedPile(BoardLocation location) {
        return location == DISPLACED_PILE_ONE || location == DISPLACED_PILE_TWO;
    }
}
