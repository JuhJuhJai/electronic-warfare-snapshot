package ew.server;

import ew.engine.resolver.*;
import ew.playerData.Commander;

import java.security.SecureRandom;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

public interface Room {
    public String toString();
    public long getRoomID();
    public long getSeed();
    public ControllerInstance getController1();
    public ControllerInstance getController2();
    public ControllerInstance getController(long ID) throws IllegalArgumentException;
    public List<Commander> getSpectators();
    public Log getLog();
    /** Returns null if the game isn't finished. */
    public GameResult getGameResult();
    public void setResult(GameResult result);

    /** Adds a commander to a list of spectators, who can receive log events and gamestate updates. Returns whether a commander was successfully added. */
    public boolean addSpectator(Commander joiningCommander);

    /**
     * Given a commander and matchID, get the current choices available for that commander to make.
     * Throws IllegalArgumentException when the matchID doesn't match to a commander.
     */
    public List<GameChoice> getNextEffect(long commanderID);

    /**
     * Given a commander and matchID, get the current choices available for that commander to make.
     * Throws IllegalArgumentException when the matchID doesn't match to a commander.
     */
    public List<List<GameChoiceChange>> getNextChange(long commanderID);

    public List<Integer> getNextNumber(long commanderID);

    public List<String> getLog(long commanderID);
}
