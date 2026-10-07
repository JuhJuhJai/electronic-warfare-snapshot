package ew.engine.board;

import ew.engine.cards.effects.ConditionType;
import ew.engine.cards.effects.EffectChangeType;
import ew.engine.cards.effects.TargetType;

import java.util.Arrays;
import java.util.List;

public enum CounterType {
    Any,
    AnyEcho,

    Energy,
    Bounty,
    Scrap,
    Tide,
    Rage,
    Sturdy,
    SiGNL,
    Travel,
    Fervor,
    Storage,
    Undead,
    Virtue,
    Sin,
    Dream,
    Injection,
    Veil,
    Overcharge,

    Fortification,
    Frozen,
    Flame,
    Infection,
    Ethereal,
    Momentum,

    RevealEcho,
    UnshroudEcho,
    NameChangeEcho,
    DiscardEcho,
    DrawEcho,
    SearchEcho,
    ExcavateEcho,
    DisplaceEcho,
    UndisplaceEcho,
    RideEcho,
    SwapEcho,
    WarpEcho,
    MoveEcho,
    AddCounterEcho,
    RestoreEcho,
    DamageEcho,
    DestroyEcho,
    ChangeControlEcho,
    AddNameEcho,
    AddEffectEcho,
    UnrevealEcho,
    ShroudEcho,
    ChangeNameEcho,
    EffectRemoveEcho,
    NegateEcho,
    EmptyEcho;

    public static boolean notCollection(CounterType type) {
        return type.ordinal() >= 2;
    }

    public static boolean isInherent(CounterType type) {
        return type.ordinal() >= 19;
    }

    /** Returns the CounterType that corresponds to the given targetType. (e.g. WithFlameCounters = CounterType.Flame)  */
    public static CounterType convertCounter(TargetType counterCheck) {
        return switch (counterCheck) {
            case WithFortificationCounters -> CounterType.Fortification;
            case WithFrozenCounters -> CounterType.Frozen;
            case WithFlameCounters -> CounterType.Flame;
            case WithInfectionCounters -> CounterType.Infection;
            case WithEtherealCounters -> CounterType.Ethereal;
            case WithMomentumCounters -> CounterType.Momentum;
            case WithEnergyCounters -> CounterType.Energy;
            case WithBountyCounters -> CounterType.Bounty;
            case WithScrapCounters -> CounterType.Scrap;
            case WithTideCounters -> CounterType.Tide;
            case WithRageCounters -> CounterType.Rage;
            case WithSturdyCounters -> CounterType.Sturdy;
            case WithSiGNLCounters -> CounterType.SiGNL;
            case WithTravelCounters -> CounterType.Travel;
            case WithFervorCounters -> CounterType.Fervor;
            case WithStorageCounters -> CounterType.Storage;
            case WithUndeadCounters -> CounterType.Undead;
            case WithVirtueCounters -> CounterType.Virtue;
            case WithSinCounters -> CounterType.Sin;
            case WithDreamCounters -> CounterType.Dream;
            case WithInjectionCounters -> CounterType.Injection;
            case WithVeilCounters -> CounterType.Veil;
            case WithOverchargeCounters -> CounterType.Overcharge;
            default -> throw new IllegalArgumentException("CounterChecker for convertCounter " + counterCheck + " is invalid.");
        };
    }

