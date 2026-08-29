package ew.engine.cards.effects;

/**
 * Lists effect conditions that are able to be checked on a card.
 */
public enum ConditionType {
    MinMaterial,
    MinHealth,
    MinPrecision,

    AvailableAttackPlacementSpace,
    AvailableProductionPlacementSpace,

    MinThings,
    MaxThings,

    MinCounters,
    MinFortificationCounters, MinFrozenCounters, MinFlameCounters,
    MinInfectionCounters, MinEtherealCounters, MinEchoCounters,
    MinMomentumCounters, MaxMomentumCounters,

    MinEnergyCounters, MinBountyCounters, MinScrapCounters, MinTideCounters,
    MinRageCounters, MinSturdyCounters, MinSiGNLCounters, MinTravelCounters,
    MinFervorCounters, MinStorageCounters, MinUndeadCounters, MinVirtueCounters,
    MinSinCounters, MinDreamCounters, MinInjectionCounters, MinVeilCounters,
    MinOverchargeCounters,

    MaxEffectPerTurn,
    IfLastChangeResolved, // Only use as a
    Exclusive,

    AbleToRideCard,

    // Trigger Conditions, checks the "Just Happened".
    DrawPhaseBegins,
    DrawPhaseEnds,
    BuildPhaseBegins,
    BuildPhaseEnds,
    CombatPhaseBegins,
    CombatPhaseEnds,
    AftermathPhaseBegins,
    AftermathPhaseEnds,
    /** Must be the last condition for any effect. */
    Strategic, // The player chose to use it.

    PhaseBegins,
    PhaseEnds,

    EffectEnds,

    CardDestroyed,
    CardDiscarded,
    CardDisplaced,

    CardDestroyedOrDiscarded,
    CardDestroyedExceptByAttack,
    CardDestroyedExceptByAttackOrDiscarded,

    CardPlaced,
    CardUnshrouded,
    CardActivated,
    CardTriggered,
    CardActed,

    CardRestored,
    CardDamaged,

    // Below are conditions which are used by cards that must be placed by an effect
    // Checks for a combination of material available that equals the strength.
    MaterialAvailable,
    CountersAvailable,
    HealthAvailable,

    // Only for EffectDuration. Always returns false.
    NONE,

    Choice, // The strength determines which value of choice is needed.
    /** Only use optional for effectChange Conditions. */
    Optional;

    /** Returns true if the condition is used for cards that must use cards to place. */
    public static boolean isConditionForUse(ConditionType check) {
        return switch(check) {
            case MaterialAvailable, CountersAvailable, HealthAvailable -> true;
            default -> false;
        };
    }
}
