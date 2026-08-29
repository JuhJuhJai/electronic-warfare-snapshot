package ew.engine.board;

import ew.engine.cards.CardType;

/**
 * The six kinds of zone (CR3.5). Whether a zone accepts a card depends on the card's type:
 * production cards go on production zones, attack cards on attack zones. OPEN_FIELD is part
 * of the field (cards may move there) but accepts no placements; EMPTY_SPACE is not field
 * at all.
 */
public enum ZoneType {
    FAR_PRODUCTION(true, false),
    CLOSE_PRODUCTION(true, false),
    BACKLINE(false, true),
    FRONTLINE(false, true),
    OPEN_FIELD(false, false),
    EMPTY_SPACE(false, false);

    private final boolean productionZone;
    private final boolean attackZone;

    ZoneType(boolean productionZone, boolean attackZone) {
        this.productionZone = productionZone;
        this.attackZone = attackZone;
    }

    public boolean isProductionZone() { return productionZone; }
    public boolean isAttackZone()     { return attackZone; }

    /** OPEN_FIELD counts as field; EMPTY_SPACE does not (CR3.1.2, CR3.5.1f). */
    public boolean isField() { return this != EMPTY_SPACE; }

    /** Returns true if the card type can be placed on the zone (doesn't check continuous) */
    public static boolean cardTypeForPlacement(CardType check, ZoneInstance zone) {
        return switch(check) {
            case ATTACK, DECISIVEATTACK, TOKENATTACK ->
                (zone.getType() == ZoneType.FRONTLINE || zone.getType() == ZoneType.BACKLINE || zone.getType() == ZoneType.OPEN_FIELD);
            case PRODUCTION, DECISIVEPRODUCTION, TOKENPRODUCTION ->
                (zone.getType() == ZoneType.CLOSE_PRODUCTION || zone.getType() == ZoneType.FAR_PRODUCTION);
            default -> false;
        };
    }

/** Returns true if the card type can move onto the zone (doesn't check continuous) */
    public static boolean cardTypeForMovement(CardType check, ZoneInstance zone) {
        return switch (check) {
            case ATTACK, DECISIVEATTACK, TOKENATTACK ->
                (zone.getType() == ZoneType.FRONTLINE || zone.getType() == ZoneType.BACKLINE || zone.getType() == ZoneType.OPEN_FIELD);
            case PRODUCTION, DECISIVEPRODUCTION, TOKENPRODUCTION ->
                (zone.getType() == ZoneType.CLOSE_PRODUCTION || zone.getType() == ZoneType.FAR_PRODUCTION
                    || zone.getType() == ZoneType.FRONTLINE || zone.getType() == ZoneType.BACKLINE
                    || zone.getType() == ZoneType.OPEN_FIELD);
            default -> false;
        };
    }
}
