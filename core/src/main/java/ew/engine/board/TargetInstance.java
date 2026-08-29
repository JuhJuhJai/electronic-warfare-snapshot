package ew.engine.board;

/** Class used to store a card's Target for later use. */
public class TargetInstance {
    Position target;
    int knownTargetID;

    public Position getTarget() { return target; }
    public int getKnownTargetID() { return knownTargetID; }

    public void setTarget(Position newTarget) { this.target = newTarget; }
    public void setKnownTargetID(int newID) { this.knownTargetID = newID; }

    /** Return if check is a target. Checks this class's knownTargetID and the check's BoardLocation, not lane and file position. */
    public boolean isOffFieldTarget(LivingObject check) {
        return knownTargetID == check.getInstanceID() && target.getBoardLocation() == check.getPosition().getBoardLocation();
    }

    /** Return if check is a target, including its Lane, File, Board Location, and CardField State. Does NOT check instanceID. */
    public boolean isOnFieldTarget(LivingObject check) { return target == check.getPosition(); }

    public TargetInstance(Position target, int knownTargetID) {
        this.target = target;
        this.knownTargetID = knownTargetID;
    }

    public TargetInstance(Position target) {
        if (target.getBoardLocation() != BoardLocation.FIELD) throw new IllegalArgumentException("Position of Target not on field.");
        this.target = target;
        this.knownTargetID = 0;
    }
}
