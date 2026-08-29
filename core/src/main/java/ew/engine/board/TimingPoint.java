package ew.engine.board;

public enum TimingPoint {
    /** Begin Draw phase. */
    D0,
    /** After both commanders draw 1 card, End Draw phase. */
    D1,

    /**
     * Start Build phase
     */
    B0,
    /**
     * After Opportunity Used
     */
    B1,
    /**
     * After Opportunity passed
     */
    B2,
    /**
     * End Build phase
     */
    B3,

    /**
     * Start Combat phase
     */
    C0,
    /**
     * After Lane Selection
     */
    C1,
    /**
     * After Action Selection
     */
    C2,
    /**
     * After Each Action
     */
    C3,
    /**
     * End Combat Phase
     */
    C4,

    /**
     * Start Aftermath phase
     */
    A0,
    /**
     * After Movement
     */
    A1,
    /**
     * After discard down to 6 and commander destroyed deck, End Aftermath phase
     */
    A2,

    // Unshroud windows which occur between effect and cost, per phase.
    UnD,
    UnB,
    UnC,
    UnA
}
