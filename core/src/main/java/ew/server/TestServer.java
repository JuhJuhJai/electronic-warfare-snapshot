package ew.server;

import ew.engine.resolver.MatchFormat;
import ew.playerData.Commander;

public class TestServer extends Server {
    final Commander spectator;

    public TestServer(Commander spectator) {
        Commander commander1 = Commander.random(1, random);
        Commander commander2 = Commander.random(1, random);
        commanderList.put(1L, commander1);
        commanderList.put(2L, commander2);
        this.spectator = spectator;
        joinGame(commander1);
        joinGame(commander2);
    }

    public long joinGame(Commander joiningCommander) {
        if (waitingRoom.contains(joiningCommander)) throw new IllegalStateException("Commander " + joiningCommander + " is already in waiting list.");
        waitingRoom.add(joiningCommander);
        for (Commander commander : waitingRoom) {
            if (joiningCommander.getPlayerID() != commander.getPlayerID()) {
                TestRoom testRoom = new TestRoom(MatchFormat.STANDARD, nextMatchID, random.nextInt(), joiningCommander, commander);
                roomList.put(nextMatchID, testRoom);
                testRoom.addSpectator(spectator);
                waitingRoom.remove(joiningCommander);
                waitingRoom.remove(commander);
                return nextMatchID++;
            }
        }
        return -1;
    }

    public long createCommander(String name) {
        return -1;
    }
}