    /** Returns the counter that corresponds to the given EffectChangeType. (e.g. AddEnergyCounters = CounterType.Energy) */
    public static CounterType convertCounter(EffectChangeType counterSetter) {
        return switch (counterSetter) {
            case RemoveCounters -> CounterType.Any;
            case RemoveEcho -> CounterType.AnyEcho;
            case AddEnergyCounters, RemoveEnergyCounters -> CounterType.Energy;
            case AddBountyCounters, RemoveBountyCounters -> CounterType.Bounty;
            case AddScrapCounters, RemoveScrapCounters -> CounterType.Scrap;
            case AddTideCounters, RemoveTideCounters -> CounterType.Tide;
            case AddRageCounters, RemoveRageCounters, AddRageCountersToMaterial -> CounterType.Rage;
            case AddSturdyCounters, RemoveSturdyCounters -> CounterType.Sturdy;
            case AddSiGNLCounters, RemoveSiGNLCounters -> CounterType.SiGNL;
            case AddTravelCounters, RemoveTravelCounters -> CounterType.Travel;
            case AddFervorCounters, RemoveFervorCounters -> CounterType.Fervor;
            case AddStorageCounters, RemoveStorageCounters -> CounterType.Storage;
            case AddUndeadCounters, RemoveUndeadCounters -> CounterType.Undead;
            case AddVirtueCounters, RemoveVirtueCounters -> CounterType.Virtue;
            case AddSinCounters, RemoveSinCounters -> CounterType.Sin;
            case AddDreamCounters, RemoveDreamCounters -> CounterType.Dream;
            case AddInjectionCounters, RemoveInjectionCounters -> CounterType.Injection;
            case AddVeilCounters, RemoveVeilCounters -> CounterType.Veil;
            case AddOverchargeCounters, RemoveOverchargeCounters -> CounterType.Overcharge;
            case AddFortificationCounters, RemoveFortificationCounters -> CounterType.Fortification;
            case AddFrozenCounters, RemoveFrozenCounters -> CounterType.Frozen;
            case AddFlameCounters, RemoveFlameCounters -> CounterType.Flame;
            case AddInfectionCounters, RemoveInfectionCounters -> CounterType.Infection;
            case AddEtherealCounters, RemoveEtherealCounters -> CounterType.Ethereal;
            case AddMomentumCounters, RemoveMomentumCounters -> CounterType.Momentum;
            case AddDamageEcho, RemoveDamageEcho -> CounterType.DamageEcho;
            case AddRestoreEcho, RemoveRestoreEcho -> CounterType.RestoreEcho;
            default -> throw new IllegalArgumentException("CounterSetter for convertCounter " + counterSetter + " is invalid.");
        };
    }

    /** Returns the counter that corresponds to the given ConditionType. (e.g. MinFrozenCounters = CounterType.Frozen) */
    public static CounterType convertCounter(ConditionType counterChecker) {
        return switch (counterChecker) {
            case MinFortificationCounters -> CounterType.Fortification;
            case MinFrozenCounters -> CounterType.Frozen;
            case MinFlameCounters -> CounterType.Flame;
            case MinInfectionCounters -> CounterType.Infection;
            case MinEtherealCounters -> CounterType.Ethereal;
            case MinMomentumCounters, MaxMomentumCounters -> CounterType.Momentum;
            case MinEnergyCounters -> CounterType.Energy;
            case MinBountyCounters -> CounterType.Bounty;
            case MinScrapCounters -> CounterType.Scrap;
            case MinTideCounters ->	CounterType.Tide;
            case MinRageCounters ->	CounterType.Rage;
            case MinSturdyCounters -> CounterType.Sturdy;
            case MinSiGNLCounters -> CounterType.SiGNL;
            case MinTravelCounters -> CounterType.Travel;
            case MinFervorCounters -> CounterType.Fervor;
            case MinStorageCounters -> CounterType.Storage;
            case MinUndeadCounters -> CounterType.Undead;
            case MinVirtueCounters -> CounterType.Virtue;
            case MinSinCounters -> CounterType.Sin;
            case MinDreamCounters -> CounterType.Dream;
            case MinInjectionCounters -> CounterType.Injection;
            case MinVeilCounters ->	CounterType.Veil;
            case MinOverchargeCounters -> CounterType.Overcharge;
            default -> throw new IllegalArgumentException("CounterChecker for convertCounter " + counterChecker + " is invalid.");
        };
    }

    public static List<CounterType> getEchoCounters() {
        return Arrays.asList(RevealEcho,
            UnshroudEcho,
            NameChangeEcho,
            DiscardEcho,
            DrawEcho,
            SearchEcho,
            ExcavateEcho,
            DisplaceEcho,
            UndisplaceEcho,
            RideEcho,
            SwapEcho,
            WarpEcho,
            MoveEcho,
            AddCounterEcho,
            RestoreEcho,
            DamageEcho,
            DestroyEcho,
            ChangeControlEcho,
            AddNameEcho,
            AddEffectEcho,
            UnrevealEcho,
            ShroudEcho,
            ChangeNameEcho,
            EffectRemoveEcho,
            NegateEcho,
            EmptyEcho);
    }

    public static boolean isEchoCounter(CounterType counter) {
        return getEchoCounters().contains(counter);
    }
}
