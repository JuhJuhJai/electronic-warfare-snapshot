package ew.engine.board;

/**
 * A direction from the controller's perspective (CR3.7.1). Concrete coordinate deltas
 * depend on which commander is looking and are resolved by {@link Field#step}.
 */
public enum Direction {
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT;

    /** Use fromDeclared to resolve this integer back into a direction. */
    public int toDeclared() {
        return this.ordinal();
    }

    public static Direction fromDeclared(int declared) {
        return values()[declared];
    }
}
