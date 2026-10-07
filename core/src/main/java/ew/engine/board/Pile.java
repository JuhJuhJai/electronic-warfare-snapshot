package ew.engine.board;

import java.util.List;

public class Pile {
    public PileType type; // There's a world where these change. .
    public SideID owner;
    public List<CardInstance> contents;
    public Position position;

    /** Owner should not be SideID.Neutral. */
    public Pile(PileType type, SideID owner, List<CardInstance> contents) {
        this.type = type;
        this.owner = owner;
        this.contents = contents;
        this.position = getPosition(getBoardLocation());
    }

    public void updatePosition(BoardLocation location) {
        this.position = getPosition(location);
    }

    private Position getPosition(BoardLocation location) {
        return new Position(location, CardRideState.NORMAL, -1);
    }

    private BoardLocation getBoardLocation() {
        if (owner == SideID.ONE)
            return switch (type) {
                case Deck -> BoardLocation.DECK_ONE;
                case DecisiveDeck -> BoardLocation.DECISIVE_PILE_ONE;
                case Hand -> BoardLocation.HAND_ONE;
                case DestroyedPile -> BoardLocation.DESTROYED_PILE_ONE;
                case DiscardPile -> BoardLocation.DISCARD_PILE_ONE;
                case DisplacedPile -> BoardLocation.DISPLACED_PILE_ONE;
            };
        else if (owner == SideID.TWO)
            return switch (type) {
                case Deck -> BoardLocation.DECK_TWO;
                case DecisiveDeck -> BoardLocation.DECISIVE_PILE_TWO;
                case Hand -> BoardLocation.HAND_TWO;
                case DestroyedPile -> BoardLocation.DESTROYED_PILE_TWO;
                case DiscardPile -> BoardLocation.DISCARD_PILE_TWO;
                case DisplacedPile -> BoardLocation.DISPLACED_PILE_TWO;
            };
        else return BoardLocation.BOARD;
    }
}
