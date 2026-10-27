package ew.engine.board.commanderInstance;

public enum PileType {
    Deck,
    DecisiveDeck,
    Hand,
    DestroyedPile,
    DiscardPile,
    DisplacedPile;

    public int toDeclared() { return this.ordinal(); }
}
