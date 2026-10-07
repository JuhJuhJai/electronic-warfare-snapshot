package ew.engine.board;

import java.util.Objects;

/**
 * Absolute board coordinate. Both axes are 1..5.
 *
 * <p>{@code lane} is the vertical column (1..5). {@code file} is the horizontal row (1..5),
 * stored in a fixed orientation: files 1-2 are player ONE's side, file 3 is the neutral
 * middle, files 4-5 are player TWO's side. "Forward", "left", etc. are perspective-relative
 * and resolved per commander via {@link Field#step}.
 */
public class Position {
    private final BoardLocation location;
    private final CardRideState fieldState;
    private final int lane;
    private final int file;

    public int getLane() { return lane; }
    /** The same as lane. */
    public int getPilePos() { return lane; }
    public int getFile() { return file; }
    public BoardLocation getBoardLocation() { return location; }
    public CardRideState getRideState() { return fieldState; }

    public String toString() { return "Location: " + location + ", " + fieldState + " (" + lane + ", " + file + ")"; }

    public int hashCode() {
        return Objects.hash(location, fieldState, lane, file);
    }

    public boolean equals(Object o) {
        if (!(o instanceof Position other)) return false;
        return other.getBoardLocation() == location
            && other.getLane() == lane
            && other.getFile() == file
            && switch(other.getRideState()) {
            case NORMAL -> this.fieldState == CardRideState.NORMAL || this.fieldState == CardRideState.RIDER || this.fieldState == CardRideState.RODE;
            case RIDER -> this.fieldState == CardRideState.NORMAL || this.fieldState == CardRideState.RIDER;
            case RODE -> this.fieldState == CardRideState.NORMAL || this.fieldState == CardRideState.RODE;
        };
    }

    /**
     * Returns true if the check position is orthogonally adjacent to this position
     * or the check position is on the same zone as this position, but has a different FieldState.
     */
    public boolean isAdjacent(Position check) {
        return this.location == BoardLocation.FIELD
            && (
                // The position is adjacent
                (((this.lane == check.getLane() - 1) || (this.lane == check.getLane() + 1))
                    && ((this.file == check.getFile() - 1) || (this.file == check.getFile() + 1)))
                    // The position is on the same zone, but with a different RideState
                || (this.lane == check.getLane()
                    && this.file == check.getFile()
                    && check.getRideState() != CardRideState.NORMAL
                    && this.fieldState != check.getRideState())
        );
    }

    /** Use for on field positions. Lanes and Files that are out of bounds are set to the proper FieldState. */
    public Position(int lane, int file, CardRideState cardRideState) {
        Position p = resolveFieldPosition(lane, file, cardRideState);
        this.lane = p.lane;
        this.file = p.file;
        this.location = p.location;
        this.fieldState = p.fieldState;
    }

    /** Use for off field positions. */
    public Position(BoardLocation location, CardRideState state, int pilePosition) {
        this.location = location;
        this.fieldState = state;
        this.lane = pilePosition;
        this.file = 0;
    }

    public static Position resolveFieldPosition(Position tryPos) {
        return resolveFieldPosition(tryPos.lane, tryPos.file, tryPos.getRideState());
    }

    public static Position resolveFieldPosition(int tryLane, int tryFile, CardRideState cardRideState) {
        final int lane;
        final int file;
        final CardRideState fieldState;
        final BoardLocation location;
        if (tryLane < 1) {
            lane = 0;
            file = 0;
            fieldState = CardRideState.NORMAL;
            if (tryFile < 3) location = BoardLocation.COMMANDER_ONE;
            else if (tryFile > 3) location = BoardLocation.COMMANDER_TWO;
            else location = BoardLocation.BOARD;
        }
        else if (tryLane > 5) {
            lane = 0;
            file = 0;
            fieldState = CardRideState.NORMAL;
            if (tryFile < 3) location = BoardLocation.COMMANDER_ONE;
            else if (tryFile > 3) location = BoardLocation.COMMANDER_TWO;
            else location = BoardLocation.BOARD;
        }
        else if (tryFile < 1) {
            lane = 0;
            file = 0;
            fieldState = CardRideState.NORMAL;
            location = BoardLocation.COMMANDER_ONE;
        }
        else if (tryFile > 5) {
            lane = 0;
            file = 0;
            fieldState = CardRideState.NORMAL;
            location = BoardLocation.COMMANDER_TWO;
        }
        else {
            lane = tryLane;
            file = tryFile;
            location = BoardLocation.FIELD;
            fieldState = cardRideState;
        }
        return new Position(lane, file, fieldState, location);
    }

    private Position(int lane, int file, CardRideState rideState, BoardLocation location) {
        this.lane = lane;
        this.file = file;
        this.fieldState = rideState;
        this.location = location;
    }

    public static Position COMMANDER_ONE() {
        return new Position(BoardLocation.COMMANDER_ONE, CardRideState.NORMAL, 0);
    }

    public static Position COMMANDER_TWO() {
        return new Position(BoardLocation.COMMANDER_TWO, CardRideState.NORMAL, 0);
    }
}
