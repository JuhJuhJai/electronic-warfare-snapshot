
package ew.engine.cards.effects;

import java.util.Arrays;
import java.util.List;

/**
 * <p>Every possible effect change that can affect the GameState.
 *
 * <p>Resolution order (CR13.5.3, as extended for this engine):
 * 1 Pay, 2 Target, 3 Declare/Choose, 4 Reveal, 5 Unshroud, 6 Shuffle, 7 Discard,
 * 8 Draw/Return, 9 Place, 10 Search, 11 Excavate, 12 Displace, 13 Undisplace, 14 Ride,
 * 15 Swap, 16 Warp, 17 Move, 18 Gain/Lose Material, 19 Add Counters, 20 Remove Counters,
 * 21 Restore, 22 Damage, 23 Send to Decisive, 24 Destroy, 25 Change Control, 26 Add Name,
 * 27 Add Effect, 28 Unreveal, 29 Shroud, 30 Add Phase, 31 Skip Phase, 32 Remove Phase,
 * 33 Change Target, 34 Change/Remove Name, 35 Change/Remove Effect, 36 Change Attribute, 37 Negate.
 */
public enum EffectChangeType {
    // --- 1 Pay ---
    PayMaterial(1),
    PayMaterialPer(1), // The same as PayMaterial, but gains the condition of MinCardThisCardUses, rather than MinMaterial.
    PayHealth(1),

    // --- 2 Target ---
    MultiTarget(2), RandomMultiTarget(2), // May target the same thing twice with 2+ targets. Other targets cannot.
    UniqueTarget(2), RandomUniqueTarget(2),
    TargetAll(2),
    AdjacentTarget(2),           // 2+ targets: each adjacent to a previous target
    AdjacentToOriginalTarget(2), // 2+ targets: each adjacent to the first target
    NonAdjacentTarget(2),        // 2+ targets: not adjacent to a previous target
    SameLaneTarget(2),           // 2+ targets: each in the same lane as the first target
    SameFileTarget(2),           // 2+ targets: each in the same file as the first target

    // --- 3 Declare / Choose ---
    DeclareNum(3), DeclareName(3), DeclareAttribute(3), DeclarePile(3), DeclareDirection(3),
    DeclareRandomDirection(3),
    Choose1(3), Choose2(3), Choose3(3), RandomChoose1(3), RandomChoose2(3), RandomChoose3(3),

    // --- 4 Reveal ---
    Reveal(4),
    // --- 5 Unshroud ---
    Unshroud(5),
    // --- 6 Shuffle ---
    Shuffle(6), ShuffleMaterial(6),
    // --- 7 Discard ---
    Discard(7), SendToDiscard(7), DiscardMaterial(7),
    // --- 8 Draw / return to hand ---
    Draw(8), DrawFromOpponentsDeck(8), // Every valid commander target draws strength
    AddToHand(8), ReturnMaterialToHand(8), ReturnToHandWith1Rage(8),

    // --- 9 Place ---
    /// As the effect portion of every PlaceCost, this effect is what actually places the card.
    /// The target determines what types of zones the card can be placed on. Only targets zones.
    selfPlacementEffect(9),

    Place(9),
    PlaceShrouded(9), PlaceAsToken(9), PlaceWith1Health(9),
    PlaceWithNoCost(9), PlaceWithOnlyMaterialCost(9),
    PlaceAdjacentToThisCard(9), PlaceOnTarget(9), PlaceWithNoCostOnTarget(9),
    /// Places 1 card, subtracting strength material from its placement cost, min 0.
    PlaceMinusStrengthMaterialMin0(9),
    /// Places 1 card, subtracting strength material from its placement cost, no min.
    PlaceMinusStrengthMaterialNoMin(9),
    /// Subtracts strength from material cost paid per card, min 0.
    PlaceAnyAmount(9),

    PlaceConspiracyToken(9), PlaceTreeToken(9), PlaceAirToken(9), PlaceScrapToken(9), PlaceSporeToken(9),
    PlaceShrubToken(9), PlaceSpiritToken(9), PlacePilotFishToken(9), PlaceHillToken(9), PlaceEchoToken(9),
    PlaceRatToken(9), PlaceDemonToken(9), PlaceImitationToken(9), PlaceLabSubject00Token(9), PlaceSoulToken(9),
    PlaceOpenMindToken(9), PlacePureEnergyToken(9), PlaceTravestyToken(9), PlaceTargetNameToken(9),

    PlaceUsingHand(9), PlaceUsingYourCardsOnField(9), PlaceUsingDestroyed(9), PlaceUsingDiscard(9),
    PlaceUsingDisplaced(9), PlaceUsingDeck(9), PlaceUsingDecisivePile(9), PlaceUsingCardsOnField(9),
    PlaceUsingOpponentsCardsOnField(9), PlaceUsingTarget(9), PlaceUsingZonesYouControl(9),
    PlaceUsingHandOrWithEffect(9),

