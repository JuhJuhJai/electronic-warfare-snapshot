package ew.engine.cards.effects;

import ew.engine.resolver.VariableGameNum;
import ew.engine.resolver.VariableNumType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class EffectChange {
    final EffectChangeType type;
    final List<TargetType> stipulations;
    final VariableGameNum strength;
    final List<Condition> changeCondition;
    final EffectDuration duration;

    public List<Condition> getChangeConditions() { return changeCondition; }
    public EffectChangeType getType() { return type; }
    public List<TargetType> getTargetRequirements() { return stipulations; }
    public VariableGameNum getStrength() { return strength; }
    public EffectDuration getDuration() { return duration; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EffectChange other)) return false;
        return this.type == other.type
            && this.stipulations == other.stipulations
            && this.strength == other.strength
            && this.duration == other.duration
            && this.changeCondition == other.changeCondition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, stipulations, stipulations, duration, changeCondition);
    }

    protected EffectChange(List<Condition> changeCondition, EffectChangeType type, List<TargetType> stipulations, VariableGameNum strength, EffectDuration duration) {
        this.changeCondition = changeCondition;
        this.type = type;
        this.stipulations = stipulations;
        this.strength = strength;
        this.duration = duration;
    }

    public static EffectChange system(EffectChangeType type) {
        return new EffectChange(List.of(), type, List.of(TargetType.Gamestate), VariableGameNum.ZERO(), EffectDuration.INSTANT());
    }

    public static EffectChange choose(int choices) {
        return new EffectChange(List.of(), EffectChangeType.Choose, List.of(), VariableGameNum.Num(choices), EffectDuration.INSTANT());
    }

    public static EffectChange chooseRandom(int choices) {
        return new EffectChange(List.of(), EffectChangeType.RandomChoose, List.of(), VariableGameNum.Num(choices), EffectDuration.INSTANT());
    }

    public static EffectChange of(EffectChangeType type, List<TargetType> targets, VariableGameNum strength) {
        return new EffectChange(getChangeCondition(type, targets, strength), type, targets, strength, EffectDuration.INSTANT());
    }

    public static EffectChange of(EffectChangeType type, List<TargetType> targets, int strength) {
        return of(type, targets, VariableGameNum.Num(strength));
    }

    public static EffectChange of(EffectChangeType type, TargetType target, VariableGameNum strength) {
        return of(type, List.of(target), strength);
    }

    public static EffectChange of(EffectChangeType type, TargetType target, int strength) {
        return of(type, List.of(target), VariableGameNum.Num(strength));
    }

    /** Abbreviation that sets strength to 1. */
    public static EffectChange of(EffectChangeType type, List<TargetType> targets) {
        return of(type, targets, VariableGameNum.Num(1));
    }

    /** Abbreviation that sets strength to 1. */
    public static EffectChange of(EffectChangeType type, TargetType target) {
        return of(type, List.of(target), 1);
    }

    public static EffectChange duration(EffectChangeType type, List<TargetType> targets, VariableGameNum strength, EffectDuration duration) {
        return new EffectChange(getChangeCondition(type, targets, strength), type, targets, strength, duration);
    }

    public static EffectChange duration(EffectChangeType type, List<TargetType> targets, int strength, EffectDuration duration) {
        return duration(type, targets, VariableGameNum.Num(strength), duration);
    }

    public static EffectChange duration(EffectChangeType type, TargetType target, VariableGameNum strength, EffectDuration duration) {
        return duration(type, List.of(target), strength, duration);
    }

    public static EffectChange duration(EffectChangeType type, TargetType target, int strength, EffectDuration duration) {
        return duration(type, List.of(target), VariableGameNum.Num(strength), duration);
    }


    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, List<TargetType> targets, VariableGameNum strength) {
        return new EffectChange(Stream.concat(getChangeCondition(type, targets, strength).stream(), conditions.stream()).toList(),
            type, targets, strength, EffectDuration.INSTANT());
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, List<TargetType> targets, int strength) {
        return conditional(conditions, type, targets, VariableGameNum.Num(strength));
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, TargetType target, VariableGameNum strength) {
        return conditional(conditions, type, List.of(target), strength);
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, TargetType target, int strength) {
        return conditional(conditions, type, List.of(target), VariableGameNum.Num(strength));
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, List<TargetType> targets, VariableGameNum strength) {
        return new EffectChange(List.of(condition), type, targets, strength, EffectDuration.INSTANT());
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, List<TargetType> targets, int strength) {
        return conditional(condition, type, targets, VariableGameNum.Num(strength));
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, TargetType target, VariableGameNum strength) {
        return conditional(condition, type, List.of(target), strength);
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, TargetType target, int strength) {
        return conditional(condition, type, List.of(target), VariableGameNum.Num(strength));
    }

    // Below adds Duration
    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, List<TargetType> targets, VariableGameNum strength, EffectDuration duration) {
        return new EffectChange(Stream.concat(getChangeCondition(type, targets, strength).stream(), conditions.stream()).toList(),
            type, targets, strength, duration);
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, List<TargetType> targets, int strength, EffectDuration duration) {
        return conditional(conditions, type, targets, VariableGameNum.Num(strength), duration);
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, TargetType target, VariableGameNum strength, EffectDuration duration) {
        return conditional(conditions, type, List.of(target), strength, duration);
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, TargetType target, int strength, EffectDuration duration) {
        return conditional(conditions, type, List.of(target), VariableGameNum.Num(strength), duration);
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, List<TargetType> targets, VariableGameNum strength, EffectDuration duration) {
        return new EffectChange(List.of(condition), type, targets, strength, duration);
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, List<TargetType> targets, int strength, EffectDuration duration) {
        return conditional(condition, type, targets, VariableGameNum.Num(strength), duration);
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, TargetType target, VariableGameNum strength, EffectDuration duration) {
        return conditional(condition, type, List.of(target), strength, duration);
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, TargetType target, int strength, EffectDuration duration) {
        return conditional(condition, type, List.of(target), VariableGameNum.Num(strength), duration);
    }

    /**
     * While every other effectChange factory automatically adds conditions to the given effect
     * that must be true for the change to resolve, this constructor does not.
     * You can still get the base condition using the public getChangeCondition.
     */
    public static EffectChange noAutomaticCondition(List<Condition> conditions, EffectChangeType type, List<TargetType> targets, VariableGameNum strength, EffectDuration duration) {
        return new EffectChange(conditions, type, targets, strength, duration);
    }

    /**
     * Returns a List of base condition that an effectChangeType must meet to resolve.
     * Allows card description to be much less verbose, and prevents mistakes in
     * identifying effect conditions from its changes, over and over.
     */
    public static List<Condition> getChangeCondition(EffectChangeType type, List<TargetType> targets, VariableGameNum strength) {
        final VariableGameNum minStrength = VariableGameNum.MinCopy(strength);
        // Checks min strength for XVar effects. If the min is null, MinCopy just returns strength.
        switch(type) {
            case PayMaterial -> {
                if (strength.getType() == VariableNumType.PerCardThisCardUses)
                    return List.of(
                        Condition.of(ConditionType.MinMaterial, targets,
                            VariableGameNum.Variable(VariableNumType.MinCardThisCardUses, strength.getModifier()))
                    );
                return List.of(
                    Condition.of(ConditionType.MinMaterial, targets, minStrength)
                );
            }

            case PayMaterialPer -> {
                return List.of(
                    Condition.of(ConditionType.MinMaterial, targets, VariableGameNum.CopyDifferentType(minStrength, VariableNumType.MinCardThisCardUses))
                );
            }

            case PayHealth, Damage -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinHealth, Stream.concat(targets.stream(), Stream.of(TargetType.CardsOnTheField)).toList(), minStrength)
                );
            }

            case HalveHealthRoundedUp, HalveHealthRoundedDown -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinHealth, Stream.concat(targets.stream(), Stream.of(TargetType.CardsOnTheField)).toList(), 1)
                );
            }

            case GainMaterial -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.canGainMaterial)).toList(), 1)
                );
            }

            case DisplaceMaterial -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MaterialAvailable, targets, minStrength)
                );
            }

            // Add proper condition types for these
            case MultiTarget, RandomMultiTarget, UniqueTarget, RandomUniqueTarget,
                 TargetAll, AdjacentTarget, AdjacentToOriginalTarget, NonAdjacentTarget,
                 SameLaneTarget, SameFileTarget -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, targets, minStrength)
                );
            }

            case DeclareNum, DeclareName, DeclareAttribute, DeclarePile, DeclareDirection,
                 DeclareRandomDirection -> {
                return List.of(
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.canDeclare)).toList(), 1)
                );
            }

            case Choose, RandomChoose -> {
                return List.of(
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.canChoose)).toList(), 1)
                );
            }

            case MoveForwards -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.CardsThatCanMoveForwards, TargetType.isMoveable)).toList(), 1)
                );
            }

            case MoveBackwards -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.CardsThatCanMoveBackwards, TargetType.isMoveable)).toList(), 1)
                );
            }

            case MoveLeft -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.CardsThatCanMoveLeft, TargetType.isMoveable)).toList(), 1)
                );
            }

            case MoveRight -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.CardsThatCanMoveRight, TargetType.isMoveable)).toList(), 1)
                );
            }

            case MoveLeftOrRight -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.CardsThatCanMoveLeftOrRight, TargetType.isMoveable)).toList(), 1)
                );
            }

            case MoveForwardsOrBackwards -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.CardsThatCanMoveForwardsOrBackwards, TargetType.isMoveable)).toList(), 1)
                );
            }

            case Restore -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.isRestorable)).toList(), 1)
                );
            }

            case AddEnergyCounters, AddBountyCounters, AddScrapCounters, AddTideCounters, AddRageCounters, AddSturdyCounters,
                 AddSiGNLCounters, AddTravelCounters, AddFervorCounters, AddStorageCounters, AddUndeadCounters,
                 AddVirtueCounters, AddSinCounters, AddInjectionCounters, AddVeilCounters,
                 AddOverchargeCounters, AddFortificationCounters, AddFrozenCounters, AddFlameCounters, AddInfectionCounters,
                 AddEtherealCounters, AddMomentumCounters,
                 AddDamageEcho, AddRestoreEcho, CopyEcho,
                 AddRageCountersToMaterial -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.canGainCounters)).toList(), 1)
                );
            }

            case AddDreamCounters -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.canGainCounters,
                        TargetType.lessThan7DreamCountersORcanGainMoreThan7DreamCounter)).toList(), 1)
                );
            }

            case RemoveCounters, RemoveEnergyCounters, RemoveBountyCounters, RemoveScrapCounters, RemoveTideCounters,
                 RemoveRageCounters, RemoveSturdyCounters, RemoveSiGNLCounters, RemoveTravelCounters, RemoveFervorCounters,
                 RemoveStorageCounters, RemoveUndeadCounters, RemoveVirtueCounters, RemoveSinCounters, RemoveDreamCounters,
                 RemoveInjectionCounters, RemoveVeilCounters, RemoveOverchargeCounters, RemoveFortificationCounters,
                 RemoveFrozenCounters, RemoveFlameCounters, RemoveInfectionCounters, RemoveEtherealCounters,
                 RemoveMomentumCounters,
                 RemoveEcho, RemoveDamageEcho, RemoveRestoreEcho -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(counterRemovalToRemovalCondition(type), Stream.concat(targets.stream(), Stream.of(TargetType.canLoseCounters)).toList(), minStrength)
                );
            }

            case Place, PlaceAdjacentToThisCard, PlaceOnTarget, PlaceWithNoCost, PlaceWithNoCostOnTarget,
                 PlaceConspiracyToken, PlaceTreeToken, PlaceAirToken, PlaceScrapToken, PlaceSporeToken, PlaceShrubToken, PlaceSpiritToken,
                 PlacePilotFishToken, PlaceHillToken, PlaceEchoToken, PlaceRatToken, PlaceDemonToken, PlaceImitationToken,
                 PlaceLabSubject00Token, PlaceSoulToken, PlaceOpenMindToken, PlacePureEnergyToken, PlaceTravestyToken,
                 PlaceTargetNameToken -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.isPlaceable, TargetType.CardsWithSpaceToBePlaced)).toList(), minStrength)
                );
            }

            case PlaceAnyAmount, PlaceMinusStrengthMaterialMin0 -> {
                return List.of(
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.isPlaceable, TargetType.CardsWithSpaceToBePlaced)).toList(), 1)
                );
            }

            case PlaceUsingHand, PlaceUsingDestroyed, PlaceUsingDiscard, PlaceUsingDisplaced, PlaceUsingDeck,
                 PlaceUsingDecisivePile, PlaceUsingZonesYouControl, PlaceUsingHandOrWithEffect -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.isPlaceable, TargetType.CardsWithSpaceToBePlaced,
                        usingPlacementToTarget(type))).toList(), minStrength)
                );
            }

            case PlaceUsingYourCardsOnField, PlaceUsingCardsOnField, PlaceUsingOpponentsCardsOnField, PlaceUsingTarget -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.isPlaceable, TargetType.CardsWithSpaceToBePlacedAfterUse,
                        usingPlacementToTarget(type))).toList(), minStrength)
                );
            }

            case SwapSelf, SecretlySwapSelf -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.NotSelf, TargetType.isSwappable)).toList(), 1)
                );
            }

            case Swap2Choices, SecretlySwap2Choices -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.isSwappable)).toList(), 2)
                );
            }

            case Destroy -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.isDestroyable)).toList(), minStrength)
                );
            }

            case Discard -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.isDiscardable)).toList(), minStrength)
                );
            }

            case Displace -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.isDisplaceable)).toList(), minStrength)
                );
            }

            case Draw, DrawFromOpponentsDeck -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.canDraw)).toList(), 1)
                );
            }

            case ReturnToHand -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(), Stream.of(TargetType.canAddToHand)).toList(), 1)
                );
            }

            case Reveal -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(),
                        Stream.of(TargetType.NotRevealed, TargetType.isRevealable)).toList(), minStrength)
                );
            }

            case Unreveal -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(),
                        Stream.of(TargetType.Revealed, TargetType.isUnrevealable)).toList(), minStrength)
                );
            }

            case Unshroud -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(),
                        Stream.of(TargetType.ShroudedCards, TargetType.isUnshroudable)).toList(), minStrength)
                );
            }

            case RideAdjacent -> {
                return List.of(
                    Condition.of(ConditionType.HigherThan0Strength, List.of(), minStrength),
                    Condition.of(ConditionType.MinThings, Stream.concat(targets.stream(),
                        Stream.of(TargetType.ShroudedCards, TargetType.isUnshroudable)).toList(), minStrength)
                );
            }
        }
        throw new IllegalArgumentException("EffectChangeType (" + type + ") conditions not implemented.");
    }

    private static TargetType usingPlacementToTarget(EffectChangeType usingPlacement) {
        return switch(usingPlacement) {
            case PlaceUsingHand ->
                TargetType.PlacementCostCanBeMetByUsersCardsInHand;
            case PlaceUsingYourCardsOnField ->
                TargetType.PlacementCostCanBeMetByUsersFieldCards;
            case PlaceUsingDestroyed ->
                TargetType.PlacementCostCanBeMetByUsersDestroyedCards;
            case PlaceUsingDiscard ->
                TargetType.PlacementCostCanBeMetByUsersDiscardedCards;
            case PlaceUsingDisplaced ->
                TargetType.PlacementCostCanBeMetByUsersDisplacedCards;
            case PlaceUsingDeck ->
                TargetType.PlacementCostCanBeMetByUsersDeck;
            case PlaceUsingDecisivePile ->
                TargetType.PlacementCostCanBeMetByUsersDecisivePileCards;
            case PlaceUsingCardsOnField ->
                TargetType.PlacementCostCanBeMetByAnyFieldCards;
            case PlaceUsingOpponentsCardsOnField ->
                TargetType.PlacementCostCanBeMetByOpponentsFieldCards;
            case PlaceUsingTarget ->
                TargetType.PlacementCostCanBeMetByTargets;
            case PlaceUsingZonesYouControl ->
                TargetType.PlacementCostCanBeMetByUsersZones;
            case PlaceUsingHandOrWithEffect ->
                TargetType.PlacementCostCanBeMetByUsersCardsInHandOrNormally;
            default -> throw new IllegalArgumentException("Illegal placement type for usingPlacement (" + usingPlacement + ")");
        };
    }
    private static ConditionType counterRemovalToRemovalCondition(EffectChangeType counterRemoval) {
        return switch(counterRemoval) {
            case RemoveCounters ->
                ConditionType.MinCounters;
            case RemoveFortificationCounters ->
                ConditionType.MinFortificationCounters;
            case RemoveFrozenCounters ->
                ConditionType.MinFrozenCounters;
            case RemoveFlameCounters ->
                ConditionType.MinFlameCounters;
            case RemoveInfectionCounters ->
                ConditionType.MinInfectionCounters;
            case RemoveEtherealCounters ->
                ConditionType.MinEtherealCounters;
            case RemoveMomentumCounters ->
                ConditionType.MinMomentumCounters;
            case RemoveEnergyCounters ->
                ConditionType.MinEnergyCounters;
            case RemoveBountyCounters ->
                ConditionType.MinBountyCounters;
            case RemoveScrapCounters ->
                ConditionType.MinScrapCounters;
            case RemoveTideCounters ->
                ConditionType.MinTideCounters;
            case RemoveRageCounters ->
                ConditionType.MinRageCounters;
            case RemoveSturdyCounters ->
                ConditionType.MinSturdyCounters;
            case RemoveSiGNLCounters ->
                ConditionType.MinSiGNLCounters;
            case RemoveTravelCounters ->
                ConditionType.MinTravelCounters;
            case RemoveFervorCounters ->
                ConditionType.MinFervorCounters;
            case RemoveStorageCounters ->
                ConditionType.MinStorageCounters;
            case RemoveUndeadCounters ->
                ConditionType.MinUndeadCounters;
            case RemoveVirtueCounters ->
                ConditionType.MinVirtueCounters;
            case RemoveSinCounters ->
                ConditionType.MinSinCounters;
            case RemoveDreamCounters ->
                ConditionType.MinDreamCounters;
            case RemoveInjectionCounters ->
                ConditionType.MinInjectionCounters;
            case RemoveVeilCounters ->
                ConditionType.MinVeilCounters;
            case RemoveOverchargeCounters ->
                ConditionType.MinOverchargeCounters;
            case RemoveEcho ->
                ConditionType.MinEchoCounters;
            case RemoveDamageEcho ->
                ConditionType.MinDamageEcho;
            case RemoveRestoreEcho ->
                ConditionType.MinRestoreEcho;
            default -> throw new IllegalArgumentException("Illegal change type for counterRemoval (" + counterRemoval + ")");
        };
    }
}
