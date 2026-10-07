package ew.engine.board;

public enum BoardLocation {
    /** This position is used only for zones that are not usable in the game. */
    BOARD(false),

    FIELD(true),

    COMMANDER_ONE(true), // used for direct targets on commander one
    DECK_ONE(false),
    HAND_ONE(false),
    DECISIVE_PILE_ONE(false),
    DESTROYED_PILE_ONE(true),
    DISCARD_PILE_ONE(true),
    DISPLACED_PILE_ONE(true),

    COMMANDER_TWO(true), // used for direct targets on commander two
    DECK_TWO(false),
    HAND_TWO(false),
    DECISIVE_PILE_TWO(false),
    DESTROYED_PILE_TWO(true),
    DISCARD_PILE_TWO(true),
    DISPLACED_PILE_TWO(true),

    REMOVED_FROM_GAME(true);

    private final boolean isPublic;

    BoardLocation(boolean isPublic) { this.isPublic = isPublic; }

    public boolean isPublicLocation() { return this.isPublic; }
    public int toDeclared() {
        return this.ordinal();
    }
    public static BoardLocation fromDeclared(int declared) {
        return values()[declared];
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
