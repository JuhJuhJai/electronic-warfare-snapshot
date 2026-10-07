package ew.server;

import ew.engine.resolver.MatchFormat;
import ew.playerData.Commander;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    final int MAX_WAITING_TIME = 20; // Seconds
    final Map<Long, Commander> commanderList = new HashMap<>();
    final Map<Long, Room> roomList = new HashMap<>();
    final Queue<Commander> waitingRoom = new ConcurrentLinkedQueue<>(); // Implement elo later
    final SecureRandom random = new SecureRandom();
    long nextCommanderID = 1;
    long nextMatchID = 1;

    public Server() {
        commanderList.put(0L, new Commander(0L, "Admin"));
    }

    /**
     * Returns a List(GameChoice), a List(List(GameChoiceChange)), List(Integer), or null,
     * which denotes whether there's a change waiting for the given commanderID
     * in the given roomID.
     */
    public List<Object> getNextChange(long roomID, long commanderID) throws IllegalArgumentException {
        final ControllerInstance controller = roomList.get(roomID).getController(commanderID);
        // Pretend these return pure information rather than java objects. I'll make a function later.
        // For now, the client checks instanceof.
        if (controller.getNextChoice() != null) return Collections.singletonList(controller.getNextChoice());
        if (controller.getNextChange() != null) return Collections.singletonList(controller.getNextChange());
        if (controller.getNextNumber() != null) return Collections.singletonList(controller.getNextNumber());
        return null;
    }

    /**
     * Sets the given room's controllerInstance connected to the commanderID's choice value
     * with the given List(Integer).
     */
    public void submitChoice(long roomID, long commanderID, List<Integer> choice) throws IllegalArgumentException {
        roomList.get(roomID).getController(commanderID).setChoice(choice);
    }

    public Commander getCommander(long commanderID) {
        return commanderList.get(commanderID);
    }

    /** Returns the ID of the made commander. */
    public long createCommander(String name) {
        commanderList.put(nextCommanderID, new Commander(nextCommanderID, name));
        return nextCommanderID++;
    }

    /** Returns the ID of a room the commander has joined - 0 if no room was joined within the MAX_WAITING_TIME. */
    public long joinGame(Commander joiningCommander) throws InterruptedException, IllegalStateException {
        if (waitingRoom.contains(joiningCommander)) throw new IllegalStateException("Commander " + joiningCommander + " is already in waiting list.");

        waitingRoom.add(joiningCommander);

        double waitingTime = 0;
        while (waitingTime < MAX_WAITING_TIME) {
            Thread.sleep(20);
            waitingTime += 0.02;
            for (Commander commander : waitingRoom) {
                if (joiningCommander.getPlayerID() != commander.getPlayerID()) {
                    roomList.put(nextMatchID, new PlayerRoom(MatchFormat.STANDARD, nextMatchID, random.nextInt(), joiningCommander, commander));
                    waitingRoom.remove(joiningCommander);
                    waitingRoom.remove(commander);
                    return nextMatchID++;
                }
                else if (!waitingRoom.contains(joiningCommander)) {
                    for (Room room : roomList.values()) {
                        try {
                            room.getController(joiningCommander.getPlayerID());
                            return room.getRoomID();
                        } catch (IllegalArgumentException ignored) {

                        }
                    }
                    return 0;
                }
            }
        }

        waitingRoom.remove(joiningCommander);
        return 0;
    }
}
