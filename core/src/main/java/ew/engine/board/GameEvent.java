package ew.engine.board;

import java.util.List;

public class GameEvent {
    final ImmutableEffectChangeInstance change;
    final List<Integer> eventInfo; // parsed in string conversion based on EffectChangeType.
    final int turn;
    final TimingPoint timingPoint;
    final boolean player1Vis;
    final boolean player2Vis;
    /* Variables defined by the livingObject target given, as opposed to copying the living object every time. */
    final int targetID;
    final int originalTargetID;
    final String targetName;

    public ImmutableEffectChangeInstance getChange() { return change; }
    public List<Integer> getEventInfo() { return eventInfo; }
    public boolean getPlayer1Vis() { return player1Vis; }
    public boolean getPlayer2Vis() { return player2Vis; }
    public int getTarget() { return targetID; }
    public int getTurn() { return turn; }


    public GameEvent(GameState timingCheck, EffectChangeInstance change, LivingObject target, List<Integer> eventInfo, boolean player1Vis, boolean player2Vis) {
        this.change = new ImmutableEffectChangeInstance(change);
        this.eventInfo = eventInfo;
        this.player1Vis = player1Vis;
        this.player2Vis = player2Vis;
        this.turn = timingCheck.turn;
        this.timingPoint = timingCheck.timingPoint;
        if (target != null) {
            this.targetID = target.getInstanceID();
            this.originalTargetID = target.getOriginalID();
            this.targetName = target.getNameInstance();
        }
        else {
            this.targetID = -1;
            this.originalTargetID = -1;
            this.targetName = "";
        }
    }

    /** Abbreviation for GameEvent with no living object target. */
    public GameEvent(GameState timingCheck, EffectChangeInstance change, List<Integer> eventInfo, boolean player1Vis, boolean player2Vis) {
        this.change = new ImmutableEffectChangeInstance(change);
        this.eventInfo = eventInfo;
        this.player1Vis = player1Vis;
        this.player2Vis = player2Vis;
        this.turn = timingCheck.turn;
        this.timingPoint = timingCheck.timingPoint;
        /* No living object target. */
        this.targetID = 0;
        this.originalTargetID = 0;
        this.targetName = "";
    }

    /** Abbreviation for system changes. */
    public GameEvent(GameState timingCheck, EffectChangeInstance systemChange) {
        this.change = new ImmutableEffectChangeInstance(systemChange);
        this.eventInfo = null;
        this.player1Vis = true;
        this.player2Vis = true;
        this.turn = timingCheck.turn;
        this.timingPoint = timingCheck.timingPoint;
        /* No living object target. */
        this.targetID = 0;
        this.originalTargetID = 0;
        this.targetName = "";
    }

    /** Abbreviation for system changes. */
    public GameEvent(GameState timingCheck, EffectChangeInstance systemChange, List<Integer> eventInfo) {
        this.change = new ImmutableEffectChangeInstance(systemChange);
        this.eventInfo = eventInfo;
        this.player1Vis = true;
        this.player2Vis = true;
        this.turn = timingCheck.turn;
        this.timingPoint = timingCheck.timingPoint;
        /* No living object target. */
        this.targetID = 0;
        this.originalTargetID = 0;
        this.targetName = "";
    }

    // If the owner of the effectChangeInstance is null, then the gamestate did the change.
    /* Incomplete. Must change format based on the type of the change. */
    /** Returns the formatted information from this change to a player that has full information from the change. */
    public String toVisibleString() {
        return targetID <= 0 ? // if it targets the gamestate or doesn't have a target
            change.getTypeInstance().toString()
            : change.getTypeInstance() + " targeting " + targetName + " (" + targetID + ") with strength " + eventInfo.get(0);
    }

    /* Incomplete. Must know whether to return an empty String or to return whether "something happened you have less information on"
    (e.g. knows opponent drew a card, doesn't know what the card is. Opponent would know what the card is.) */
    /** Returns the formatter information from this change to a player that has the minimal information from the change. May be empty. */
    public String toHiddenString() {
        return "Hidden effect from " + change.getUser();
    }
}
