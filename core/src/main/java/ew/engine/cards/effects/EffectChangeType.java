package ew.engine.cards.effects;

/**
 * <p>Every possible effect type that can occur in the game.
 * <p>This requires every effect (including type) because there are many possible types for each
 * effect change, past strength, which *could* be made into something like an index system for
 * each type in the Effect Change (1 = EnergyCounter, 2 = BountyCounter, etc. with AddCounter
 * being the one type) but then the cards wouldn't be human-readable from just their descriptions.
 *
 * <p>Includes all effects in the Simultaneous Resolution Order:
 * Pay → Target → Declare → Reveal → Unshroud → Shuffle → Discard → Draw → Place → Search →
 * Excavate → Displace → Undisplace → Ride → Swap → Warp → Move → Gain/Lose Material →
 * Add Counters → Remove Counters → Restore → Damage → Destroy → Send to Decisive Deck →
 * Change Control → Add Name → Add Effect → Unreveal → Shroud → Add Phase → Skip Phase →
 * Remove Phase → Change Target → Change/Remove Name → Change/Remove Effect → Negate
 */
public enum EffectChangeType {
    // The following are effects referenced in the Simultaneous Resolution Order
    PayMaterial, PayHealth,
    UniqueTarget, MultiTarget, RandomTarget, RandomMultiTarget, // "multi" may target the same card twice in instances of more than one target
    DeclareNum, DeclareName, DeclareAttribute, DeclarePile, DeclareDirection, DeclareRandomDirection, Choose, RandomChoose, // Strength determines reference. There are too many options.
    Reveal,
    Unshroud,
    Shuffle, ShuffleMaterial,
    Discard, SendToDiscard, DiscardMaterial,
    Draw, DrawFromOpponentsDeck, ReturnToHand, ReturnMaterialToHand,
    Place, PlaceOnTarget, PlaceWithNoCost, PlaceWithNoCostOnTarget,
        PlaceConspiracyToken, PlaceTreeToken, PlaceAirToken, PlaceScrapToken, PlaceSporeToken, PlaceShrubToken, PlaceSpiritToken,
        PlacePilotFishToken, PlaceHillToken, PlaceEchoToken, PlaceRatToken, PlaceDemonToken, PlaceImitationToken,
        PlaceLabSubject00Token, PlaceSoulToken, PlaceOpenMindToken, PlacePureEnergyToken, PlaceTravestyToken,
        PlaceTargetNameToken,
        PlaceUsingHand, PlaceUsingYourCardsOnField, PlaceUsingDestroyed, PlaceUsingDiscard, PlaceUsingDisplaced, PlaceUsingDeck,
        PlaceUsingDecisivePile, PlaceUsingCardsOnField, PlaceUsingOpponentsCardsOnField, PlaceUsingTarget, PlaceUsingZonesYouControl,
        PlaceUsingOrWithEffect,
    Search,
    Excavate,
    // For displace, strength is the number of cards. Displace material's strength is the number of material.
    // Displace material also stores the number of cards displaced in the card's Qvariable.
    Displace, DisplaceMaterial,
    Undisplace,
    RideAdjacent, /* the following ride effects make the choice ride the target */
        RideByEffect, RideWithoutCost, RideWithoutCostHeedingRestrictions,
    SwapWithTarget, SecretlySwapWithTarget, Swap2Choices, SecretlySwap2Choices, // Swap can also be used on cards not on the field, if not specified
    Warp,
    MoveForwards, MoveBackwards, MoveLeftOrRight, MoveForwardsOrBackwards, MoveAnyDirection, // orthogonal
    GainMaterial, LoseMaterial,

    // Card readability prioritized over adding a type variable
    AddEnergyCounters, AddBountyCounters, AddScrapCounters, AddTideCounters, AddRageCounters, AddSturdyCounters,
        AddSiGNLCounters, AddTravelCounters, AddFervorCounters, AddStorageCounters, AddUndeadCounters,
        AddVirtueCounters, AddSinCounters, AddDreamCounters, AddInjectionCounters, AddVeilCounters,
        AddOverchargeCounters, AddFortificationCounters, AddFrozenCounters, AddFlameCounters, AddInfectionCounters,
        AddEtherealCounters, AddMomentumCounters,
        AddDamageEcho, AddRestoreEcho, CopyEcho,
        AddRageCountersToMaterial, // there's a card that does this
    RemoveCounters, RemoveEnergyCounters, RemoveBountyCounters, RemoveScrapCounters, RemoveTideCounters,
        RemoveRageCounters, RemoveSturdyCounters, RemoveSiGNLCounters, RemoveTravelCounters, RemoveFervorCounters,
        RemoveStorageCounters, RemoveUndeadCounters, RemoveVirtueCounters, RemoveSinCounters, RemoveDreamCounters,
        RemoveInjectionCounters, RemoveVeilCounters, RemoveOverchargeCounters, RemoveFortificationCounters,
        RemoveFrozenCounters, RemoveFlameCounters, RemoveInfectionCounters, RemoveEtherealCounters,
        RemoveMomentumCounters,
        RemoveEcho, RemoveDamageEcho, RemoveRestoreEcho,

