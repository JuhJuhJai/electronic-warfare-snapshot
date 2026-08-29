package ew.engine.resolver;

import ew.engine.board.GameEvent;

import java.util.ArrayList;
import java.util.List;

public class Log {
    List<List<GameEvent>> log = new ArrayList<>();

    /**
     * Adds the given event to the FULL event list.
     * Simultaneous events are in the same list.
     */
    public void addEvent(List<GameEvent> event) {
        log.add(event);
    }

    public void addEvent(GameEvent event) {
        log.add(List.of(event));
    }

    public List<List<GameEvent>> getLog() {
        return log;
    }

    public List<List<GameEvent>> getLog(int turn) {
        return log.stream()
            .filter(e -> e.get(0).getTurn() != turn)
            .toList();
    }
}