    // --- 10 Search ---
    Search(10),
    // --- 11 Excavate ---
    Excavate(11),
    // --- 12 Displace ---
    Displace(12), DisplaceMaterial(12),
    // --- 13 Undisplace ---
    Undisplace(13),
    // --- 14 Ride ---
    RideAdjacent(14), RideByEffect(14),
    /// Doesn't heed restrictions
    RideWithoutCost(14), RideWithoutCostHeedingRestrictions(14),
    // --- 15 Swap ---
    SwapSelf(15), SecretlySwapSelf(15), Swap2Choices(15), SecretlySwap2Choices(15),
    // --- 16 Warp ---
    Warp(16),
    // --- 17 Move ---
    MoveForwards(17), MoveBackwards(17), MoveLeftOrRight(17), MoveLeft(17), MoveRight(17),
    MoveDeclaredDirection(17), MoveForwardsOrBackwards(17), MoveAnyDirection(17),
    MoveAllForwards(17), MoveAllDeclared(17),
    // --- 18 Gain / lose material ---
    GainMaterial(18), LoseMaterial(18),

    // --- 19 Add counters ---
    AddEnergyCounters(19), AddBountyCounters(19), AddScrapCounters(19), AddTideCounters(19), AddRageCounters(19),
    AddSturdyCounters(19), AddSiGNLCounters(19), AddTravelCounters(19), AddFervorCounters(19),
    AddStorageCounters(19), AddUndeadCounters(19), AddVirtueCounters(19), AddSinCounters(19),
    AddDreamCounters(19), AddInjectionCounters(19), AddVeilCounters(19), AddOverchargeCounters(19),

    AddFortificationCounters(19), AddFrozenCounters(19), AddFlameCounters(19), AddInfectionCounters(19),
    AddEtherealCounters(19), AddMomentumCounters(19),
    AddDamageEcho(19), AddRestoreEcho(19), CopyEcho(19),

    AddRageCountersToMaterial(19),

    // --- 20 Remove counters ---
    RemoveCounters(20), RemoveEnergyCounters(20), RemoveBountyCounters(20), RemoveScrapCounters(20),
    RemoveTideCounters(20), RemoveRageCounters(20), RemoveSturdyCounters(20), RemoveSiGNLCounters(20),
    RemoveTravelCounters(20), RemoveFervorCounters(20), RemoveStorageCounters(20), RemoveUndeadCounters(20),
    RemoveVirtueCounters(20), RemoveSinCounters(20), RemoveDreamCounters(20), RemoveInjectionCounters(20),
    RemoveVeilCounters(20), RemoveOverchargeCounters(20),

    RemoveFortificationCounters(20), RemoveFrozenCounters(20), RemoveFlameCounters(20), RemoveInfectionCounters(20),
    RemoveEtherealCounters(20), RemoveMomentumCounters(20),
    RemoveEcho(20), RemoveDamageEcho(20), RemoveRestoreEcho(20),

    RemoveCountersFromMaterial(20),

    // --- 21 Restore ---
    Restore(21), DoubleHealth(21), RestoreAll(21),
    // --- 22 Damage ---
    Damage(22), HalveHealthRoundedUp(22), HalveHealthRoundedDown(22), DamageAll(22), RemoveHealthFromMaterial(22),
    // --- 23 Send to Decisive ---
    SendToDecisiveDeck(23), SendMaterialToDecisiveDeck(23),
    // --- 24 Destroy ---
    Destroy(24), SendToDestroyed(24), DestroyMaterial(24),
    // --- 25 Change Control ---
    GainControl(25),
    ControllerBecomesThisCardsUser(25),
    OwnerBecomesThisCardsUser(25),
    // --- 26 Add Name ---
    CopyName(26), AddNameDoomed(26),
    // --- 27 Add Effect ---
    AddEffect(27), // Will need to be a different effectChange for every added effect.
    // --- 28 Unreveal ---
    Unreveal(28),
    // --- 29 Shroud ---
    Shroud(29), ShroudAll(29),
    // --- 30 Add Phase ---
    AddDrawPhase(30), AddBuildPhase(30), AddCombatPhase(30), AddAftermathPhase(30),
    // --- 31 Skip Phase ---
    SkipDrawPhase(31), SkipBuildPhase(32), SkipCombatPhase(32), SkipAftermathPhase(32), SkipCurrentPhase(31),
    // --- 32 Remove Phase ---
    RemoveDrawPhase(32), RemoveBuildPhase(32), RemoveCombatPhase(32), RemoveAftermathPhase(32), RemoveCurrentPhase(32),
    // --- 33 Change Target ---
    ChangeTarget(33),
    // --- 34 Change / Remove Name ---
    AddDoomedToName(34), AddVolcanicToName(34), RemoveName(34), CopyTargetsName(35),
    // --- 35 Change / Remove Effect ---
    GainOverclocked(35),
    GainImmobile(35),
    RemoveEffect(35),
    // --- 36 Change Attribute ---
    ChangeAttributeToEther(36), ChangeAttributeToTarget(36),
    // --- 37 Negate ---
    /// Negates entire living object
    Negate(37),
    NegateAll(37),
    /// Choose any effect of a target to negate
    NegateEffect(37),

