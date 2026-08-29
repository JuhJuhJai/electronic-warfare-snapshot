package ew.engine.board;

public enum BoardLocation {
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
}
