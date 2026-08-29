package ew.engine.board;

public enum CardRideState {
    NORMAL, // if the card isn't a rider or rode. A target on zone targets all "Zone", "Rider" and "Rode" cards on the zone.
    RIDER,
    RODE,
}
