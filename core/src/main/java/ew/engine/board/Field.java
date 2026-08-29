package ew.engine.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The 5x5 field of zones, stored in fixed absolute coordinates. Perspective
 * (forward/back/left/right, "your side") is computed per commander rather than baked in.
 */
public final class Field {

    // 1-indexed for readability; row/col 0 are unused. Indexed [lane][file].
    private final ZoneInstance[][] zoneInstances = new ZoneInstance[6][6];

    public Field(AtomicInteger nextLivingIDs) {
        for (int lane = 1; lane <= 5; lane++) {
            for (int file = 1; file <= 5; file++) {
                zoneInstances[lane][file] = new ZoneInstance(
                        new Position(lane, file, CardRideState.NORMAL),
                        typeFor(lane, file),
                        sideFor(file),
                        nextLivingIDs.getAndIncrement());
            }
        }
    }

    /** Helper function to set up board. */
    private static boolean economyLane(int lane) {
        return lane == 1 || lane == 5;
    }

    /** Helper Function to set up board. Zone type as a function of coordinates (CR3.3.2a/b). */
    private static ZoneType typeFor(int lane, int file) {
        if (economyLane(lane)) {
            return switch (file) {
                case 1, 5 -> ZoneType.FAR_PRODUCTION;
                case 2, 4 -> ZoneType.CLOSE_PRODUCTION;
                case 3    -> ZoneType.EMPTY_SPACE;
                default   -> throw new IllegalStateException();
            };
        }
        else return switch (file) {
            case 1, 5 -> ZoneType.BACKLINE;
            case 2, 4 -> ZoneType.FRONTLINE;
            case 3    -> ZoneType.OPEN_FIELD;
            default   -> throw new IllegalStateException();
        };
    }

    /** Helper Function to set up board. Files 1-2 are player ONE's side, 4-5 are player TWO's, 3 is neutral. */
    private static SideID sideFor(int file) {
        if (file <= 2) return SideID.ONE;
        if (file >= 4) return SideID.TWO;
        return null;
    }

    /** Returns every zone on the field */
    public List<ZoneInstance> getZones() { return Arrays.stream(zoneInstances).flatMap(Arrays::stream).toList(); }
    /** Returns the zone of a given position */
    public ZoneInstance getZoneAt(Position p) { return zoneInstances[p.getLane()][p.getFile()]; }
    /** Returns the zone from a given lane and file */
    public ZoneInstance getZoneAt(int lane, int file) { return zoneInstances[lane][file]; }

    /** Returns a List of zones in a lane */
    public List<ZoneInstance> getLane(int lane) {
        List<ZoneInstance> out = new ArrayList<>(5);
        for (int file = 1; file <= 5; file++)  if (zoneInstances[lane][file].getType().isField()) out.add(zoneInstances[lane][file]);
        return out;
    }

    /** Returns a List of zones in a file */
    public List<ZoneInstance> getFile(int file) {
        List<ZoneInstance> out = new ArrayList<>(5);
        for (int lane = 1; lane <= 5; lane++) if (zoneInstances[lane][file].getType().isField()) out.add(zoneInstances[lane][file]);
        return out;
    }

    /** Returns a List of every zone on the field */
    public List<ZoneInstance> getBoard() {
        List<ZoneInstance> out = new ArrayList<>(23);
        for (int lane = 1; lane <= 5; lane++) {
            for (int file = 1; file <= 5; file++) {
                if (zoneInstances[lane][file].getType().isField()) out.add(zoneInstances[lane][file]);
            }
        }
        return out;
    }

    /**
     * Removes a card from its old position on field (if available), then adds it to the given position.
     * Checks for a valid zone on the field, does NOT check for occupancy.
     * Returns true if the original card was removed from the field, otherwise returns false.
     */
    public boolean moveCard(CardInstance card, Position newPosition) {
        if (newPosition.getBoardLocation() != BoardLocation.FIELD
        || newPosition.getLane() < 1 || newPosition.getLane() > 5
        || newPosition.getFile() < 1 || newPosition.getFile() > 5
        || (newPosition.getFile() == 3 && (newPosition.getLane() == 1 || newPosition.getLane() == 5)))
            throw new IllegalArgumentException("Invalid zone for field position.");

        boolean wasRemoved = true;
        try { getZoneAt(card.getPosition()).removeCard(card.getInstanceID()); }
        catch (IllegalStateException e) {
            return false;
        }

        getZoneAt(newPosition).addCard(card);
        card.setPosition(newPosition);
        return wasRemoved;
    }