    Restore, DoubleHealth,
    Damage, HalveHealthRoundedUp, HalveHealthRoundedDown,
    SendToDecisiveDeck, SendMaterialToDecisiveDeck,
    Destroy, SendToDestroyed, DestroyMaterial,
    GainControl,
    CopyName, AddNameDoomed,
    AddEffect,
    Unreveal,
    Shroud,
    AddDrawPhase, AddBuildPhase, AddCombatPhase, AddAftermathPhase,
    SkipPhase,
    RemoveDrawPhase, RemoveBuildPhase, RemoveCombatPhase, RemoveAftermathPhase,
    ChangeName, RemoveName,
    ChangeEffect, RemoveEffect, ChangeType,
    ChangeTarget,
    Negate,

    // Continuous effects will be verbose and specific. They can't be modular.
    // The following are Constant Continuous effects
    Production,
    IncreaseAttackDamage,
    IfCardYouControlWouldBeNegatedDestroyThisCard,
    IfYouWouldNegateACardAddThisCardToHand,
    IncreaseHandSize,
    CannotBeUsed,

    RideablePayMaterial,

    // The following are Optional Continuous effects (That are chosen at effect resolution)
    IfYouExcavateCanAddSiGNLtoExcavatedCardBeforeShuffle,
    IfYouExcavateCanExcavateMore,
    IfAttackCanAddDamage,

    // The following effects are System Effect Changes
    MatchStart, MatchEnd, Surrender, Disconnect,

    ShuffleDeck, ShuffleHand,

    SetupAddOrDraw, Draw4OrRetry, ClearJustHappened,

    BeginDrawPhase, GainCollectionMaterial, DrawPhaseDraw, EndDrawPhase,
    BeginBuildPhase, CommanderOpportunity, Pass, EndBuildPhase,
    BeginCombatPhase, CommanderLaneSelection, CommanderActionListSelection, DoNextAction, EndCombatPhase,
    BeginAftermathPhase, CheckForWin, EndAftermathPhase, AftermathDiscard,

    CommanderAftermathMovement, FrozenRemoval, FlameRemoval, EtherealDouble, EtherealDisplace,

    RemoveFromGame;
    /**
     * Returns the simultaneity number of an effect, as it is in the simultaneous resolution order.
     * As there some have several types of effect per reference, many ordinal are assigned to the same value.
     */
    public int getSimultNum() {
        final int n = this.ordinal();
        if (n >= 166) return 0; // Effects not mentioned in simultaneous resolution order go first.
        if (n <= 1) return 1; // Pay
        if (n <= 5) return 2; // Target
        if (n <= 13) return 3; // Declare / choose
        if (n == 14) return 4; // Reveal
        if (n == 15) return 5; // Unshroud
        if (n <= 17) return 6; // Shuffle
        if (n <= 20) return 7; // Discard
        if (n <= 25) return 8; // Draw / return to hand
        if (n <= 58) return 9; // Place
        if (n == 60) return 10; // Search
        if (n == 61) return 11; // Excavate
        if (n <= 63) return 12; // Displace
        if (n == 64) return 13; // Undisplace
        if (n <= 68) return 14; // Ride
        if (n <= 72) return 15; // Swap
        if (n == 73) return 16; // Warp
        if (n <= 78) return 17; // Move
        if (n <= 80) return 18; // Gain / lose material
        if (n <= 107) return 19; // Add counters
        if (n <= 134) return 20; // Remove counters
        if (n <= 136) return 21; // Restore
        if (n <= 139) return 22; // Damage
        if (n <= 141) return 23; // Send to Decisive
        if (n <= 144) return 24; // Destroy
        if (n == 145) return 25; // Gain control
        if (n <= 147) return 26; // Change name
        if (n == 148) return 27; // Add Effect
        if (n == 149) return 28; // Unreveal
        if (n == 150) return 29; // Shroud
        // etc

        return 0;
    }

    /** Returns whether the effectChangeType is a "Beginning" of a phase / the game. */
    public static boolean isBeginning(EffectChangeType typeCheck) {
        return switch(typeCheck) {
            case BeginDrawPhase, BeginBuildPhase, BeginAftermathPhase, BeginCombatPhase, MatchStart -> true;
            default -> false;
        };
    }
}
