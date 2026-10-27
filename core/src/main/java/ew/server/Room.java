package ew.server;

import ew.engine.board.state.GameResult;
import ew.engine.resolver.log.Log;
import ew.engine.resolver.log.GameChoice;
import ew.engine.resolver.log.GameChoiceChange;
import ew.engine.resolver.view.FullGameStateView;
import ew.playerData.Commander;

import java.util.List;

public interface Room {
    String toString();
    long getRoomID();
    long getSeed();
    ControllerInstance getController1();
    ControllerInstance getController2();
    ControllerInstance getController(long ID) throws IllegalArgumentException;
    List<Commander> getSpectators();
    Log getLog();
    List<GameResult> getGameResult();
    /** Starts the match, which will eventually set the GameResult to something other than null. */
    void start();

    /** Adds a commander to a list of spectators, who can receive log events and gamestate updates. Returns whether a commander was successfully added. */
    boolean addSpectator(Commander joiningCommander);

    /**
     * Given a commander and matchID, get the current choices available for that commander to make.
     * Throws IllegalArgumentException when the matchID doesn't match to a commander.
     */
    List<GameChoice> getNextEffect(long commanderID);

    /**
     * Given a commander and matchID, get the current choices available for that commander to make.
     * Throws IllegalArgumentException when the matchID doesn't match to a commander.
     */
    List<List<GameChoiceChange>> getNextChange(long commanderID);

    /**
     * Given a commander and matchID, get the current numbers available for that commander to choose.
     * Throws IllegalArgumentException when the matchID doesn't match to a commander.
     */
    List<Integer> getNextNumber(long commanderID);

    List<String> getLog(long commanderID);

    FullGameStateView getView(long commanderID);
}