    // --- Continuous effects ---
    // Simult num represents which Type the continuous affects: (-1 state) (-2 LivingObject) (-3 EffectInstance) (-4 ConditionInstance on the Effect) (-5 EffectChange)
    Production(-1),
    /// Refers only to being not usable to place other cards

    NotUsable(-2),
    NotTargetable(-2),
    NotPlaceable(-2),
    NotDamageable(-2),
    IncreaseHandSize(-2), // On a commander
    /// Should never be written by itself, should only be defined implicitly using Effect.RIDEABLE
    Rideable(-2),

    ExcavateAddSiGNLtoExcavatedCardBeforeShuffle(-3),
    IncreaseAttackDamage(-3),
    // Adds that it can be used in [position] - doesn't replace the normal activation position
    TriggersCanBeUsedInHand(-3),
    ActivatableCanBeUsedInHand(-3),
    WhileDestroyedActivatableAlsoUsableWhileDiscarded(-3),
    RemoveNonMaterialPlacementRequirement(-3),

    OneLessRequiredInOpenField(-4),
    DamagingCardRequirementAutoMet(-4),

    EditOpenMindTokenToBeForAnyToken(-5),
    ExcavateMore(-5),
    YouWouldNegateACardAddThisCardToHand(-5),
    CardYouControlWouldBeNegatedDestroyThisCard(-5),

    // --- System Game Flow Changes (all begin with a lowercase letter) ---
    matchStart(0), matchEnd(0), surrender(0), disconnect(0),
    // in turn order //
    setupAddOrDraw(0), draw4AndRetry(0), clearJustHappened(0),

    gainCollectionMaterial(0), drawPhaseDraw(0),
        setProperOpportunity(0), beginDrawPhase(0), endDrawPhase(0),

    beginBuildPhase(0), commanderOpportunity(0), pass(0), endBuildPhase(0),

    beginCombatPhase(0), commanderLaneSelection(0), commanderActionListSelection(0),
        doNextAction(0), endCombatPhase(0),

    beginAftermathPhase(0), checkForWin(0), aftermathDiscard(0),
        commanderAftermathMovement(0), endAftermathPhase(0), frozenRemoval(0),
        flameRemoval(0), etherealDouble(0),

    // --- System Resolution Changes ---
    shuffleDeck(0), shuffleHand(0),
    /// For when a card gains more ethereal than health
    etherealDisplace(0),
    /// For when a commander gains more ethereal than health
    etherealHalve(0),

    removeFromGame(0), targetUnshroud(5), tickDuration(0), removeDuration(0),
    // //
    setController(0), setUser(0), setOwner(0), setName(0),
    /// For when a rider loses its rode card
    dismount(0),
    /// For when a non-rider is on the same zone as a rideable card, but not because they rode normally,
    /// they can immediately ride the card instead of being destroyed.
    forcedRide(0),
    /// For when two+ cards are on the same zone, and one can't ride the other
    rideDestruction(0),

    // duration / continuous unwinding //
    durationUndisplace(0);

    private final int simultNum;

    EffectChangeType(int s) {
        this.simultNum = s;
    }

    /**
     * The simultaneity number of this change in the Simultaneous Resolution Order (CR13.5.3).
     * 0 means the change is not part of the ordered resolution (continuous / system changes).
     */
    public int getSimultNum() {
        return simultNum;
    }

    /** Returns whether the effectChangeType is a "Beginning" of a phase / the game. */
    public static boolean isBeginning(EffectChangeType typeCheck) {
        return switch (typeCheck) {
            case beginDrawPhase, beginBuildPhase, beginAftermathPhase, beginCombatPhase, matchStart -> true;
            default -> false;
        };
    }

    /** Returns every EffectChangeType whose SimultNum matches with the given simultNum. */
    public static List<EffectChangeType> fromDeclaredSimult(int simultNum) {
        return Arrays.stream(values()).filter(e -> e.getSimultNum() == simultNum).toList();
    }

    /// A list of every EffectChange which can also be an optional continuous.
    public final static List<EffectChangeType> optionalContinuousList = List.of(IncreaseAttackDamage, CardYouControlWouldBeNegatedDestroyThisCard,
        YouWouldNegateACardAddThisCardToHand, ExcavateAddSiGNLtoExcavatedCardBeforeShuffle, ExcavateMore);
}
