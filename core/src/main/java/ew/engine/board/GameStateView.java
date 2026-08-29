package ew.engine.board;

/**
 * Generates the limited / extended information to display to a spectator, or
 * commander currently playing the game. This view's information should not
 * provide any more information than the player can see in the game, should
 * that player be able to see all of this class's variables.
 *
 * Thus,
 * It will hold the number each player has of things (cards in hand, deck, decisive deck, destroyed pile, etc.)
 * then, it will hold cardInstance which are revealed to the sideID.
 * The LibGDX portion will receive the cardInstance and place them in the proper position.
 * For example, if I say sideID.ONE has 4 cards in hand, and one of the given cardInstance has a Position in
 * sideID.ONE's hand, then that card will be revealed in their hand, and the other 3 will be blank. Same goes
 * for zones.
 */
public class GameStateView {
    public GameStateView(GameState state, SideID viewer) {

    }
}
