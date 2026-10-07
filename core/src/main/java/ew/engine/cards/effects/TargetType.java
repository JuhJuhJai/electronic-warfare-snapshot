package ew.engine.cards.effects;

/**
 * Enum of target params. Names resovled in match.
 * (Card, Commander, Zone, LivingObject) applies to a type of target based on its instance state.
 */
public enum TargetType {
    // These targets are for Pattern Targets, referential to the lane|col position of the card using it.
    // In choosing available targets, these are INCLUSIVE.
    // ...damn I've gotta get more creative with patterns.
    // f = forward, r = right, b = backwards, l = left.
    Zone3f3l, Zone3f2l, Zone3f1l, Zone3f, Zone3f1r, Zone3f2r, Zone3f3r,
    Zone2f3l, Zone2f2l, Zone2f1l, Zone2f, Zone2f1r, Zone2f2r, Zone2f3r,
    Zone1f3l, Zone1f2l, Zone1f1l, Zone1f, Zone1f1r, Zone1f2r, Zone1f3r,
    Zone3l,   Zone2l,   Zone1l,  ZoneSelf, Zone1r,  Zone2r,   Zone3r,
    Zone1b3l, Zone1b2l, Zone1b1l, Zone1b, Zone1b1r, Zone1b2r, Zone1b3r,
    Zone2b3l, Zone2b2l, Zone2b1l, Zone2b, Zone2b1r, Zone2b2r, Zone2b3r,
    Zone3b3l, Zone3b2l, Zone3b1l, Zone3b, Zone3b1r, Zone3b2r, Zone3b3r,

    // A Target that mentions Target is used when a TargetField or TargetOffField is explicitly stated before
    Target,
    ExceptUsersTargets,
    OccupyingTargetsZone,

    // Declared types also are used when a Declaration is made beforehand.
    DeclaredName,
    DeclaredAttribute,
    DeclaredPile,
    InDeclaredDirectionFromSelf, // Keeps objects which are in the

    // Targets used when target isn't explicitly stated
    Self,
    NotSelf,

    Commanders,
    OpponentCommander,

    OwningCommander, // Cards the owner put in their deck / tokens they created
    ControllingCommander, // Cards that are on the field
    UsingCommander, // The Commander that pays the cost when the card isn't on the field (before being placed, activated in hand, etc.)

    Gamestate, // Used singularly, for something not owned by anything except the match (the current phase, for example)

    ZonesOnly,
    // For effects other than the above, zone targets include the cards in the zones.
    FieldZones,
    OccupiedZones,
    OccupyingThisZone, // Targets every card with the same onField position as this card
    UnoccupiedZones,
    ZonesOnYourSide,
    ZonesOnYourOpponentsSide,
    OpenFieldZones,
    AttackZones,
    FrontlineZones,
    BacklineZones,
    ProductionZones,
    CloseProductionZones,
    FarProductionZones,

    ZonesAdjacentToThis,
    ZonesAdjacentToTarget,
    ZonesInThisLane,
    ZonesInThisFile,
    ZonesInTargetsLane,
    ZonesInTargetsFile,
    ZonesYouControl,
    ZonesYouDontControl,

    ZonesAdjacentToThisLane, // Excludes the lane "this" is in
    ZonesAdjacentToThisFile, // Excludes the file "this" is in

    // Card targets
    Cards,
    CardsExceptThisCard,

    CardsYouOwn,
    CardsYouDontOwn,
    CardsYouControl,
    CardsYouDontControl,
    CardsYouUse,
    CardsYouDontUse,

    AttackCards,
    ProductionCards,
    DecisiveCards,
    TokenCards,

    HumanCards,	ExceptHumanCards,
    MachineCards, ExceptMachineCards,
    RockCards,	ExceptRockCards,
    PlantCards,	ExceptPlantCards,
    WindCards,	ExceptWindCards,
    WaterCards,	ExceptWaterCards,
    FlameCards,	ExceptFlameCards,
    BeastCards,	ExceptBeastCards,
    DivineCards, ExceptDivineCards,
    GhoulCards,	ExceptGhoulCards,
    EtherCards,	ExceptEtherCards,
    RitualCards, ExceptRitualCards,