    /**
     * Removes a card from wherever it is on the field.
     * Throws an Illegal State Exception if the card isn't on the field.
     */
    public void removeCard(int cardID) throws IllegalStateException {
        boolean removed = false;
        for (ZoneInstance zone : getBoard()) {
            removed = zone.getCards().removeIf(candidate -> candidate.getInstanceID() == cardID);
        }
        if (!removed) {
            throw new IllegalStateException("Card to remove not found.");
        }
    }

    /** Returns a List of every card on the field, without positions */
    public List<CardInstance> getCards() {
        List<CardInstance> cards = new ArrayList<CardInstance>();
        for (ZoneInstance zoneInstance : this.getBoard()) {
            cards.addAll(zoneInstance.getCards());
        }
        return cards;
    }

    /** Returns a List of every zone on a player's side */
    public List<ZoneInstance> getSide(SideID player) {
        List<ZoneInstance> out = new ArrayList<ZoneInstance>(10);
        if (player == SideID.ONE) {
            for (int file = 1; file <= 2; file++) {
                for (int lane = 1; lane <= 5; lane++) {
                    out.add(zoneInstances[lane][file]);
                }
            }
        }
        else {
            for (int file = 4; file <= 5; file++) {
                for (int lane = 1; lane <= 5; lane++) {
                    out.add(zoneInstances[lane][file]);
                }
            }
        }
        return out;
    }

    /** Returns a List of every attack zone on a player's side */
    public List<ZoneInstance> playerAttackZones(SideID player) {
        List<ZoneInstance> out = new ArrayList<ZoneInstance>(6);
        if (player == SideID.ONE) {
            for (int file = 1; file <= 2; file++) {
                for (int lane = 2; lane <= 4; lane++) {
                    out.add(zoneInstances[lane][file]);
                }
            }
        }
        else {
            for (int file = 4; file <= 5; file++) {
                for (int lane = 2; lane <= 4; lane++) {
                    out.add(zoneInstances[lane][file]);
                }
            }
        }
        return out;
    }

    /** Returns a list of every production zone on a player's side */
    public List<ZoneInstance> playerProductionZones(SideID player) {
        List<ZoneInstance> out = new ArrayList<ZoneInstance>(6);
        if (player == SideID.ONE) {
            for (int file = 1; file <= 2; file++) {
                for (int lane = 1; lane <= 5; lane += 4) {
                    out.add(zoneInstances[lane][file]);
                }
            }
        }
        else {
            for (int file = 4; file <= 5; file++) {
                for (int lane = 1; lane <= 5; lane += 4) {
                    out.add(zoneInstances[lane][file]);
                }
            }
        }
        return out;
    }

    /** Returns a list of every Open Field Zone */
    public List<ZoneInstance> openFieldZone() {
        return new ArrayList<ZoneInstance>(List.of(zoneInstances[2][3], zoneInstances[3][3], zoneInstances[4][3]));
    }

    /**
     * One step in a direction, from {@code viewer}'s perspective. Returns null if the step
     * would leave the board.
     *
     * <p>Convention used: for player ONE, FORWARD increases file and
     * RIGHT increases lane; player TWO is mirrored on both axes.
     */
    public static Position step(SideID viewer, Position from, Direction dir) {
        boolean one = viewer == SideID.ONE;
        int dLane = 0, dFile = 0;
        switch (dir) {
            case FORWARD  -> dFile = one ? +1 : -1;
            case BACKWARD -> dFile = one ? -1 : +1;
            case LEFT     -> dLane = one ? -1 : +1;
            case RIGHT    -> dLane = one ? +1 : -1;
        }
        int lane = from.getLane() + dLane;
        int file = from.getFile() + dFile;
        return Position.resolveFieldPosition(lane, file, CardRideState.NORMAL);
    }
}
