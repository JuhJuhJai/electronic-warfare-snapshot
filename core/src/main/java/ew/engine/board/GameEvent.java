package ew.engine.board;

import java.util.List;

public class GameEvent {
    final EffectChangeInstance change;
    final List<Integer> eventInfo; // parsed in string conversion based on type.
    final int turn;
    final TimingPoint timingPoint;
    final boolean player1Vis;
    final boolean player2Vis;
    // Variables defined by the livingObject target given
    final int targetID;
    final String targetName;

    public EffectChangeInstance getChange() { return change; }
    public List<Integer> getEventInfo() { return eventInfo; }
    public boolean getPlayer1Vis() { return player1Vis; }
    public boolean getPlayer2Vis() { return player2Vis; }
    public int getTarget() { return targetID; }
    public int getTurn() { return turn; }


    public GameEvent(GameState timingCheck, EffectChangeInstance change, LivingObject target, List<Integer> eventInfo, boolean player1Vis, boolean player2Vis) {
        this.change = new EffectChangeInstance(change);
        this.eventInfo = eventInfo;
        this.player1Vis = player1Vis;
        this.player2Vis = player2Vis;
        this.turn = timingCheck.getTurn();
        this.timingPoint = timingCheck.getTimingPoint();
        if (target != null) {
            this.targetID = target.getInstanceID();
            this.targetName = target.getNameInstance();
        }
        else {
            this.targetID = -1;
            this.targetName = "";
        }
    }

    /** Abbreviation for system changes. */
    public GameEvent(GameState timingCheck, EffectChangeInstance systemChange) {
        this.change = new EffectChangeInstance(systemChange);
        this.eventInfo = null;
        this.player1Vis = true;
        this.player2Vis = true;
        this.turn = timingCheck.getTurn();
        this.timingPoint = timingCheck.getTimingPoint();
        this.targetID = 0;
        this.targetName = "";
    }

    /** Abbreviation for system changes. */
    public GameEvent(GameState timingCheck, EffectChangeInstance systemChange, List<Integer> eventInfo) {
        this.change = new EffectChangeInstance(systemChange);
        this.eventInfo = eventInfo;
        this.player1Vis = true;
        this.player2Vis = true;
        this.turn = timingCheck.getTurn();
        this.timingPoint = timingCheck.getTimingPoint();
        this.targetID = 0;
        this.targetName = "";
    }

    // If the owner of the effectChangeInstance is null, then the gamestate did the change.
    /** Returns the formatted information from this change to a player that has full information from the change. */
    public String toVisibleString() {
        return targetID <= 0 ? // if it targets the gamestate or doesn't have a target
            change.getTypeInstance().toString()
            : change.getTypeInstance() + " targeting " + targetName + " (" + targetID + ") with strength " + eventInfo.get(0);
    }
    /** Returns the formatter information from this change to a player that has the minimal information from the change. May be empty. */
    public String toHiddenString() {
        return "Hidden effect from " + change.getUser();
    }
}