    FaceUpCards,
    FaceDownCards,
    ShroudableCards,
    ShroudedCards,
    CardsShroudedThisTurn,
    CardsNotShroudedThisTurn,
    RevealedCards,
    UnrevealedCards,
    DestroyedCards, // refers to destroyed cards on the field, their isDestroyed state.
    NotDestroyedCards, // refers to cards on the field that aren't destroyed
    RidingCards,
    RodeCards,

    CardsOnTheField,
    CardsInHand,
    CardsInDeck,
    CardsInDecisiveDeck,
    CardsInDestroyedPile,
    CardsInDiscardPile,
    CardsInDisplacedPile,

    CardsNotOnTheField,
    CardsNotInHand,
    CardsNotInDeck,
    CardsNotInDecisiveDeck,
    CardsNotDestroyed,
    CardsNotDiscarded,
    CardsNotDisplaced,

    CardsThatCanMoveForwards,
    CardsThatCanMoveBackwards,
    CardsThatCanMoveLeft,
    CardsThatCanMoveRight,
    CardsThatCanMoveForwardsOrBackwards,
    CardsThatCanMoveLeftOrRight,

    CardsWithSpaceToBePlaced,
    CardsWithSpaceToBePlacedAfterUse,

    // Things that could potentially apply to cards as well as zones
    Revealed,
    NotRevealed,

    // Rideability checks
    RideableCards,
    RideableCardsWithoutARider,
    RideableCardsWhoseRideableCostCanBePaidByUser,
    CardsThatCanRideAnAdjacentCard, // CardsAdjacentToARideableCardWithNoRiderANDWhoseRideableCostCanBePaidByUser
    CardsThatCanRideThisTarget,

    // Counter checks
    WithFortificationCounters, WithFrozenCounters, WithFlameCounters,
    WithInfectionCounters, WithEtherealCounters, WithEchoCounters,
    WithMomentumCounters,

    WithEnergyCounters, WithBountyCounters, WithScrapCounters, WithTideCounters,
    WithRageCounters, WithSturdyCounters, WithSiGNLCounters, WithTravelCounters,
    WithFervorCounters, WithStorageCounters, WithUndeadCounters, WithVirtueCounters,
    WithSinCounters, WithDreamCounters, WithInjectionCounters, WithVeilCounters,
    WithOverchargeCounters, WithCounters,

    // Placement cost checks
    PlacementCostIsMet, // by the user of the checked card

    // For cards that are used by an effect that places cards.
    // Cards that can be used are stored in the target of the card that places cards.
    CurrentlyUsable,

    // PlacementCostCanBeMet excludes the card that would be placed from using itself.
    PlacementCostCanBeMetByUsersFieldCards,
    PlacementCostCanBeMetByUsersCardsInHand, PlacementCostCanBeMetByUsersCardsInHandOrNormally,
    PlacementCostCanBeMetByUsersDestroyedCards,
    PlacementCostCanBeMetByUsersDiscardedCards,
    PlacementCostCanBeMetByUsersDisplacedCards,
    PlacementCostCanBeMetByUsersDeck,
    PlacementCostCanBeMetByUsersDecisivePileCards,
    PlacementCostCanBeMetByTargets,
    PlacementCostCanBeMetByAnyFieldCards,
    PlacementCostCanBeMetByOpponentsFieldCards,
    PlacementCostCanBeMetByUsersZones,

    // Continuous checks, all automatically placed on changes.
    // These all have individual functions that take active continuous in Engine.
    isTargetable,
    isPlaceable,
    isDamageable,
    isRestorable,
    isShroudable,
    isRevealable,
    isUnrevealable,
    isSwappable,
    isMoveable, // has immobile
    isDestroyable,
    isDiscardable,
    isDisplaceable,
    isUnshroudable,
    canDeclare,
    canChoose,
    canGainCounters,
    canLoseCounters,
    canAddToHand,
    lessThan7DreamCountersORcanGainMoreThan7DreamCounter,

    // Commander
    canGainMaterial,
    canDraw; // Is true even with an empty deck. Draw effects instead deal damage.

    public static boolean isPatternTarget(TargetType check) {
        return check.ordinal() <= 48; // This is fine because nothing will ever go behind the 48 targetType.
    }
}
