package ew.server;

import ew.engine.board.GameStateView;
import ew.engine.board.SideID;
import ew.engine.resolver.*;
import ew.playerData.Commander;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

public final class TestRoom implements Room {

    final long roomID;
    final long seed;
    final Engine engine;
    final Log log = new Log();
    final MatchFormat format;
    boolean gameGoing = false;
    List<GameResult> result = null;

    final ControllerInstance controller1;
    final ControllerInstance controller2;
    final List<Commander> spectators = new ArrayList<>();

    public String toString() { return format + " test room."; }

    public TestRoom(MatchFormat format, long roomID, int seed, Commander one, Commander two) {
        this.format = format;
        this.roomID = roomID;
        this.seed = seed;
        RandomGenerator random = new SecureRandom(SecureRandom.getSeed(seed));
        if (random.nextInt(1, 2) == 1) {
            controller1 = new RandomControllerInstance(this, one, random);
            controller2 = new RandomControllerInstance(this, two, random);
        }
        else {
            controller1 = new RandomControllerInstance(this, two, random);
            controller2 = new RandomControllerInstance(this, one, random);
        }
        this.engine = new Engine(this, roomID, random,
            one, one.getSavedDecks().get(one.getSelectedDeck()),
            two, two.getSavedDecks().get(two.getSelectedDeck()));
    }

    public void start() {
        gameGoing = true;
        result = engine.start();
        gameGoing = false;
    }
    public boolean isGameGoing() { return gameGoing; }
    public long getRoomID() { return roomID; }
    public long getSeed() { return seed; }
    public ControllerInstance getController1() { return controller1; }
    public ControllerInstance getController2() { return controller2; }
    public ControllerInstance getController(long ID) throws IllegalArgumentException {
        if (controller1.getCommander().getPlayerID() == ID) return controller1;
        if (controller2.getCommander().getPlayerID() == ID) return controller2;
        throw new IllegalArgumentException("No available controller in " + this + " has an ID of " + ID);
    }
    public List<Commander> getSpectators()     { return spectators; }
    public Log getLog()                        { return log; }
    /** Returns null if the game isn't finished. */
    public List<GameResult> getGameResult() { return result; }

    /** Adds a commander to a list of spectators, who can receive log events and gamestate updates. Returns whether a commander was successfully added. */
    public boolean addSpectator(Commander joiningCommander) {
        if (Stream.of(controller1, controller2).map(ControllerInstance::getCommander).anyMatch(c -> c == joiningCommander)) return false;
        spectators.add(joiningCommander);
        return true;
    }

    /**
     * Given a commander and matchID, get the current choices available for that commander to make.
     * Throws IllegalArgumentException when the matchID doesn't match to a commander.
     * Returns null if no effect is there.
     */
    public List<GameChoice> getNextEffect(long commanderID) {
        if (controller1.getCommander().getPlayerID() == commanderID) return controller1.getNextChoice();
        if (controller2.getCommander().getPlayerID() == commanderID) return controller2.getNextChoice();
        throw new IllegalArgumentException(commanderID + " isn't a controller in " + this);
    }

    /**
     * Given a commander and matchID, get the current choices available for that commander to make.
     * Throws IllegalArgumentException when the matchID doesn't match to a commander.
     * Returns null if no change is there.
     */
    public List<List<GameChoiceChange>> getNextChange(long commanderID) {
        if (controller1.getCommander().getPlayerID() == commanderID) return controller1.getNextChange();
        if (controller2.getCommander().getPlayerID() == commanderID) return controller2.getNextChange();
        throw new IllegalArgumentException(commanderID + " isn't a controller in " + this);
    }

    /**
     * Given a commander and matchID, get the current numbers available for that commander to choose.
     * Throws IllegalArgumentException when the matchID doesn't match to a commander.
     * Returns null if no number is there.
     */
    public List<Integer> getNextNumber(long commanderID) {
        if (controller1.getCommander().getPlayerID() == commanderID) return controller1.getNextNumber();
        if (controller2.getCommander().getPlayerID() == commanderID) return controller2.getNextNumber();
        throw new IllegalArgumentException(commanderID + " isn't a controller in " + this);
    }

    public List<String> getLog(long commanderID) {
        if (controller1.getCommander().getPlayerID() == commanderID) return log.getLog().stream()
            .flatMap(List::stream)
            .map(g -> {
                if (g.getPlayer1Vis()) return g.toVisibleString();
                else return g.toHiddenString();
            })
            .toList();
        if (controller2.getCommander().getPlayerID() == commanderID) return log.getLog().stream()
            .flatMap(List::stream)
            .map(g -> {
                if (g.getPlayer1Vis()) return g.toVisibleString();
                else return g.toHiddenString();
            })
            .toList();
        if (spectators.stream().map(Commander::getPlayerID).anyMatch(c -> c == commanderID)) {
            return log.getLog().stream()
                .flatMap(List::stream)
                .map(g -> {
                    if (g.getPlayer1Vis() || g.getPlayer2Vis()) return g.toVisibleString();
                    return g.toHiddenString();
                })
                .toList();
        }
        throw new IllegalArgumentException("Given commanderID " + commanderID + " isn't in this room.");
    }

    public GameStateView getView(long commanderID) {
        if (controller1.getCommander().getPlayerID() == commanderID) return engine.getGameView(SideID.ONE);
        if (controller2.getCommander().getPlayerID() == commanderID) return engine.getGameView(SideID.TWO);
        if (spectators.stream().map(Commander::getPlayerID).anyMatch(c -> c == commanderID))
            return engine.getGameView(SideID.NEUTRAL);
        throw new IllegalArgumentException("Given commanderID (" + commanderID + ") isn't in this room.");
    }
}
