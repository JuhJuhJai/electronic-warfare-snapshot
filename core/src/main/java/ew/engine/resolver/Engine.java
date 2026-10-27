package ew.engine.resolver;

import ew.engine.board.*;
import ew.engine.board.cardInstance.CardInstance;
import ew.engine.board.cardInstance.CardRideState;
import ew.engine.board.commanderInstance.CommanderInstance;
import ew.engine.board.effectInstance.*;
import ew.engine.board.state.*;
import ew.engine.board.zoneInstance.ZoneInstance;
import ew.engine.board.zoneInstance.ZoneType;
import ew.engine.cards.CardAttribute;
import ew.engine.cards.CardDefinition;
import ew.engine.cards.CardType;
import ew.engine.cards.effects.*;
import ew.engine.resolver.log.Log;
import ew.engine.resolver.log.GameChoice;
import ew.engine.resolver.log.GameChoiceChange;
import ew.engine.resolver.view.FullGameStateView;
import ew.playerData.Commander;
import ew.playerData.Deck;
import ew.server.*;
import ew.engine.resolver.log.GameEvent;

import java.util.*;
import java.util.function.*;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

/**
 * Creates a GameState and returns the winning commander of the game.
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class Engine { // Engine, along with VariableGameNum, should be the only thing that enact rules from https://docs.google.com/document/d/1SiZaZUJU2aiVstrVPI6lUHv41mXre5qFFRzWFt9wrsY/edit?tab=t.0 on the GameState.
    final Room room;
    final GameState state;
    final long matchID;
    final RandomGenerator random;
    final MatchFormat format = MatchFormat.STANDARD; // Eventually change to be set in the constructor.

    /** Standard Match. */
    public Engine(Room room, long matchID, RandomGenerator random, Commander player1, Deck player1Deck, Commander player2, Deck player2Deck) {
        this.room = room;
        this.matchID = matchID;
        this.random = random;
        state = new GameState(format, player1, player1Deck, player2, player2Deck);
    }

    public FullGameStateView getGameView(SideID view) {
        return new FullGameStateView(); // Implement later - resolve view to records.
    }

    /** Returns the result of the match. */
    public List<GameResult> start() {
        // Setup. No cards can do anything yet.
        systemChange(EffectChangeType.shuffleDeck, state.getCommanders());
        // Both players choose a card to add with <= 6 material cost or draw 1 card.
        systemChange(EffectChangeType.setupAddOrDraw); // Removes material equal to its cost from the commanders that add

        // Both players draw 4 cards, and may retry.
        systemChange(EffectChangeType.draw4AndRetry);
        systemChange(EffectChangeType.matchStart);

        // Game Begins.
        while(state.gameGoing) {
            for (Phase phase : state.getPhaseOrderInstance()) {
                switch (phase) {
                    case DRAW -> {
                        // If another draw phase already occurred, then don't increase the turn count. The turn count begins at 0.
                        if (!justHappenedThisTurn(e -> e.getChange().getType() == EffectChangeType.beginDrawPhase)) state.turn = state.turn + 1;

                        systemChange(EffectChangeType.setProperOpportunity);
                        if (state.turn != 1) {
                            systemChange(EffectChangeType.drawPhaseDraw); // Doesn't occur on the first turn
                        }
                        systemChange(EffectChangeType.gainCollectionMaterial);
                        systemChange(EffectChangeType.beginDrawPhase);
                        resolveTriggerChains();

                        systemChange(EffectChangeType.endDrawPhase);
                        resolveTriggerChains();
                    }

                    case BUILD -> {
                        systemChange(EffectChangeType.beginBuildPhase); // Includes giving knowledge of which commander has the first opportunity
                        resolveTriggerChains();

                        while (noConsecutivePasses()) {
                            systemChange(EffectChangeType.commanderOpportunity); // Includes the commander with the opportunity spending it (using an activatable / passing)
                            resolveTriggerChains();
                        }

                        systemChange(EffectChangeType.endBuildPhase);
                        resolveTriggerChains();
                    }

                    case COMBAT -> {
                        systemChange(EffectChangeType.beginCombatPhase);
                        resolveTriggerChains();

                        while (hasAvailableLanes()) {
                            systemChange(EffectChangeType.commanderLaneSelection); // also sets the next commander to choose a lane
                            resolveTriggerChains();
                            systemChange(EffectChangeType.commanderActionListSelection);
                            while (hasAvailableActions()) {
                                systemChange(EffectChangeType.doNextAction);
                                resolveTriggerChains();
                            }
                        }

                        systemChange(EffectChangeType.endCombatPhase);
                        resolveTriggerChains();
                    }

                    case AFTERMATH -> {
                        systemChange(EffectChangeType.beginAftermathPhase);
                        resolveTriggerChains();

                        systemChange(EffectChangeType.commanderAftermathMovement);
                        resolveTriggerChains();

                        systemChange(EffectChangeType.checkForWin); // If one commander's health <= 0, the other wins. If both are below zero, it's a draw.
                        if (state.getGameResult() != null) return state.getGameResult();

                        systemChange(EffectChangeType.endAftermathPhase); // Aftermath discard included in ending aftermath phase
                        resolveTriggerChains();
                    }
                }
            }
        }
        return List.of(
            new GameResult(
                room.getController1().getCommander(),
                GameResultType.ABANDONED),
            new GameResult(
                room.getController2().getCommander(),
                GameResultType.ABANDONED)
        );
    }

    /* --- Game Flow Functions --- */
    /**
     * Returns true unless the last two opportunities to play have been passed.
     * Sets the state's firstToPass when it returns false.
     */
    private boolean noConsecutivePasses() {
        final List<EffectChangeType> passesAndUses = room.getLog().getLog(state.turn).stream()
            .flatMap(List::stream)
            .map(c -> c.getChange().getType())
            .filter(type -> type == EffectChangeType.commanderOpportunity || type == EffectChangeType.pass)
            .toList();
        for (int i = 1; i < passesAndUses.size(); i++) {
            if (passesAndUses.get(i) == EffectChangeType.pass && passesAndUses.get(i - 1) == EffectChangeType.pass) return false;
        }
        return true;
    }

    /** Returns true unless no lanes are available for selection during the combat phase (has been selected / has no cards with usable action) */
    private boolean hasAvailableLanes() {
        for (int i = 1; i <= 5; i++) {
            if (state.getResolvedLanes().contains(i)) continue;
            if (state.field.getLane(i).stream()
                .map(ZoneInstance::getCards)
                .flatMap(List::stream)
                .map(CardInstance::getInstancedEffects)
                .flatMap(List::stream)
                .anyMatch(effect -> effect.getType() == EffectType.Action
                    && isEffectLegal(effect, getActiveContinuous())
                )) return true;
        }
        return false;
    }

    /** Returns true unless there's no more actions in either commander's action list */
    private boolean hasAvailableActions() {
        return !state.getPlayerOneActionList().isEmpty() || !state.getPlayerTwoActionList().isEmpty(); // Resolution deletes already used actions.
    }

    /** Calls resolveTriggerChain on the first action in state's playerOneActionList and playerTwoActionList, then removes it. */
    private void resolveNextAction() {
        final EffectInstance one = !state.getPlayerOneActionList().isEmpty() ? state.getPlayerOneActionList().get(0) : null;
        final EffectInstance two = !state.getPlayerTwoActionList().isEmpty() ? state.getPlayerTwoActionList().get(0) : null;
        if (one == null && two == null) return;
        if (one == null) {
            resolveEffect(two);
        }
        else if (two == null) {
            resolveEffect(one);
        }
        else {
            resolveEffects(List.of(one, two));
        }
        state.removeOneAction();
    }

    /** A commander uses an available effect of theirs. */
    private void resolveCommanderOpportunity() {
        // A commander can do any met effect they have or their card has,
        // which should only be an activatable they have or their card has.
        List<ContinuousInstance> c = getActiveContinuous();
        final ControllerInstance opportunityHolder = sideIDtoController(state.opportunityHolder);
        resolveEffect(getPlayerChoice(opportunityHolder, getGameChoices(getAvailableEffects(c), c)).getEffect());
    }

    /* --- Resolution Functions --- */
    private void systemChange(EffectChangeType changeType) {
        systemChange(changeType, List.of(), List.of());
    }

    private void systemChange(EffectChangeType changeType, LivingObject target) {
        systemChange(changeType, List.of(target));
    }

    private void systemChange(EffectChangeType changeType, List<? extends LivingObject> targets) {
        systemChange(changeType, targets, List.of());
    }

    private void systemChange(EffectChangeType changeType, List<? extends LivingObject> targets, List<Integer> choiceInfo) {
        final EffectChangeInstance effectChange = new EffectChangeInstance(EffectChange.system(changeType), null, state.getNextEffectID(), null);
        systemChange(new GameChoiceChange(
                    effectChange,
                    targets,
                    choiceInfo));
    }

    private void systemChange(GameChoiceChange change) {
        final List<GameEvent> events = new ArrayList<>();
        resolveGameChange(List.of(List.of(change)), events);
        resolveAfterChange(events);
        resolveLogAdding(events);
    }

    // systemChange below are used when other system change must call another effect.
    // They do not resolveAfterChange, and must be provided a List GameEvent to add to.
    private void systemRecursiveChange(GameChoiceChange change, List<GameEvent> prevLog) {
        resolveGameChange(List.of(List.of(change)), prevLog);
    }

    private void systemRecursiveChange(EffectChangeType changeType, List<Integer> choiceInfo, List<GameEvent> prevLog) {
        final EffectChangeInstance effectChange = new EffectChangeInstance(EffectChange.system(changeType), null, state.getNextEffectID(), null);
        resolveGameChange(List.of(List.of(
            new GameChoiceChange(
                effectChange,
                List.of(),
                choiceInfo))), prevLog);
    }

    private void systemRecursiveChange(EffectChangeType changeType, LivingObject target, List<Integer> choiceInfo, List<GameEvent> prevLog) {
        final EffectChangeInstance effectChange = new EffectChangeInstance(EffectChange.system(changeType), null, state.getNextEffectID(), null);
        resolveGameChange(List.of(List.of(
            new GameChoiceChange(
                effectChange,
                List.of(target),
                choiceInfo))), prevLog);
    }

    private void systemRecursiveChange(EffectChangeType changeType, List<? extends LivingObject> targets, List<Integer> choiceInfo, List<GameEvent> prevLog) {
        final EffectChangeInstance effectChange = new EffectChangeInstance(EffectChange.system(changeType), null, state.getNextEffectID(), null);
        resolveGameChange(List.of(List.of(
            new GameChoiceChange(
                effectChange,
                targets,
                choiceInfo))), prevLog);
    }

    /**
     * Resolves trigger chain rules, which are as follows:
     * When a timing point is reached, check for any triggers whose conditions are true. Those triggers are "met".
     * Commanders alternate choices for which trigger they own that has been met which they want to resolve.
     * The commander who chooses first is the one who has the first opportunity to play this turn.
     * If a commander doesn't have any met triggers, then their opponent resolves any of theirs.
     * If neither commander has any met triggers, or all triggers that remain are strategic, and they aren't chosen, then the trigger chain ends.
     * At the end of the trigger chain, remove all that's "Just happened"
     */
    private void resolveTriggerChains() {
        state.triggerOpportunity = state.firstTriggerOpportunity;
        state.isInTriggerChain = true;
        boolean lastEffectUsed = true;
        while (true) {
            List<ContinuousInstance> c = getActiveContinuous();
            List<GameChoice> metEffects = getGameChoices(getAvailableEffects(c), c); // updated after each trigger. This removes old ones that are now invalid.
            // If no effects are met
            if (metEffects.isEmpty() || (metEffects.stream() // Or if only strategic remain,
                .allMatch(e -> e.getEffect().getInstanceConditions().stream()
                    .anyMatch(s -> s.getType() == ConditionType.Strategic))
                // Then ask if they want to use the strategic, and if not
                    && !getPlayerOption(sideIDtoController(state.triggerOpportunity), GameChoiceChange.USE_STRATEGIC()))) {
                // If the last commander didn't use a trigger either
                if (lastEffectUsed) {
                    lastEffectUsed = false;
                }
                // Stop the trigger chain
                else break;
            }
            else { // If an effect is met
                // Resolve the effect a player chooses (resolve effects adds to the log itself, and also does resolveAfterEffect)
                resolveEffects(List.of(getPlayerChoice(sideIDtoController(state.triggerOpportunity), metEffects).getEffect()));
                lastEffectUsed = true;
            }
            state.triggerOpportunity = state.triggerOpportunity.opponentOf();
        }

        state.clearJustHappened();
        state.isInTriggerChain = false;
    }

    /** Abbreviation for resolveEffects which only needs one effect. */
    private void resolveEffect(EffectInstance effect) {
        resolveEffects(Collections.singletonList(effect));
    }

    /**
     * Given a List EffectInstance, attempts to resolve every EffectChange for the cost and effect of that effectInstance,
     * as well as the resulting EffectInstance that may be made legal as a result of the effectChange resolution.
     * Calls resolveAfterEffect after itself.
     * Does not check initially given effects for legality.
     */
    private void resolveEffects(List<EffectInstance> allSimultaneousEffects) {
        class Bool { // Class so that furtherResolutionAllowed is mutable within GameEffect
            public boolean bool;
            public Bool(boolean bool) {
                this.bool = bool;
            }
        }
        record GameEffect(int costStart, int effectStart, EffectInstance effect, Bool furtherResolutionAllowed) {}
        List<GameEffect> effects = new ArrayList<>();
        for (EffectInstance e : allSimultaneousEffects) {
            effects.add(new GameEffect(0, 0, e, new Bool(true)));
        }
        final TimingPoint originalTiming = state.timingPoint;
        // For the cost and effect
        for (int i = 0; i <= 1; i++) {
            // For each change in the cost and effect
            for (int n = 0; true; n++) {
                final List<ContinuousInstance> activeConstantContinuous = getActiveContinuous();
                // For every possible choice for every current effect change (1 change is a List<GameChoiceChange>, )
                final List<List<GameChoiceChange>> effectsToResolve = new ArrayList<>();
                // For every effect
                for (GameEffect effect : effects) {
                    if (effect.furtherResolutionAllowed.bool) { // If the effect is allowed to resolve further
                        // get the simultaneous changes that need to resolve
                        if (i == 0) {
                            // If we're in the cost, then look at the cost of the effect
                            List<EffectChangeInstance> cost = effect.effect.getInstanceCost();
                            int index = n - effect.costStart; // The simultaneous effect change, minus where the effect started
                            if (cost.size() > index) {
                                EffectChangeInstance currentChange = cost.get(index);
                                List<ContinuousInstance> activeContinuous = getActiveOptionalContinuous(currentChange, activeConstantContinuous);
                                if (isEffectLegal(currentChange, activeContinuous)) {
                                    final List<List<GameChoiceChange>> choices = getChoiceChanges(currentChange, activeContinuous);
                                    if (choices.isEmpty()) { // If there's no choice for a cost, then a cost cannot be paid, so the effect cannot be performed.
                                        effect.furtherResolutionAllowed.bool = false;
                                    }
                                    effectsToResolve.add(getPlayerChangeChoice(
                                        sideIDtoController(currentChange.getUser().getUser()),
                                        choices
                                    ));
                                } else {
                                    effect.furtherResolutionAllowed.bool = false; // A cost was illegal, so it cannot be paid
                                }
                            }
                        } else {
                            // If we're in the effect, then look at the effect
                            List<EffectChangeInstance> effectPortion = effect.effect.getInstanceEffectChanges();
                            int index = n - effect.effectStart;
                            if (effectPortion.size() > index) {
                                EffectChangeInstance currentChange = effectPortion.get(index);
                                List<ContinuousInstance> activeContinuous = getActiveOptionalContinuous(currentChange, activeConstantContinuous);
                                if (isEffectLegal(currentChange, activeContinuous)) {
                                    effectsToResolve.add(getPlayerChangeChoice(
                                            sideIDtoController(currentChange.getUser().getUser()),
                                        getChoiceChanges(currentChange, activeContinuous
                                        )
                                    ));
                                }
                            }
                        }
                    }
                }
                // If there are no effects
                if (effectsToResolve.isEmpty()) {
                    if (i == 1) { // Break if we're past the effect
                        break;
                    }
                    else { // Otherwise, before the cost goes to the effect, check the unshroud window.
                        state.timingPoint = TimingPoint.U0;
                        state.isBetweenEffects = true;
                        List<GameChoice> metEffects = getGameChoices(getAvailableEffects(activeConstantContinuous), activeConstantContinuous);
                        if (!metEffects.isEmpty()) { // Effect(s) during the unshroud window were met
                            for (GameChoice met : metEffects) {
                                effects.add( // So add those effects whose costs must still be paid
                                    // We're in the cost still, so offset the cost.
                                    new GameEffect(n + 1, 0, met.getEffect(), new Bool(true)) // +1 since it continues
                                );
                            }
                            state.isBetweenEffects = false;
                            continue;
                        }
                        else { // No unshroud effects left
                            state.isBetweenEffects = false;
                            state.timingPoint = originalTiming;
                            break;
                        }
                    }
                }
                // Then resolve chosen effects
                List<GameEvent> changeEffectList = new ArrayList<>();
                resolveGameChange(effectsToResolve, changeEffectList);
                resolveLogAdding(changeEffectList);
                // Resolve effects after each effect change
                List<GameEvent> afterChangeEffectList = new ArrayList<>();
                resolveAfterChange(afterChangeEffectList);
                resolveLogAdding(afterChangeEffectList);
                // Get effects which can be met between effects
                state.isBetweenEffects = true;
                List<ContinuousInstance> c = getActiveContinuous(); // Continuous after resolution
                List<GameChoice> metEffects = getGameChoices(getAvailableEffects(c), c);
                for (GameChoice met : metEffects) {
                    effects.add(
                        // If we're in the cost offset the cost, if we're in the effect offset the effect.
                        new GameEffect(i == 0 ? n : 0, i == 0 ? 0 : n, met.getEffect(), new Bool(true))
                    );
                }
                state.isBetweenEffects = false;
            }
        }
        List<GameEvent> afterEffectList = new ArrayList<>();
        resolveAfterEffect(afterEffectList);
        resolveLogAdding(afterEffectList);
    }

    /**
     * Given a List List gameChoiceChange in priority order,
     * where the first List is every Simultaneous Effect,
     * and the second List holds a single change,
     * resolves that change's effect on the gamestate and adds events to the log which include information from that event,
     * to be used for String conversion.
     * All changes passed to this event are resolved.
     * Do not pass choice generation to this function - that would resolve every possible choice.

     * Adds the log events to the given List GameEvent. After calling this function, always call resolveLogAdding on the full eventList.
     */
    private void resolveGameChange(List<List<GameChoiceChange>> choice, List<GameEvent> eventList) {
        // Sort the list based off of simult num
        // The first change in every effect has the base effectChangeType, which is what's written on the card
        choice.sort(Comparator.comparing(effect -> effect.get(0).getChange().getType().getSimultNum()));

        // For every written change in the list
        for (List<GameChoiceChange> singleEffect : choice) for (GameChoiceChange change : singleEffect) {
            final List<ContinuousInstance> activeContinuous = getActiveOptionalContinuous(change.getChange(), getActiveContinuous());

            // It's the match making the change.
            if (change.getChange().getOwner() == null) {
                resolveSingleChange(change, eventList, activeContinuous);
            }
            // It's a LivingObject making the change. Echo Counters not implemented.
            else if (checkChangeConditions(change, activeContinuous, change.getChange().getEffectID())) {
                resolveSingleChange(change, eventList, activeContinuous);
            } else { // The living object's effect resolution conditions weren't met.
                eventList.add(new GameEvent(state,
                    change.getChange(),
                    null,
                    null,
                    change.getChange().getUser().getCommanderOneVis(),
                    change.getChange().getUser().getCommanderTwoVis()
                ));
            }
        }
    }

    /** Resolve a GameChoiceChange's effect on the gamestate. This includes system changes as well as  */
    private void resolveSingleChange(GameChoiceChange change, List<GameEvent> eventList, List<ContinuousInstance> activeContinuous) {
        final EffectChangeInstance effectChange = change.getChange();
        final List<? extends LivingObject> changeTargets = change.getTarget();
        final LivingObject user = effectChange.getUser();
        final EffectChangeType currentType = effectChange.getType();
        final List<Integer> info = change.getChoiceInfo();

        if (change.getChange().getOwner() == null) {
            switch (currentType) {
                /* System Changes (Not checked for legality in performing them. */
                case matchStart -> {
                    state.gameGoing = true;
                    state.opportunityHolder = SideID.ONE;

                    eventList.add(new GameEvent(state, effectChange));
                }

                case matchEnd -> {
                    state.gameGoing = false;

                    eventList.add(new GameEvent(state, effectChange));
                }

                case setProperOpportunity -> {
                    if (state.turn % 2 == 1) { // The turn counter starts at 1, so on add turns, commander 1 has opportunity
                        state.opportunityHolder = SideID.ONE;
                    }
                    else state.opportunityHolder = SideID.TWO;

                    eventList.add(new GameEvent(state, effectChange, List.of()));
                }

                // Repeats code from draw as this is a different draw than the effect draw
                // "If this card is drawn" still refers to this, but something may refer to "If this card is drawn by an effect"
                /*
                 Len 0 = it's the first turn, so no draw occurred
                 Len 1 = a token was drawn, so no draw occurred, and the event targets that drawn token
                 Len 2 = {
                    [0] = 0, a draw was successful, and the commander took damage
                    [0] != 0, the commanders deck was empty, and they thus took [0] damage. [1] has no use.
                 }
                 */
                case drawPhaseDraw -> {
                    // Doesn't draw on the first turn
                    if (state.turn == 1) {
                        eventList.add(new GameEvent(state, effectChange));
                    }
                    else {
                        for (CommanderInstance commander : state.getCommanders()) {
                            if (!commander.getDeck().contents.isEmpty()) {
                                final CardInstance drawn = commander.getDeck().contents.get(0);
                                if (!drawn.isTokenInstance()) {
                                    state.moveCard(drawn, commander.getHand().position);
                                    eventList.add(new GameEvent(state, effectChange, commander, List.of(
                                        0,  // Damage dealt to commander from excess draws
                                        drawn.getInstanceID() // Drawn card ID, as the target is the commander
                                    ), commander.getCommanderOneVis(), commander.getCommanderTwoVis()));
                                }
                                else {
                                    eventList.add(new GameEvent(state, effectChange, drawn, List.of(
                                        0 // Card removed from game
                                        // Further IDs would represent cardIDs, but the eventList now targets the drawn card.
                                    ), commander.getCommanderOneVis(), commander.getCommanderTwoVis()));
                                    systemRecursiveChange(EffectChangeType.removeFromGame, drawn, List.of(), eventList);
                                }

                            }
                            else {
                                final int drawMulti = 2; // Dealt 2 damage for each excess card drawn. May change from continuous.
                                commander.setHealth(commander.getHealthMax() - drawMulti);
                                eventList.add(new GameEvent(state, effectChange, commander, List.of(
                                    drawMulti, // Damage dealt to commander from excess draws
                                    0 // Further IDs represent card IDs of those that were drawn, 0 as there are none
                                ), commander.getCommanderOneVis(), commander.getCommanderTwoVis()));
                            }
                        }
                    }
                }

                case gainCollectionMaterial -> {
                    for (CommanderInstance commander : state.getCommanders()) {
                        final int commanderGain = commander.getCollectionBase() + getCollectionMaterial(commander, activeContinuous);
                        final int commanderOriginal = commander.getMaterial();
                        final int commanderNew = commanderOriginal + commanderGain;
                        commander.setMaterial(commanderNew);

                        eventList.add(new GameEvent(state, effectChange, commander,
                            List.of(
                                commanderOriginal, // Commander's original Material
                                commanderGain,     // Commander's gained Material
                                commanderNew       // Commander's new Material
                            ), commander.getCommanderOneVis(), commander.getCommanderTwoVis())
                        );
                    }
                }

                case beginDrawPhase -> {
                    state.phase = Phase.DRAW;
                    state.timingPoint = TimingPoint.D0;

                    eventList.add(new GameEvent(state, effectChange));
                }


                case endDrawPhase -> {
                    state.timingPoint = TimingPoint.D1;

                    eventList.add(new GameEvent(state, effectChange));
                }

                case beginBuildPhase -> {
                    state.phase = Phase.BUILD;
                    state.timingPoint = TimingPoint.B0;

                    eventList.add(new GameEvent(state, effectChange));

                    // Frozen Counter Removal
                    systemRecursiveChange(EffectChangeType.frozenRemoval, state.getLivingObjects(l -> l.hasCounter(CounterType.Frozen)), List.of(1), eventList);

                    // Ethereal Doubling. Displacement is handled by ResolveAfterChange.
                    systemRecursiveChange(EffectChangeType.etherealDouble, state.getLivingObjects(l -> l.hasCounter(CounterType.Ethereal)), List.of(), eventList);
                }

                case commanderOpportunity -> {

                }

                case frozenRemoval -> {
                    for (LivingObject obj : changeTargets) {
                        final int originalFrozenCounters = Math.toIntExact(obj.getCounters().stream().filter(counter -> counter == CounterType.Frozen).count());
                        obj.removeCounter(CounterType.Frozen, 1);
                        final int frozenCounters = Math.toIntExact(obj.getCounters().stream().filter(counter -> counter == CounterType.Frozen).count());

                        eventList.add(new GameEvent(state,
                            effectChange,
                            obj,
                            List.of(
                                originalFrozenCounters, // Original number of Frozen Counters
                                frozenCounters // New number of Frozen Counters
                            ),
                            obj.getCommanderOneVis(),
                            obj.getCommanderTwoVis()
                        ));
                    }
                }

                case etherealDouble -> {
                    for (LivingObject obj : changeTargets) {
                        final int originalEthereal = Math.toIntExact(obj.getCounters().stream().filter(counter -> counter == CounterType.Ethereal).count());
                        obj.addCounter(CounterType.Ethereal, originalEthereal);
                        final int newEthereal = Math.toIntExact(obj.getCounters().stream().filter(counter -> counter == CounterType.Ethereal).count());

                        eventList.add(new GameEvent(state,
                            effectChange.returnAndSetTypeAndStrengthInstance(EffectChangeType.etherealDouble, VariableGameNum.Num(originalEthereal)),
                            obj,
                            List.of(
                                originalEthereal, // Original number of Ethereal Counters
                                newEthereal // New number of Ethereal Counters
                            ),
                            obj.getCommanderOneVis(),
                            obj.getCommanderTwoVis()
                        ));
                    }
                }

                case endBuildPhase -> {
                    state.timingPoint = TimingPoint.B3;

                    eventList.add(new GameEvent(state, effectChange));
                }

                case beginCombatPhase -> {
                    state.phase = Phase.COMBAT;
                    state.timingPoint = TimingPoint.C0;

                    eventList.add(new GameEvent(state, effectChange));
                }

                case commanderLaneSelection -> {
                }

                case commanderActionListSelection -> {
                    // Choice between available actions in chosen lane (that must be checked for here) and
                }

                case doNextAction -> {

                }

                case endCombatPhase -> {
                    state.timingPoint = TimingPoint.C4;
                    eventList.add(new GameEvent(state, effectChange));

                    // Flame Counters
                    systemRecursiveChange(EffectChangeType.flameRemoval, state.getLivingObjects(l -> l.hasCounter(CounterType.Flame)), List.of(), eventList);

                }

                case flameRemoval -> {
                    for (LivingObject obj : changeTargets) {
                        final int damage = Math.toIntExact(obj.getCounters().stream().filter(counter -> counter == CounterType.Flame).count());
                        final int newHealth = obj.getHealth() - damage;
                        obj.setHealth(newHealth);

                        final int flameToRemove = Math.toIntExact(obj.getCounters().stream().filter(counter -> counter == CounterType.Flame).count() / 2); // rounds down
                        obj.removeCounter(CounterType.Flame, flameToRemove);

                        eventList.add(new GameEvent(state,
                            effectChange.returnAndSetTypeInstance(EffectChangeType.flameRemoval),
                            obj,
                            List.of(
                                damage, // Damage from FlameCounters
                                newHealth, // New health of the thing
                                flameToRemove // Flame counters removed
                            ),
                            obj.getCommanderOneVis(),
                            obj.getCommanderTwoVis()
                        ));
                    }
                }

                case beginAftermathPhase -> {
                    state.phase = Phase.AFTERMATH;
                    state.timingPoint = TimingPoint.A0;

                    eventList.add(new GameEvent(state, effectChange));
                    resolveTriggerChains();
                }

                case commanderAftermathMovement -> {

                }

                case checkForWin -> {

                }

                case endAftermathPhase -> {
                    state.timingPoint = TimingPoint.A2;

                    eventList.add(new GameEvent(state, effectChange));

                    // Aftermath Discard
                    for (CommanderInstance commander : state.getCommanders()) {
                        final int maxHandSize = getMaxHandSize(commander, activeContinuous);
                        final int currentSize = commander.getHand().contents.size();
                        if (currentSize > maxHandSize) {
                            systemRecursiveChange(
                                getPlayerChangeChoice(
                                    sideIDtoController(commander.getSide()),
                                    getChoiceChanges(effectChange.returnAndSetTypeAndStrengthInstance(
                                            EffectChangeType.aftermathDiscard,
                                            VariableGameNum.Num(currentSize - maxHandSize)
                                        ),
                                        activeContinuous)
                                ).get(0),
                                eventList
                            );
                        }
                    }
                }

                // Same effect as "Discard", but cards that check for being "Discard"-ed don't check for being "AftermathDiscard"-ed
                case aftermathDiscard -> {

                }

                // Ticks a duration, decreasing the base value of it's VariableGameNum by 1.
                case tickDuration -> {
                    // Find the duration
                    final int durationEffectID = change.getChoiceInfo().get(0);
                    for (ContinuousInstance duration : state.getDurationContinuous()) {
                        if (duration.effect().getDurationInstance().getEffectID() == durationEffectID) {
                            final int oldDuration = duration.effect().getDurationInstance().getStrengthInstance()
                                .getValue(state, sideIDtoController(duration.effect().getDurationInstance().getUser().getUser()),
                                    effectChange, activeContinuous);

                            // tick the duration by lowering its base value by 1
                            duration.effect().getDurationInstance().setStrengthInstance(
                                VariableGameNum.CopyDifferentBase(
                                    duration.effect().getDurationInstance().getStrengthInstance(),
                                    duration.effect().getDurationInstance().getStrengthInstance().baseValue - 1)
                            );

                            final int newDuration = duration.effect().getDurationInstance().getStrengthInstance()
                                .getValue(state, sideIDtoController(duration.effect().getDurationInstance().getUser().getUser()),
                                    effectChange, activeContinuous);

                            eventList.add(new GameEvent(state, effectChange,
                                List.of(
                                    durationEffectID, // Duration ID
                                    oldDuration, // old tick num
                                    newDuration // new tick num
                                )
                            ));

                            // If the duration is now <= 0
                            if (newDuration <= 0) {
                                // Remove that duration
                                systemRecursiveChange(EffectChangeType.removeDuration, List.of(
                                    durationEffectID // EffectID of the duration to remove.
                                ), eventList);
                            }
                            break;
                        }
                    }
                }

                case removeDuration -> {
                    final int durationEffectID = change.getChoiceInfo().get(0);
                    for (int i = 0; i < state.getDurationContinuous().size(); i++) {
                        if (state.getDurationContinuous().get(i).effect().getDurationInstance().getEffectID() == durationEffectID) {
                            state.getDurationContinuous().remove(i);
                            eventList.add(new GameEvent(state, effectChange,
                                List.of(
                                    durationEffectID // ID of duration that was removed
                                )
                            ));
                            break;
                        }
                    }
                }

                case targetUnshroud -> {
                    final LivingObject target = changeTargets.get(0);
                    boolean initialShrouded = target.isShrouded();
                    if (target.isShrouded()) target.setShrouded(false);
                    boolean afterShrouded = target.isShrouded();
                    eventList.add(new GameEvent(state, effectChange,
                        List.of(
                            initialShrouded ? 1 : 0, // The shrouded state before, 1 for shrouded, 0 if not
                            afterShrouded ? 1 : 0 // The shrouded state after, 1 for shrouded, 0 if not
                        ),
                        target.getCommanderOneVis(),
                        target.getCommanderTwoVis()
                    ));
                }

                default -> throw new IllegalArgumentException("System change (" + currentType + ") is not implemented.");
            }
        } else for (LivingObject target : changeTargets) {
            final BoardLocation boardLocation = target.getPosition().getBoardLocation();
            switch (currentType) {
                case UniqueTarget, MultiTarget, RandomMultiTarget, RandomUniqueTarget, TargetAll,
                     AdjacentTarget, AdjacentToOriginalTarget, NonAdjacentTarget, SameLaneTarget,
                     SameFileTarget -> {
                    if (boardLocation == BoardLocation.FIELD
                        || boardLocation == BoardLocation.COMMANDER_ONE
                        || boardLocation == BoardLocation.COMMANDER_TWO) {
                        // resolves to onFieldTarget. May unshroud a card if the effect will damage.
                        user.addTarget(new TargetInstance(target.getPosition()));
                        if (target.isShrouded() && isDamagingToTarget(effectChange.getEffectFrom())) {
                            systemRecursiveChange(EffectChangeType.targetUnshroud, List.of(), eventList);
                        }
                    } else {
                        // resolves to an offFieldTarget
                        user.addTarget(new TargetInstance(target.getPosition(), target.getInstanceID()));
                    }

                    eventList.add(new GameEvent(state,
                        effectChange,
                        target,
                        List.of(), // No other information needed
                        user.getCommanderOneVis(),
                        user.getCommanderTwoVis()
                    ));
                }

                case Place, PlaceAsToken, PlaceWith1Health,
                     PlaceAdjacentToThisCard, PlaceOnTarget, PlaceWithNoCost, PlaceWithNoCostOnTarget,
                     PlaceShrouded, PlaceMinusStrengthMaterialMin0, PlaceMinusStrengthMaterialNoMin,
                     PlaceAnyAmount,

                     PlaceConspiracyToken, PlaceTreeToken, PlaceAirToken, PlaceScrapToken, PlaceSporeToken,
                     PlaceShrubToken, PlaceSpiritToken, PlacePilotFishToken, PlaceHillToken, PlaceEchoToken,
                     PlaceRatToken, PlaceDemonToken, PlaceImitationToken, PlaceLabSubject00Token, PlaceSoulToken,
                     PlaceOpenMindToken, PlacePureEnergyToken, PlaceTravestyToken, PlaceTargetNameToken,

                     PlaceUsingHand, PlaceUsingYourCardsOnField, PlaceUsingDestroyed, PlaceUsingDiscard,
                     PlaceUsingDisplaced, PlaceUsingDeck, PlaceUsingDecisivePile, PlaceUsingCardsOnField,
                     PlaceUsingOpponentsCardsOnField, PlaceUsingTarget, PlaceUsingZonesYouControl,
                     PlaceUsingHandOrWithEffect -> {
                    final ZoneInstance zonePlacedOn = state.field.getZone(info.get(0)); // Zone placed on
                    final int numCardsUsed = info.get(3); // Number of cards used

                    // Get what the target does to used cards
                    final List<EffectChangeInstance> usedChanges = getAllUsedChange(target, activeContinuous);

                    // Info after index 4 is all of the used cards.
                    for (int i = 5; i <= 4 + numCardsUsed; i++) {
                        // Do it to the used cards
                        for (EffectChangeInstance usedChange : usedChanges) {
                            resolveGameChange(
                                Collections.singletonList(getPlayerChangeChoice(
                                    sideIDtoController(user.getUser()),
                                    getChoiceChangesOfTarget(usedChange, state.getCard(info.get(i)), activeContinuous))),
                                eventList);
                        }
                    }

                    // Resolve the place cost of the card being placed. This includes any material cost to be used.
                    // Cards have a "Self Placement Effect" which, when resolved, places the card
                    resolveEffect(target.getInstancedPlaceCost());
                    try {
                        state.moveCard(ensureMatches(CardInstance.class, target).get(0), state.field.getZone(info.get(0)).getPosition());
                        // Effects that occur after the placement, determined by the effectChangeType.
                        // The effect was checked for legality before, so this is fully fine.
                        switch (currentType) {
                            case PlaceShrouded -> {
                                target.setShrouded(true);
                            }
                            case PlaceAsToken -> {
                                target.setTokenState(true);
                            }
                            case PlaceWith1Health -> {
                                target.setHealth(1);
                            }
                        }

                        // This is just the event of placing the card, paying costs make their own events
                        eventList.add(new GameEvent(state,
                            effectChange,
                            target, // Targets the card that is placed
                            List.of(
                                zonePlacedOn.getInstanceID(), // ID of zone placed on
                                numCardsUsed                  // Number of cards used (their events are added first)
                            ),
                            target.getCommanderOneVis(),
                            target.getCommanderTwoVis()
                        ));
                    } catch (IllegalStateException e) { // Thrown by state when something goes wrong with a movement
                        eventList.add(new GameEvent(state,
                            effectChange,
                            target,
                            List.of(
                                -1, // Illegal placement
                                numCardsUsed
                            ),
                            false,
                            false
                        ));
                    }
                }
                case selfPlacementEffect -> {} // This is only for the information of the targetType of available zones the card can be plced on.

                case PayMaterial -> {
                    final CommanderInstance usingCommander = state.getCommander(change.getChange().getUser().getUser());
                    final int originalMaterial = usingCommander.getMaterial();
                    final int materialOriginallyPaid;
                    final int materialActuallyPaid;
                    final int remainingMaterial;
                    if (info.size() == 1) { // Normal payment effect
                        materialOriginallyPaid = info.get(0);
                        materialActuallyPaid = materialOriginallyPaid; // Add continuous checks here
                        remainingMaterial = originalMaterial - materialActuallyPaid;
                    } else { // Payment effect as a place cost
                        // Add 1 to indexes as info is added on top of the base, which was already calculated.
                        materialOriginallyPaid = info.get(2) + info.get(5); // Base material + material bonus from cards used
                        materialActuallyPaid = info.get(3) + info.get(5); // Base material with discount + material bonus from cards used
                        remainingMaterial = originalMaterial - materialActuallyPaid;
                    }

                    usingCommander.setMaterial(remainingMaterial);

                    eventList.add(new GameEvent(state,
                        effectChange,
                        usingCommander,
                        List.of(
                            originalMaterial,             // Commander's original material
                            materialOriginallyPaid,       // The material that would be paid without discount
                            materialActuallyPaid,         // The material that was paid with discount
                            remainingMaterial             // The material remaining after material removed from commander
                        ),
                        usingCommander.getCommanderOneVis(),
                        usingCommander.getCommanderTwoVis()
                    ));
                }

                case Damage -> {
                    if (boardLocation != BoardLocation.FIELD) // Damage cannot be done on things not on the field.
                        throw new IllegalStateException(target + " isn't on the field to be dealt damage.");
                    int damage = info.get(0);
                    final int startingDamage = damage;

                    // Fortification Counters
                    int damageReduced = 0;
                    if (effectChange.getEffectFrom().getType() == EffectType.Action) {
                        while (damage > 1 && target.getCounters().contains(CounterType.Fortification)) {
                            target.removeCounter(CounterType.Fortification, 1);
                            damage--;
                            damageReduced++;
                        }
                    }
                    final int newFortification = Math.toIntExact(target.getCounters().stream().filter(c -> c == CounterType.Fortification).count());

                    final int newHealth = target.getHealthMax() - damage;
                    target.setHealth(newHealth);

                    eventList.add(new GameEvent(state,
                        effectChange,
                        target,
                        List.of(
                            startingDamage, // original damage
                            damage, // actual damage
                            newHealth, // new health of the thing
                            damageReduced, // damage reduced by fortification
                            newFortification // new fortification counters of the thing
                        ),
                        target.getCommanderOneVis(),
                        target.getCommanderTwoVis()
                    ));
                }

                case Restore -> {
                    if (boardLocation != BoardLocation.FIELD) // Restoration cannot be done to things nots on the field.
                        throw new IllegalStateException(target + " isn't on the field to be restored.");
                    int restoration = info.get(0);
                    final int startingRestoration = restoration;

                    // Infection Counters
                    final int reverseRestoration = Math.abs(restoration) * -1;
                    int infectionReduction = 0;
                    while (restoration >= reverseRestoration && target.getCounters().contains(CounterType.Infection)) {
                        target.removeCounter(CounterType.Infection, 1);
                        restoration--;
                        infectionReduction++;
                    }

                    final int newHealth = target.getHealthMax() + restoration;
                    target.setHealth(newHealth);

                    eventList.add(new GameEvent(state,
                        effectChange,
                        target,
                        List.of(
                            startingRestoration, // original restoration
                            restoration, // actual restoration
                            newHealth, // new health of the thing
                            infectionReduction // restoration reduced by infection
                        ),
                        target.getCommanderOneVis(),
                        target.getCommanderTwoVis()
                    ));
                }

                // The target is sent to the discard pile. Before, assumedly, it was checked that the target is in the hand.
                case Discard -> {

                }

                case AddBountyCounters, AddScrapCounters, AddTideCounters, AddRageCounters, AddSturdyCounters,
                     AddSiGNLCounters, AddTravelCounters, AddFervorCounters, AddStorageCounters,
                     AddUndeadCounters,
                     AddVirtueCounters, AddSinCounters, AddDreamCounters, AddInjectionCounters, AddVeilCounters,
                     AddOverchargeCounters, AddFortificationCounters, AddFrozenCounters, AddFlameCounters,
                     AddInfectionCounters, AddEtherealCounters, AddMomentumCounters, AddDamageEcho,
                     AddRestoreEcho -> {
                    CounterType counterType = CounterType.convertCounter(currentType);
                    final int originalCounters = Math.toIntExact(target.getCounters().stream()
                        .filter(counter -> counter == counterType).count());
                    target.addCounter(counterType, info.get(0));
                    final int newCounters = Math.toIntExact(target.getCounters().stream().filter(counter -> counter == counterType).count());

                    eventList.add(new GameEvent(state,
                        effectChange,
                        target,
                        List.of(
                            originalCounters, // Original number of __ Counters
                            newCounters // New number of __ Counters
                        ),
                        target.getCommanderOneVis(),
                        target.getCommanderTwoVis()
                    ));
                }

                case RemoveEnergyCounters, RemoveBountyCounters, RemoveScrapCounters, RemoveTideCounters,
                     RemoveRageCounters, RemoveSturdyCounters, RemoveSiGNLCounters, RemoveTravelCounters,
                     RemoveFervorCounters, RemoveStorageCounters, RemoveUndeadCounters, RemoveVirtueCounters,
                     RemoveSinCounters, RemoveDreamCounters, RemoveInjectionCounters, RemoveVeilCounters,
                     RemoveOverchargeCounters, RemoveFortificationCounters, RemoveFrozenCounters,
                     RemoveFlameCounters,
                     RemoveInfectionCounters, RemoveEtherealCounters, RemoveMomentumCounters, RemoveDamageEcho,
                     RemoveRestoreEcho -> {
                    CounterType counterType = CounterType.convertCounter(currentType);
                    final int originalCounters = Math.toIntExact(target.getCounters().stream()
                        .filter(counter -> counter == counterType).count());
                    target.removeCounter(counterType, info.get(0));
                    final int newCounters = Math.toIntExact(target.getCounters().stream().filter(counter -> counter == counterType).count());

                    eventList.add(new GameEvent(state,
                        effectChange,
                        target,
                        List.of(
                            originalCounters, // Original number of __ Counters
                            newCounters // New number of __ Counters
                        ),
                        target.getCommanderOneVis(),
                        target.getCommanderTwoVis()
                    ));
                }

                default -> throw new IllegalArgumentException("EffectChangeType (" + currentType + ") is not implemented.");
            }
            // If the effect has a duration
            int durationTime = effectChange.getDurationInstance().getStrength().getValue(state, sideIDtoController(user.getUser()), effectChange, target, activeContinuous);
            if (durationTime >= 0) {
                // Add it to the list of effects with duration, which is a Continuous.
                ContinuousInstance fullDuration = new ContinuousInstance(effectChange, List.of(target), info);
                state.getDurationContinuous().add(fullDuration);
            }
        }
    }

    /** Given a list of every simultaneous event, adds that event to the room's eventlist and the state's justHappened. */
    private void resolveLogAdding(List<GameEvent> eventList) {
        room.getLog().addEvent(eventList);
        state.addJustHappened(eventList);
    }

    /**
     * Given a list of ActiveContinuous, resolves effects on the gamestate that occur after each effect via systemChange.
     * a) Removing "Used by" from cards;
     * b) Shuffling decks that have been searched / excavated;
     * c) Resetting searched / excavated / usedBy state;
     * d) Resetting x / y / declared values on living objects;

     * All of these happen "simultaneously"; that is - continuous are not updated in between.
     */
    private void resolveAfterEffect(List<GameEvent> eventList) {
        List<ContinuousInstance> c = getActiveContinuous();
        resolveAfterChange(eventList);


        resolveAfterChange(eventList);
    }

    /**
     * Resolves effects on the gamestate that occur after each effectChange via systemChange and adds those events to the eventList.
     * a) Ticking / ending duration which have their condition met, and removing incompatible duration on the same object (keeping recent);
     * b) Updating the user, controller, and owner; the negation; the name; the effects ; the [instanced attributes];
     * c) Displacing cards who have more Ethereal Counters than health;
     * d) Moving the position fieldState of rode cards who no longer have a rider to ZONE rather than RODE;
     * e) Setting CommanderVis per livingObject;

     * All of these happen "simultaneously", but continuous are updated in between.
     */
    private void resolveAfterChange(List<GameEvent> lastSimultaneous) {
        // c, ethereal displacement
        for (LivingObject l : state.getLivingObjects()) {
            {
                final int counters = Math.toIntExact(l.getCounters().stream().filter(e -> e == CounterType.Ethereal).count());
                if (counters > l.getHealthMax()) {
                    if (l instanceof CommanderInstance) {
                        systemRecursiveChange(EffectChangeType.etherealHalve, l, List.of(), lastSimultaneous);
                    } else {
                        systemRecursiveChange(EffectChangeType.etherealDisplace, l, List.of(), lastSimultaneous);
                    }
                    // Zones cannot gain counters.
                }
            }
        }

        // d, ride state
        {
            final List<ContinuousInstance> c = getActiveContinuous();
            final Consumer<CardInstance> rideDestroy = card ->
                systemRecursiveChange(EffectChangeType.rideDestruction, card, List.of(), lastSimultaneous);

            for (ZoneInstance zone : state.getZones()) {
                List<CardInstance> invalidCards = getInvalidZoneCards(zone, c);

                // All cards in invalidCards are illegal. This makes resolution much easier.
                while (!invalidCards.isEmpty()) {
                    CardInstance card = invalidCards.get(0);
                    final long cardCount = zone.getCards().stream().filter(CardInstance::isOccupying).count();
                    switch (card.getPosition().getRideState()) {
                        case NORMAL -> {
                            List<ContinuousInstance> rideable = zone.getCards().stream()
                                .map(z -> getAvailableRideableContinuous(card, z, c)).flatMap(List::stream).toList();
                            if (!rideable.isEmpty()) systemRecursiveChange(EffectChangeType.forcedRide, card, List.of(
                                rideable.get(0).effect().getUser().getInstanceID() // The card to be forced to ride
                            ), lastSimultaneous);

                            else rideDestroy.accept(card);
                        }
                        case RIDER -> {
                            if (cardCount == 1) systemRecursiveChange(EffectChangeType.dismount, card, List.of(
                                0 // 0 meaning this is just to dismount the rider, and not set its rode card.
                            ), lastSimultaneous);

                            else rideDestroy.accept(card);
                        }
                        case RODE -> {
                            if (cardCount == 1) systemRecursiveChange(EffectChangeType.dismount, card.getThisCardsRider(),
                                List.of( // Dismount targets the card that was riding the Rode card.
                                    card.getInstanceID() // ID of the rode card which has the rider, to remove the rider from its list.
                                ), lastSimultaneous);

                            else rideDestroy.accept(card);
                        }
                    }
                    invalidCards = getInvalidZoneCards(zone, c);
                }
            }
        }

        // a, duration
        {
            // For every active duration
            List<ContinuousInstance> durations = new ArrayList<>();
            Collections.copy(durations, state.getDurationContinuous());
            for (int i = durations.size() - 1; i >= 0;  i--) {
                List<ContinuousInstance> c = getActiveContinuous();
                final ContinuousInstance duration = durations.get(i);

                if (!isEffectLegal(duration.effect().getDurationInstance(), c)) {
                    // If the duration isn't legal, then remove the duration from the list
                    systemRecursiveChange(EffectChangeType.removeDuration, List.of(
                        duration.effect().getDurationInstance().getEffectID() // EffectID of the duration to remove.
                    ), lastSimultaneous);
                } else if (isDurationTick(duration, lastSimultaneous, c, l -> true)) {
                    // Otherwise, if the duration needs to tick, tick it. Tick also removes duration that go to 0.
                    systemRecursiveChange(EffectChangeType.tickDuration, List.of(
                        duration.effect().getDurationInstance().getEffectID() // EffectID of the duration to tick.
                    ), lastSimultaneous);
                }
            }
        }
        // b, instanced attributes
        {
            List<ContinuousInstance> c = getActiveContinuous();
            resolveBaseGameState();
            resolveStateContinuous(lastSimultaneous, c);
            for (LivingObject l : state.getLivingObjects()) {
                resolveBaseLivingState(l);
                resolveControlUseOwner(l, lastSimultaneous, c);
                resolveNegation(l, lastSimultaneous, c);
                resolveName(l, lastSimultaneous, c);
                resolveLivingContinuous(l, lastSimultaneous, c);

                for (EffectInstance e : l.getInstancedEffects()) {
                    resolveNegation(e, lastSimultaneous, c);
                    resolveEffectInstance(e, lastSimultaneous, c);

                    for (ConditionInstance condition : e.getInstanceConditions()) {
                        resolveNegation(condition, lastSimultaneous, c);
                        resolveEffectConditionInstance(condition, lastSimultaneous, c);
                    }
                    for (EffectChangeInstance change : Stream.of(e.getInstanceCost(), e.getInstanceEffectChanges())
                        .flatMap(List::stream).toList()) {
                        resolveNegation(change, lastSimultaneous, c);
                        resolveEffectChangeInstance(change, lastSimultaneous, c);
                    }
                }
            }
        }

        // e, visibility. Must be last.
        // Nothing should check CommanderVisibility (they would check searched, excavated, revealed state instead),
        // so duration and attributes don't need to run afterwards
        for (LivingObject l : state.getLivingObjects()) {
            resolveVisibility(l);
        }
    }

    /** Sets a living object back to it's base state. */
    private void resolveBaseLivingState(LivingObject l) {
        l.setTypeInstance(l.getCardType()); // original type
        l.setName(l.getOriginalName()); // original name
        l.setAttributeInstance(l.getOriginalAttribute()); // original attribute
        l.setOwnerInstance(l.getOwner()); // original owner
        l.setHealthMaxInstance(l.getHealthMax()); // Original health max
    }

    /** Sets the state back to its non-instanced state. */
    private void resolveBaseGameState() {

    }

    /// Adds / removes effects from an EffectChange using activeContinuous.
    private void resolveEffectChangeInstance(EffectChangeInstance check, List<GameEvent> eventList, List<ContinuousInstance> activeContinuous) {
        final List<ContinuousInstance> changes = getEffectChangeContinuous(check, activeContinuous);
        for (ContinuousInstance change : changes) {
            switch (change.effect().getType()) {
                case EditOpenMindTokenToBeForAnyToken -> {
                    systemRecursiveChange(EffectChangeType.EditOpenMindTokenToBeForAnyToken, change.effect().getUser(),
                        List.of(check.getEffectID()), // EffectID of the effectChange to change (TargetType in effectChange OpenMindToken -> Token)
                        eventList
                    );
                }
                case YouWouldNegateACardAddThisCardToHand -> {
                    systemRecursiveChange(EffectChangeType.YouWouldNegateACardAddThisCardToHand, change.effect().getUser(),
                        List.of(check.getEffectID(), // EffectID of the effectChange to change
                            change.effect().getUser().getInstanceID()), // InstanceID of card to add to hand (Negate -> add the card to hand)
                        eventList
                    );
                }

                default -> throw new IllegalArgumentException("Effect Change Continuous (" + change.effect().getType() + ") is not implemented.");
            }
        }
    }
    /// Returns every continuous from the list that affects EffectChange.
    private List<ContinuousInstance> getEffectChangeContinuous(EffectChangeInstance check, List<ContinuousInstance> activeContinuous) {
        return getContinuousThatApplyOnObject(check.getUser(), activeContinuous, e -> {
            int s = e.effect().getType().getSimultNum();
            return s == -5 // Affects EffectChange

        ;});
    }

    /// Adds / remove effects from a Condition using activeContinuous.
    private void resolveEffectConditionInstance(ConditionInstance check, List<GameEvent> eventList, List<ContinuousInstance> activeContinuous) {
        final List<ContinuousInstance> changes = getConditionContinuous(check, activeContinuous);
        for (ContinuousInstance change : changes) {
            switch (change.effect().getType()) {
                case OneLessRequiredInOpenField -> {
                    if (check.getType() == ConditionType.MinThings && check.getTargetInstance().contains(TargetType.OpenFieldZones)) {
                        systemRecursiveChange(EffectChangeType.OneLessRequiredInOpenField, change.effect().getUser(),
                            List.of(check.getEffectID()), // EffectID of the condition to change (lower check's strength by 1)
                            eventList
                        );
                    }
                }

                case DamagingCardRequirementAutoMet -> {
                    if (check.getType() == ConditionType.CardDamaged) {
                        systemRecursiveChange(EffectChangeType.DamagingCardRequirementAutoMet, change.effect().getUser(),
                            List.of(check.getEffectID()), // EffectID of the condition to change (Change typeInstance to TRUE)
                            eventList
                        );
                    }
                }

                default -> throw new IllegalArgumentException("Effect Continuous (" + change.effect().getType() + ") is not implemented.");
            }
        }
    }
    /// Returns every continuous from the list that affects a Condition.
    private List<ContinuousInstance> getConditionContinuous(ConditionInstance check, List<ContinuousInstance> activeContinuous) {
        return getContinuousThatApplyOnObject(check.getUser(), activeContinuous, e -> {
            int s = e.effect().getType().getSimultNum();
            return s == -4 // Affects Condition

        ;});
    }

    /// Changes EffectInstance based on activeConstantContinuous.
    private void resolveEffectInstance(EffectInstance check, List<GameEvent> eventList, List<ContinuousInstance> activeContinuous) {
        final List<ContinuousInstance> changes = getEffectContinuous(check, activeContinuous);
        for (ContinuousInstance change : changes) {
            switch (change.effect().getType()) {
                case WhileDestroyedActivatableAlsoUsableWhileDiscarded -> {
                    if (check.type == EffectType.Activatable && check.getInstanceConditions().stream()
                        .anyMatch(c -> new HashSet<>(c.getTargetInstance()).containsAll(List.of(TargetType.Self, TargetType.CardsInDestroyedPile))))
                        systemRecursiveChange(EffectChangeType.WhileDestroyedActivatableAlsoUsableWhileDiscarded, change.effect().getUser(),
                            List.of(check.getEffectID()), // EffectID of the Effect to change (while destroyed -> while destroyed / discarded)
                            eventList
                        );
                }
                case TriggersCanBeUsedInHand -> {
                    if (check.type == EffectType.Trigger)
                        systemRecursiveChange(EffectChangeType.TriggersCanBeUsedInHand, change.effect().getUser(),
                            List.of(check.getEffectID()), // EffectID of the Effect to change (Position Condition Before -> condition / On Field)
                            eventList
                        );
                }
                case ActivatableCanBeUsedInHand -> {
                    if (check.type == EffectType.Activatable)
                        systemRecursiveChange(EffectChangeType.ActivatableCanBeUsedInHand, change.effect().getUser(),
                            List.of(check.getEffectID()), // EffectID of the Effect to change (Position Condition Before -> condition / On Field)
                            eventList
                        );
                }
                case IncreaseAttackDamage -> {
                    if (isAttack(check))
                        systemRecursiveChange(EffectChangeType.IncreaseAttackDamage, change.effect().getUser(),
                            List.of(check.getEffectID()), // EffectID of the Effect to change (increase base strength)
                            eventList
                        );
                }
                default -> throw new IllegalArgumentException("Effect Continuous (" + change.effect().getType() + ") is not implemented.");
            }
        }
    }
    /// Returns every continuous from the list that affects an EffectInstance.
    private List<ContinuousInstance> getEffectContinuous(EffectInstance check, List<ContinuousInstance> activeContinuous) {
        return getContinuousThatApplyOnObject(check.getUser(), activeContinuous, e -> {
            int s = e.effect().getType().getSimultNum();
            return s == -3 // Affects Effects
        ;});
    }

    /// Changes a LivingObject based on activeConstantContinuous.
    private void resolveLivingContinuous(LivingObject check, List<GameEvent> eventList, List<ContinuousInstance> activeContinuous) {
        final List<ContinuousInstance> changes = getLivingContinuous(check, activeContinuous);
        for (ContinuousInstance change : changes) {
            switch(change.effect().getType()) {
                case GainOverclocked -> { // Adding / removing effects is affecting the LivingObject, not the individual effect.
                    systemRecursiveChange(EffectChangeType.GainOverclocked, check, List.of(), eventList);
                }
                case GainImmobile -> {
                    systemRecursiveChange(EffectChangeType.GainImmobile, check, List.of(), eventList);
                }
                case RemoveEffect -> {
                    systemRecursiveChange(EffectChangeType.RemoveEffect, change.effect().getUser(),
                        List.of(change.effect().getEffectID()), // EffectID of the effect to change (remove)
                        eventList
                    );
                }
                default -> throw new IllegalArgumentException("Living Object Continuous (" + change.effect().getType() + ") is not implemented.");
            }
        }
    }
    /// Returns every continuous from the list that SPECIFICALLY affects the living object - not it's effects.
    private List<ContinuousInstance> getLivingContinuous(LivingObject check, List<ContinuousInstance> activeContinuous) {
        return getContinuousThatApplyOnObject(check, activeContinuous, e -> {
            int s = e.effect().getType().getSimultNum();
            return s == -2 // Affects livingObjects
                || s == 33 // Change target
                || s == 34 // Change / remove name
                || s == 35 // Change / remove Effect
                || s == 36 // Change attribute
                || s == 37 // Negate
                || s == 27 // Add effect
                || s == 26 // Add name
                || s == 25 // Change Control
                || s == 12 // Displace
                || s == 13 // Undisplace
        ;});
    }

    private void resolveStateContinuous(List<GameEvent> eventList, List<ContinuousInstance> activeContinuous) {
        final List<ContinuousInstance> changes = getStateContinuous(activeContinuous);
        for (ContinuousInstance change : changes) {
            switch(change.effect().getType()) {
                case Production -> {
                    systemRecursiveChange(EffectChangeType.Production, List.of(), List.of(), eventList);
                }
                case SkipCurrentPhase -> {
                    systemRecursiveChange(EffectChangeType.SkipCurrentPhase, List.of(), List.of(), eventList);
                }
                default -> throw new IllegalArgumentException("State Continuous (" + change + ") is not implemented.");
            }
        }
    }
    /// Returns every continuous from the list that affects the gamestate.
    private List<ContinuousInstance> getStateContinuous(List<ContinuousInstance> activeContinuous) {
        return activeContinuous.stream().filter(e -> {
            int s = e.effect().getType().getSimultNum();
            return s == -1  // Affects the GameState
                || s == 31 // Add phase
                || s == 32 // Skip phase
                || s == 33 // Remove phase
        ;}).toList();
    }

    private void resolveName(LivingObject set, List<GameEvent> eventList, List<ContinuousInstance> activeContinuous) {
        List<Integer> prefixes = new ArrayList<>();
        boolean nameRemoved = false;
        int nameSetTo = 0;
        for (ContinuousInstance nameChange : getContinuousThatApplyOnObject(set, activeContinuous,
            e -> e.effect().getType().getSimultNum() == 34)) { // Change / Remove Name
            switch(nameChange.effect().getType()) {
                case AddDoomedToName -> prefixes.add(nameToInt("Doomed"));
                case AddVolcanicToName -> prefixes.add(nameToInt("Volcanic"));
                case RemoveName -> {
                    nameRemoved = true;
                }
                case CopyTargetsName -> {
                    nameSetTo = nameChange.info().get(0);
                    for (int i = 1; i < nameChange.info().size(); i++) {
                        prefixes.add(nameChange.info().get(0));
                    }
                    nameRemoved = false;
                }
            }
        }
        if (nameRemoved) {
            if (!set.getNameInstance().isEmpty()) // If the name wouldn't be the same as before
                systemRecursiveChange(EffectChangeType.setName, set, List.of(
                    0 // ID of the baseName of copied card, 0 if none
                ), eventList);
        } else if (!set.getNameInstance().equals(resolveName(nameSetTo, prefixes))) // If the name wouldn't be the same as before
                systemRecursiveChange(EffectChangeType.setName, set,
                    Stream.concat(
                        Stream.of(nameSetTo), // ID of the baseName of copied card, 0 if none
                        prefixes.stream()).toList() // Remaining ID are all prefixes
                    , eventList);
    }

    private String resolveName(int baseName, List<Integer> prefixes) {
        StringBuilder builder = new StringBuilder(3);
        for (String string : prefixes.stream().map(this::integerToPrefix).toList()) builder.append(string);
        builder.append(integerToPrefix(baseName));
        return builder.toString();
    }

    /** Given an int from nameToInt, converts that int into it's original name / prefix.  */
    private String integerToPrefix(int nameID) {
        return switch (nameID) {
            case 0 -> "";
            case -1 -> "Doomed";
            case -2 -> "Volcanic";
            default -> CardDefinition.integerToName(nameID);
        };
    }

    /** Given a string, which is the name of a card or of a prefix, gives a number which represents it, specifically. */
    private int nameToInt(String name) {
        return switch(name) {
            case "" -> 0;
            case "Doomed" -> -1;
            case "Volcanic" -> -2;
            default -> CardDefinition.nameToInteger(name);
        };
    }

    private void resolveNegation(LivingObject set, List<GameEvent> eventList, List<ContinuousInstance> activeContinuous) {
        boolean negation = isAnyContinuousTypeOnObject(set, List.of(EffectChangeType.Negate, EffectChangeType.NegateAll), activeContinuous); // True if negated
        if (negation != set.isNegated()) {
            systemRecursiveChange(EffectChangeType.Negate, set, List.of(), eventList);
        }
    }

    private void resolveNegation(EffectObject set, List<GameEvent> eventList, List<ContinuousInstance> activeContinuous) {
        boolean negation = activeContinuous.stream().anyMatch(e -> e.effect().getType() == EffectChangeType.NegateEffect
            && !e.info().isEmpty()
            && e.info().get(0) == set.getEffectID()); // The effectID to negate
        if (negation != set.isNegated()) {
            systemRecursiveChange(EffectChangeType.NegateEffect, List.of(
                set.getEffectID() // ID of effect to negate
            ), eventList);
        }
    }

    /**
     * Given a LivingObject, updates it's commander 1 and commander 2 visibility according to it's state.
     * Deliberately does not use EffectChange to do this, as this is all that is needed to resolve visibility.
     * Continuous would change the revealed / excavated / searched state on the card itself.
     */
    private void resolveVisibility(LivingObject set) {
        // Sets owner vis to true and opponent vis to false
        Consumer<LivingObject> setOnlyOwnerVis = l -> {
            if (l.getUser() == SideID.ONE) {
                l.setCommanderOneVis(true);
                l.setCommanderTwoVis(false);}
            else {
                l.setCommanderOneVis(false);
                l.setCommanderTwoVis(true);}};

        // Conditions
        if (set.isRevealed() || set.isExcavated() || set.isSearched()) {
            set.setCommanderOneVis(true);
            set.setCommanderTwoVis(true);
        }
        else if (set.getPosition().getBoardLocation().isPublicLocation()) {
            if (set.isShrouded() || !set.isFaceUp()) {
                setOnlyOwnerVis.accept(set);
            }
            else {
                set.setCommanderOneVis(true);
                set.setCommanderTwoVis(true);
            }
        }
        else { // If in the deck, then the owner doesn't know where it is.
            if (BoardLocation.inDeck(set.getPosition().getBoardLocation())) {
                set.setCommanderOneVis(false);
                set.setCommanderTwoVis(false);
            }
            // If not in the deck, but isn't a public location (decisive deck / hand) then only the owner sees it.
            else setOnlyOwnerVis.accept(set);
        }
    }

    /** The resolved owner/controller/user of a living object after continuous effects. */
    private record SideResolution(SideID owner, SideID controller, SideID user) {}

    /** Given a LivingObject, updates its owner, control, and use instance state from continuous effects. */
    private void resolveControlUseOwner(LivingObject set, List<GameEvent> eventList, List<ContinuousInstance> activeContinuous) {
        final SideResolution r = resolveSides(set, activeContinuous, new HashSet<>());

        if (set.getOwner() != r.owner())
            systemRecursiveChange(EffectChangeType.setOwner, set,
                List.of(set.getOwner().ordinal(), r.owner().ordinal()), eventList);

        if (set.getController() != r.controller())
            systemRecursiveChange(EffectChangeType.setController, set,
                List.of(set.getController().ordinal(), r.controller().ordinal()), eventList);

        if (set.getUser() != r.user())
            systemRecursiveChange(EffectChangeType.setUser, set,
                List.of(set.getUser().ordinal(), r.user().ordinal()), eventList);
    }

    /** Resolves owner, then controller, then user for one object. */
    private SideResolution resolveSides(LivingObject check, List<ContinuousInstance> activeContinuous, Set<Integer> path) {
        if (!path.add(check.getInstanceID())) {
            final SideID base = check.getOwner();
            return new SideResolution(base, base, base);
        }
        try {
            final SideID owner = resolveOwner(check, activeContinuous, path);

            if (check.getPosition().getBoardLocation() != BoardLocation.FIELD) {
                // Off field: user is the pile's side, controller is nobody.
                final BoardLocation loc = check.getPosition().getBoardLocation();
                final SideID user = loc.isOne() ? SideID.ONE : loc.isTwo() ? SideID.TWO : SideID.NEUTRAL;
                return new SideResolution(owner, SideID.NEUTRAL, user);
            }

            final SideID controller = resolveController(check, owner, activeContinuous, path);
            return new SideResolution(owner, controller, controller); // on field, user tracks controller
        } finally {
            path.remove(check.getInstanceID()); // leave the path on the way out
        }
    }

    /** Resolves owner for one object. */
    private SideID resolveOwner(LivingObject check, List<ContinuousInstance> activeContinuous, Set<Integer> path) {
        final List<ContinuousInstance> ownerChanges = getContinuousThatApplyOnObject(check, activeContinuous,
            e -> e.effect().getType() == EffectChangeType.OwnerBecomesThisCardsUser)
            .stream()
            .sorted(Comparator.comparingInt((ContinuousInstance a) -> a.effect().getUser().getPriority()).reversed())
            .toList();
        if (ownerChanges.isEmpty()) return check.getOwner();
        // Highest-priority owner-change wins: owner becomes that effect's source's user.
        return resolveSides(ownerChanges.get(0).effect().getUser(), activeContinuous, path).user();
    }

    /** Resolves controller for one object. */
    private SideID resolveController(LivingObject check, SideID owner,
                                     List<ContinuousInstance> activeContinuous, Set<Integer> path) {
        final List<ContinuousInstance> controlChanges = getContinuousThatApplyOnObject(check, activeContinuous,
            e -> e.effect().getType() == EffectChangeType.ControllerBecomesThisCardsUser
                || e.effect().getType() == EffectChangeType.GainControl)
            .stream()
            .sorted(Comparator.comparingInt((ContinuousInstance a) -> a.effect().getUser().getPriority()).reversed())
            .toList();

        if (controlChanges.isEmpty()) return owner;    // no control effect → controlled by owner

        // A ControllerBecomesThisCardsUser effect overrides; highest priority wins.
        for (ContinuousInstance c : controlChanges) {
            if (c.effect().getType() == EffectChangeType.ControllerBecomesThisCardsUser) {
                return resolveSides(c.effect().getUser(), activeContinuous, path).user();
            }
        }

        // Otherwise tally GainControl effects.
        int tally = 0;
        for (ContinuousInstance c : controlChanges) {
            final SideID side = resolveSides(c.effect().getUser(), activeContinuous, path).user();
            if (side == SideID.ONE) tally++;
            else if (side == SideID.TWO) tally--;
        }
        if (tally > 0) return SideID.ONE;
        if (tally < 0) return SideID.TWO;
        return owner;
    }

    /* --- Legality Functions --- */
    /**
     * Returns a list of CardInstance that are the invalid cards in a zone, empty if the zone is legal.
     */
    private List<CardInstance> getInvalidZoneCards(ZoneInstance zone, List<ContinuousInstance> activeContinuous) {
        if (zone.getCards().isEmpty()) return List.of();
        final List<CardInstance> occupying = zone.getCards().stream()
            .filter(CardInstance::isOccupying)
            .sorted(Comparator.comparingInt(CardInstance::getPriority).reversed()).toList();

        // Attempt to not consider 1 rider and rode pair from occupying (the one with the higher priority rode)
        Map.Entry<List<CardInstance>, Boolean> entry = filterForRiderRode(occupying, zone, activeContinuous);
        if (entry.getValue()) {
            return entry.getKey(); // The remaining cards must be illegal, as a riderRode pair was removed.
        }
        // If the only occupant is a card with the NORMAL card ride state, then the zone is also legal.
        if (occupying.size() == 1 && occupying.get(0).getPosition().getRideState() == CardRideState.NORMAL) {
            return List.of();
        }
        /*
        If occupying contains any cards, they are illegal.
        This is because a zones legal states are only as follows:
            0 cards
            1 card with NORMAL ride state
            2 cards, one RIDER and 1 RODE whose getThisCardsRider() returns that RIDER
         All 3 have been checked for.
         */
        return occupying;
    }

    /**
     * Returns a List CardInstance with 1 rider & Rode pair removed from the given list of zoneCards,
     * or just the given list if no cards were removed.
     * Also returns a boolean for whether a rider rode pair was removed, true if it was.
     * @param zone only used for continuous checks
     */
    private Map.Entry<List<CardInstance>, Boolean> filterForRiderRode(List<CardInstance> zoneCards, ZoneInstance zone, List<ContinuousInstance> activeContinuous) {
        for (CardInstance card : zoneCards) {
            if (card.getPosition().getRideState() == CardRideState.RODE) {
                final CardInstance rider = card.getThisCardsRider();
                final List<CardInstance> riderRodeRemoved = zoneCards.stream()
                    .filter(sel -> !sel.equals(card) && !sel.equals(rider))
                    .toList();
                // If the list didn't change, then the rider is still there
                if (riderRodeRemoved.size() == zoneCards.size() - 2) {
                    return new AbstractMap.SimpleEntry<>(riderRodeRemoved, true);
                }
            }
        }
        // No card was removed.
        return new AbstractMap.SimpleEntry<>(zoneCards, false);
    }

    /** Returns every rideable continuous that the rider can use to ride the card */
    private List<ContinuousInstance> getAvailableRideableContinuous(CardInstance rider, CardInstance attemptedRode, List<ContinuousInstance> activeContinuous) {
        List<ContinuousInstance> rideables;
        if (attemptedRode.getThisCardsRider() == null
        && !(rideables = getRideableContinuous(attemptedRode, activeContinuous)).isEmpty()) { // has rideable continuous
            return rideables.stream().filter(e -> canPayRideableCost(rider, e.effect(), activeContinuous)).toList();
        }
        return List.of();
    }

    private boolean canPayRideableCost(CardInstance rider, ImmutableEffectStipulationObject rideableEffect, List<ContinuousInstance> activeContinuous) { // Resolves rideable cost based on strength.
        return isEffectLegal(rideableEffect, activeContinuous) && // If the rideable effect is legal
            !findChoicesFromList(CardInstance.class, List.of(rider), rideableEffect, activeContinuous).isEmpty(); // and the rider is a valid target for the continuous
    }

    /** Returns all rideable continuous effect on a card. Empty if none. */
    private List<ContinuousInstance> getRideableContinuous(CardInstance check, List<ContinuousInstance> activeContinuous) {
        return getContinuousThatApplyOnObject(check, activeContinuous,
            e -> e.effect().getType() == EffectChangeType.Rideable);
    }

    /** Returns every card with the same position as the given LivingObject, disregarding ride state. Includes the given object, if it is a card. */
    private List<CardInstance> getAllOnSamePos(LivingObject object) {
        if (object.getPosition().getBoardLocation() == BoardLocation.FIELD) {
            return state.field.getZoneAt(object.getPosition()).getCards();
        } else return state.getPile(object.getPosition().getBoardLocation()).contents.stream()
            .filter(c -> c.getPosition().getPilePos() == object.getPosition().getPilePos()).toList();
    }

    /** Returns if check is usable for placement, only based on activeContinuous. */
    private boolean isUsable(LivingObject check, List<ContinuousInstance> activeContinuous) {
        return isContinuousTypeOnObject(check, EffectChangeType.NotUsable, activeContinuous);
    }

    /** Returns whether the duration of a continuous should tick, based on the given List GameEvent.
     * @param filter defines the things that are looked at by checkCondition, which could be less than every living object (rarely). */
    private boolean isDurationTick(ContinuousInstance duration,
                                   List<GameEvent> lastSimultaneous,
                                   List<ContinuousInstance> activeContinuous,
                                   Predicate<LivingObject> filter) {
        // If the target is the gamestate, check tick condition without the target.
        if (duration.target() == null) {
            List<LivingObject> objects = state.getLivingObjects(filter);
            for (ConditionInstance condition : duration.effect().getDurationInstance().getTicksOnInstance()) {
                if (!checkCondition(condition,
                    condition.getCheck().getValue(state, sideIDtoController(duration.effect().getDurationInstance().getUser().getUser()), duration.effect().getDurationInstance(), activeContinuous),
                    objects, activeContinuous, 0)) {
                    return true; // The duration should tick if it's condition is false
                }
            }
        }
        else {
            List<LivingObject> targets = duration.target().stream().filter(filter).toList();
            for (ConditionInstance condition : duration.effect().getDurationInstance().getTicksOnInstance()) {
                if (!checkCondition(condition,
                    condition.getCheck().getValue(state, sideIDtoController(duration.effect().getDurationInstance().getUser().getUser()), duration.effect().getDurationInstance(), activeContinuous),
                    targets, List.of(lastSimultaneous), activeContinuous, 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True if the card can move at least 1 zone in any of the given Direction. */
    private boolean isAbleToMoveAnyDirection(LivingObject choice, Direction direction, List<ContinuousInstance> activeContinuous) {
        final Position movesFrom = choice.getPosition();
        if (movesFrom.getBoardLocation() != BoardLocation.FIELD) return false;
        Position movesTo = Position.resolveFieldPosition(Field.step(choice.getUser(), movesFrom, direction));
        return !state.field.getZoneAt(movesTo).isOccupied();
    }

    /** Abbreviation for isEffectLegal that sets usedByEffectID to 0. */
    private boolean isEffectLegal(EffectInstance effect, List<ContinuousInstance> activeContinuous) {
        return isEffectLegal(effect, activeContinuous, 0);
    }

    /**
     * Given an EffectInstance, returns true if it is legal to be used.
     * All conditions and EffectChangeInstance in the cost must be legal,
     * and one EffectChangeInstance in the effect must be legal.
     */
    private boolean isEffectLegal(EffectInstance effect, List<ContinuousInstance> activeContinuous, int usedByEffectID) {
        // Every condition on the effect must be met
        for (ConditionInstance condition : effect.getInstanceConditions()) {
            if (!isEffectLegal(condition, activeContinuous, usedByEffectID)) return false;
        }
        // Every EffectInstance in the cost must be met
        for (ConditionInstance condition : effect.getInstanceCost().stream().map(EffectChangeInstance::getInstanceConditions).flatMap(List::stream).toList()) {
            if (!isEffectLegal(condition, activeContinuous, usedByEffectID)) return false;
        }
        // Only one EffectInstance in the effect must be met
        for (int i = 0; i < effect.getInstanceEffectChanges().size(); i++) {
            if (effect.getInstanceEffectChanges().get(i).getInstanceConditions()
                .stream().allMatch(c -> isEffectLegal(c, activeContinuous, usedByEffectID))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given an EffectStipulation Object (ConditionInstance, EffectChangeInstance, EffectDurationInstance),
     * returns true if it can be used.
     */
    private boolean isEffectLegal(ImmutableEffectStipulationObject effect, List<ContinuousInstance> activeConstantContinuous) {
        return isEffectLegal(effect, activeConstantContinuous, 0);
    }

    /**
     * Given an effectStipulationObject (EffectChangeInstance or ConditionInstance),
     * returns whether the effect is legal by checking its stated conditions (and some preconditions).
     * This is the ONLY way that effects are checked for legality before resolution.
     */
    private boolean isEffectLegal(ImmutableEffectStipulationObject effect, List<ContinuousInstance> activeConstantContinuous, int usedByEffectID) {
        if (!state.gameGoing) return false;

        final EffectType type = effect.getEffectFrom().getType();
        final LivingObject user = effect.getUser();

        // Preconditions based on type
        if (state.isBetweenEffects && !effect.isUsableBetweenEffectsInstance()) return false;
        if (state.timingPoint == TimingPoint.U0 && (!effect.isUsableBetweenEffectsInstance() && type != EffectType.Unshroud)) return false;
        if (user.getUser() == SideID.NEUTRAL) return false; // NEUTRAL is only applicable to zones who don't have a controller, so those zones cannot use effects.
        if (user.isNegated() || effect.isNegated()) return false; // If negated, it isn't met.
        if (!user.isFaceUp() && !user.isShrouded()) return false; // If the user isn't faceUp, and it's NOT because its shrouded
        if (user.isShrouded() && !effect.getEffectFrom().isUsableWhileShroudedInstance()) return false; // If shrouded, the effect must be able to be used while shrouded

        if (type == EffectType.Placed && !justHappened(e -> e.getTarget() == user.getInstanceID()
            && e.getChange().getType().getSimultNum() == 9)) return false; // Must have just been placed

        if (type == EffectType.Activatable && (state.isInTriggerChain || state.opportunityHolder != user.getUser())) return false; // Must have the opportunity to play

        if (type == EffectType.Action && (state.isInTriggerChain // Actions cannot be used during a trigger chain
            || (justHappenedThisPhase(e -> e.getChange().getEffectID() == effect.getEffectID())))) return false; // Cannot have used an action this phase.

        if (type == EffectType.Unshroud && (!justHappened(e -> e.getTarget() == user.getInstanceID()
            && e.getChange().getType().getSimultNum() == 5))) return false; // Must have just unshrouded

        // Conditions based on continuous effects
        if (user.getCounters().contains(CounterType.Frozen)
            && type != EffectType.ConstantContinuous
            && type != EffectType.OptionalContinuous) return false;

        // Conditions based on the effect's actual written conditions
        final List<LivingObject> objects = state.getLivingObjects();
        for (ConditionInstance condition : effect.getInstanceConditions()) {
            int checkVal = condition.getCheck().getValue(state, sideIDtoController(user.getUser()), condition, user, activeConstantContinuous);
            if (!checkCondition(condition, checkVal, objects, activeConstantContinuous, usedByEffectID)) return false;
        }
        return true;
    }

    /** EffectChanges that are resolving are checked for legality here. */
    private boolean checkChangeConditions(GameChoiceChange gameChange, List<ContinuousInstance> activeContinuous, int usedByEffectID) {
        final EffectChangeType type = gameChange.getChange().getTypeInstance();
        final EffectChangeInstance change = gameChange.getChange();
        final List<? extends LivingObject> changeTargets = gameChange.getTarget();

        if (change.isNegated()) return false;
        // If an effect (that doesn't target the gamestate) has no target, then it can't be performed.
        if ((changeTargets == null || changeTargets.isEmpty()) &&
            Stream.of(EffectChangeType.beginBuildPhase, EffectChangeType.endBuildPhase,
                EffectChangeType.beginDrawPhase, EffectChangeType.endDrawPhase,
                EffectChangeType.beginCombatPhase, EffectChangeType.endCombatPhase,
                EffectChangeType.beginAftermathPhase, EffectChangeType.endAftermathPhase,
                EffectChangeType.AddDrawPhase, EffectChangeType.AddBuildPhase,
                EffectChangeType.AddCombatPhase, EffectChangeType.AddAftermathPhase,
                EffectChangeType.RemoveDrawPhase, EffectChangeType.RemoveBuildPhase,
                EffectChangeType.RemoveCombatPhase, EffectChangeType.RemoveAftermathPhase).noneMatch(c -> c == type)) return false;

        final LivingObject user = change.getUser();
        final ControllerInstance controller = sideIDtoController(user.getUser());
        final List<LivingObject> objects = state.getLivingObjects();
        // Conditions that are stated in the change itself are checked for here. Doesn't use the
        for (ConditionInstance condition : change.getInstanceConditions()) {
            if (changeTargets != null) {
                for (LivingObject target : changeTargets) {
                    int checkVal = condition.getCheck().getValue(state, controller, condition, target, activeContinuous);
                    if (!checkCondition(condition, checkVal, objects, activeContinuous, usedByEffectID)) return false;
                }
            }
            else {
                int checkVal = condition.getCheck().getValue(state, controller, condition, activeContinuous);
                if (!checkCondition(condition, checkVal, objects, activeContinuous, usedByEffectID)) return false;
            }
        }

        return true;
    }

    /** Returns true if the given condition is met with the given checkValue for the given targets. */
    private boolean checkCondition(ConditionInstance condition,
                                   int checkVal,
                                   List<LivingObject> available,
                                   List<ContinuousInstance> activeContinuous,
                                   int usedByEffectID) {
        return checkCondition(condition, checkVal, available, state.getJustHappened(), activeContinuous, usedByEffectID);
    }

    /** Returns true if the given condition is met with the given checkValue for the given targets for the given events that have just happened. */
    private boolean checkCondition(ConditionInstance condition,
                                   int checkVal,
                                   List<LivingObject> available,
                                   List<List<GameEvent>> just,
                                   List<ContinuousInstance> activeContinuous,
                                   int usedByEffectID) {
        final ConditionType currentType = condition.getType();
        final LivingObject user = condition.getUser();
        switch (condition.getTypeInstance()) {
            case TRUE -> {
                return true;
            }

            case FALSE -> {
                return false;
            }

            case Strategic -> {
                return true; // A strategic condition has conditions on its own effect.
            }

            case MinHealth -> {
                for (LivingObject check : getLivingChoices(condition, available, activeContinuous)) checkVal -= check.getHealthMax();
                return checkVal <= 0;
            }

            case MinMaterial -> {
                for(CommanderInstance check : getCommanderChoices(condition, available, activeContinuous)) checkVal -= check.getMaterial();
                return checkVal <= 0;
            }

            case MinPrecision -> {
                for (CommanderInstance check : getCommanderChoices(condition, available, activeContinuous)) {
                    if (check.getPrecision() < checkVal) return false;
                }
                return true;
            }

            case HigherThan0Strength -> {
                return checkVal > 0;
            }

            case AvailableAttackPlacementSpace -> {
                for (CommanderInstance check : getCommanderChoices(condition, available, activeContinuous)) {
                    for (ZoneInstance zoneInstance : state.field.playerAttackZones(check.getSide())) {
                        if (zoneInstance.isEmpty()) checkVal--;
                    }
                }
                return checkVal <= 0;
            }

            case AvailableProductionPlacementSpace -> {
                for (CommanderInstance check : getCommanderChoices(condition, available, activeContinuous)) {
                    for (ZoneInstance zoneInstance : state.field.playerProductionZones(check.getSide())) {
                        if (zoneInstance.isEmpty()) checkVal--;
                    }
                }
                return checkVal <= 0;
            }

            case MinThings -> {
                checkVal -= getLivingChoices(condition, available, activeContinuous).size();
                return checkVal <= 0;
            }

            case MaxThings -> {
                checkVal -= getLivingChoices(condition, available, activeContinuous).size();
                return checkVal >= 0;
            }

            case MaxEffectPerTurn -> {
                checkVal -= condition.getEffectFrom().getTimesUsedThisTurn();
                return checkVal > 0;
            }

            case Exclusive -> {
                for (LivingObject check : getLivingChoices(condition, available, activeContinuous)) {
                    final int originalID = check.getOriginalID();
                    final EffectInstance effectFrom = condition.getEffectFrom();
                    // For every living object whose OriginalID and user are the same as check
                    for (LivingObject everyInstance : state.getLivingObjects().stream().filter(l -> l.getOriginalID() == originalID && l.getUser() == user.getUser()).toList()) {
                        for (EffectInstance effect : everyInstance.getInstancedEffects()) {
                            if (effectFrom == effect.getEffectFrom()) checkVal -= effect.getTimesUsedThisTurn();
                        }
                    }
                }
                return checkVal > 0;
            }

            case IfLastChangeResolved -> {
                final EffectChangeInstance previous = getPreviousChange(condition);
                for (LivingObject check : getLivingChoices(condition, available, activeContinuous)) {
                    // The previous effect must be n the last simultaneous effect.
                    checkVal -= justHappenedLastSimultaneousNum(e -> {
                        assert previous != null; // "If Last Change Resolved" shouldn't be done on the first change.
                        return e.getChange().getEffectID() == previous.getEffectID() && e.getTarget() != -1;
                    }); // If the targetID is -1, then nothing happened.
                }
                return checkVal >= 0;
            }

            case MinCounters -> {
                for (LivingObject check : getLivingChoices(condition, available, activeContinuous))
                    checkVal -= check.getCounters().size();
                return checkVal <= 0;
            }

            case MinEchoCounters -> {
                for (LivingObject check : getLivingChoices(condition, available, activeContinuous))
                    checkVal -= check.getCounters().stream()
                    .filter(CounterType::isEchoCounter)
                    .toList().size();
                return checkVal <= 0;
            }

            case MinFortificationCounters, MinFrozenCounters, MinFlameCounters,
                 MinInfectionCounters, MinEtherealCounters, MinMomentumCounters,
                 MaxMomentumCounters, MinEnergyCounters, MinBountyCounters,
                 MinScrapCounters, MinTideCounters, MinRageCounters, MinSturdyCounters,
                 MinSiGNLCounters, MinTravelCounters, MinFervorCounters, MinStorageCounters,
                 MinUndeadCounters, MinVirtueCounters, MinSinCounters, MinDreamCounters,
                 MinInjectionCounters, MinVeilCounters, MinOverchargeCounters -> {
                final CounterType counter = CounterType.convertCounter(currentType);
                for (LivingObject check : getLivingChoices(condition, available, activeContinuous)) checkVal -= check.getCounters().stream()
                    .filter(c -> c == counter)
                    .toList().size();
                return checkVal <= 0;
            }

            // Below check "Just Happened"
            case CardDestroyed -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getTarget() == check.getInstanceID()
                        && e.getChange().getType().getSimultNum() == 24, just)) checkVal--; // Destroy
                    if (checkVal <= 0) return true;
                }
            }

            case CardDiscarded -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getTarget() == check.getInstanceID()
                        && e.getChange().getType().getSimultNum() == 7, just)) checkVal--; // Discard
                    if (checkVal <= 0) return true;
                }
            }

            case CardDisplaced -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getTarget() == check.getInstanceID()
                        && e.getChange().getType().getSimultNum() == 12, just)) checkVal--; // Displace
                    if (checkVal <= 0) return true;
                }
            }

            case CardDestroyedOrDiscarded -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getTarget() == check.getInstanceID()
                        && (e.getChange().getType().getSimultNum() == 24 // Destroy
                || e.getChange().getType().getSimultNum() == 7), just)) checkVal--; // Discard
                    if (checkVal <= 0) return true;
                }
            }

            case CardDestroyedExceptByAttack -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getTarget() == check.getInstanceID()
                        && e.getChange().getType().getSimultNum() == 24 // Destroy
                        && !isAttack(e.getChange().getEffectFrom()), just)) {
                        checkVal--;
                        if (checkVal <= 0) return true;
                    }
                }
            }

            case CardDestroyedExceptByAttackOrDiscarded -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getTarget() == check.getInstanceID()
                        && ((e.getChange().getType().getSimultNum() == 24 // Destroy
                        && !isAttack(e.getChange().getEffectFrom()))
                        || e.getChange().getType().getSimultNum() == 7), just)) { // Discard
                        checkVal--;
                        if (checkVal <= 0) return true;
                    }
                }
            }

            case CardPlaced -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getTarget() == check.getInstanceID()
                        && e.getChange().getType().getSimultNum() == 9, just)) { // Place
                        checkVal--;
                        if (checkVal <= 0) return true;
                    }
                }

            }

            case CardUnshrouded -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getTarget() == check.getInstanceID()
                        && e.getChange().getType().getSimultNum() == 5, just)) { // Unshroud
                        checkVal--;
                        if (checkVal <= 0) return true;
                    }
                }
            }

            case CardActivated -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getChange().getEffectFrom().getType() == EffectType.Activatable
                        && e.getChange().getUser() == check, just)) {
                        checkVal--;
                        if (checkVal <= 0) return true;
                    }
                }
            }

            case CardTriggered -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getChange().getEffectFrom().getType() == EffectType.Trigger
                        && e.getChange().getUser() == check, just)) {
                        checkVal--;
                        if (checkVal <= 0) return true;
                    }
                }
            }

            case CardActed -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getChange().getEffectFrom().getType() == EffectType.Action
                        && e.getChange().getUser() == check, just)) {
                        checkVal--;
                        if (checkVal <= 0) return true;
                    }
                }
            }

            case CardRestored -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getTarget() == check.getInstanceID()
                        && e.getChange().getType().getSimultNum() == 21, just)) { // Restore
                        checkVal--;
                        if (checkVal <= 0) return true;
                    }
                }
            }

            case CardDamaged -> {
                for (CardInstance check : getCardChoices(condition, available, activeContinuous)) {
                    if (occursInGameEvents(e -> e.getTarget() == check.getInstanceID()
                        && e.getChange().getType().getSimultNum() == 22, just)) { // Damage
                        checkVal--;
                        if (checkVal <= 0) return true;
                    }
                }
            }

            // GameState conditions
            case DrawPhaseBegins, DrawPhaseEnds,
                 BuildPhaseBegins, BuildPhaseEnds,
                 CombatPhaseBegins, CombatPhaseEnds,
                 AftermathPhaseBegins, AftermathPhaseEnds -> {
                return state.timingPoint == switch (condition.getTypeInstance()) {
                    case DrawPhaseBegins -> TimingPoint.D0;
                    case DrawPhaseEnds -> TimingPoint.D1;
                    case BuildPhaseBegins -> TimingPoint.B0;
                    case BuildPhaseEnds -> TimingPoint.B3;
                    case CombatPhaseBegins -> TimingPoint.C0;
                    case CombatPhaseEnds -> TimingPoint.C4;
                    case AftermathPhaseBegins -> TimingPoint.A0;
                    case AftermathPhaseEnds -> TimingPoint.A2;
                    default -> throw new IllegalArgumentException("How did we get here?");
                };
            }

            case PhaseBegins -> {
                return switch(state.timingPoint) {
                    case D0, B0, C0, A0 -> true;
                    default -> false;
                };
            }

            case PhaseEnds -> {
                return switch(state.timingPoint) {
                    case D1, B3, C4, A2 -> true;
                    default -> false;
                };
            }

            // Use Optional only for effectChange conditions
            case Optional -> {
                return getPlayerOption(sideIDtoController(user.getUser()), GameChoiceChange.OPTIONAL(condition.getEffectChangeFrom()));
            }

            case Choice -> {
                return condition.getUser().getDeclaredChoices().contains(checkVal);
            }

            // Cards that Use cards conditions
            case MaterialAvailable -> {
                return canMakeExactSum(
                    getLivingChoices(condition, available, activeContinuous).stream() // Used object must be used by the checked usedByEffectID AND it cannot be the card that is being placed
                        .filter(o ->  o.getUsedByEffectID().contains(usedByEffectID) && o.getInstanceID() != condition.getUser().getInstanceID())
                        .map(c -> getMaterialPlaceCost(c, activeContinuous).getValue(state, sideIDtoController(condition.getUser().getUser()), condition, condition.getUser(), activeContinuous))
                        .toList(),
                    checkVal);
            }

            case CountersAvailable -> {
                return getLivingChoices(condition, available, activeContinuous).stream()
                    .filter(o -> o.getUsedByEffectID().contains(usedByEffectID) && o.getInstanceID() != condition.getUser().getInstanceID())
                    .map(LivingObject::getCounters)
                    .flatMap(List::stream)
                    .toList()
                    .size() >= checkVal;
            }

            case HealthAvailable -> {
                return getLivingChoices(condition, available, activeContinuous).stream()
                    .filter(o -> o.getUsedByEffectID().contains(usedByEffectID) && o.getInstanceID() != condition.getUser().getInstanceID() && o.getPosition().getBoardLocation() == BoardLocation.FIELD)
                    .mapToInt(LivingObject::getHealthMax)
                    .sum() >= checkVal;
            }

            default -> throw new IllegalArgumentException("Condition type (" + condition.getType() + ") is not implemented.");
        }
        return false;
    }

    /** Returns if a commander can pay the given material, which is being asked for by EffectObject check. */
    private boolean isMaterialPayable(int materialToPay, CommanderInstance commander, EffectObject check, List<ContinuousInstance> activeContinuous) {
        // Effect and activeContinuous checks to be added when continuous may affect this.
        return (commander.getMaterial() >= materialToPay);
    }

    /* Player Input Functions */
    /** Returns the number a commander chooses from a list of Integer. */
    private int getPlayerNumber(ControllerInstance controller, List<Integer> choices) {
        return controller.addNumChoice(choices);
    }

    /** Returns the choice a commander chooses from a list of GameChoice. */
    private GameChoice getPlayerChoice(ControllerInstance controller, List<GameChoice> legalChoices) {
        return controller.addChoice(Stream.of(List.of(GameChoice.NOT_USED()), legalChoices).flatMap(List::stream).toList());
    }

    /**
     * Returns the choice a commander chooses from a List List gameChoiceChanges,
     * where the first list contains each choice, and the second list contains each
     * possible GameChoiceChange involved in that choice.
     */
    private List<GameChoiceChange> getPlayerChangeChoice(ControllerInstance controller, List<List<GameChoiceChange>> legalChoices) {
        if (legalChoices.isEmpty()) { return List.of(); }
        if (legalChoices.size() == 1 && legalChoices.get(0).size() == 1) { return List.of(legalChoices.get(0).get(0)); }
        return controller.addChangeChoice(Stream.of(List.of(List.of(GameChoiceChange.FAILED_USE())), legalChoices).flatMap(List::stream).toList());
    }

    /** Returns whether the player chose to use the choice. */
    private boolean getPlayerOption(ControllerInstance controller, GameChoice yesChoice) {
        return !Objects.equals(
            controller.addChoice(
                List.of(
                    yesChoice,
                    GameChoice.NOT_USED()
                )
            ),
            GameChoice.NOT_USED()
        );
    }

    /** Returns whether the player chose to use the choiceChange. */
    private boolean getPlayerOption(ControllerInstance controller, GameChoiceChange yesChoice) {
        return !Objects.equals(
            controller.addChangeChoice(
                List.of(List.of(
                    yesChoice,
                    GameChoiceChange.NOT_USED()
                ))
            ).get(0),
            GameChoiceChange.NOT_USED()
        );
    }

    /** Given a controller, makes a RandomControllerInstance with the same Commander as the owner. Use to make random choices. */
    private RandomControllerInstance getRandom(ControllerInstance controller) {
        return new RandomControllerInstance(room, controller.getCommander(), random);
    }

    /* --- Choice Resolution Functions (Makes all player choices) --- */
    /** Given a list of EffectInstance and active continuous, creates GameChoice with a preview of options for that choice. */
    private List<GameChoice> getGameChoices(List<EffectInstance> legalEffects, List<ContinuousInstance> activeConstantContinuous) {
        final List<GameChoice> options = new ArrayList<>();
        for (EffectInstance checkEffect : legalEffects) {
            options.add(getGameChoice(checkEffect, activeConstantContinuous));
        }
        return options;
    }

    /** Given an EffectInstance and activeConstantContinuous, creates a GameChoice with a preview of options for that choice. */
    private GameChoice getGameChoice(EffectInstance checkEffect, List<ContinuousInstance> activeConstantContinuous) {
        List<LivingObject> previewOptions = new ArrayList<LivingObject>();
        for (EffectChangeInstance effectChange : checkEffect.getInstanceCost()) {
            previewOptions.addAll(getLivingChoices(effectChange, activeConstantContinuous));
        }
        return new GameChoice(checkEffect, previewOptions);
    }

    /** Returns the effectChange associated with the given EffectStipulationObject, returns null if it cannot find it. */
    private EffectChangeInstance getAssociatedChange(EffectStipulationObject currentChange) {
        final List<EffectChangeInstance> changes = state.getEffectChanges();
        final int checkID = currentChange.getEffectID();
        int i = changes.size() - 1;
        while (i >= 0) {
            if (changes.get(i).getEffectID() <= checkID) return changes.get(i);
            i--;
        }
        return null;
    }

    /** Returns the effectChange before the given change, assuming that the user has their previous change.
     * Returns null if the current change is the first change. */
    private EffectChangeInstance getPreviousChange(EffectStipulationObject currentChange) {
        final LivingObject user = currentChange.getUser();
        final int checkID = currentChange.getEffectID();
        // Checking the user's instancedPlaceCost
        for (int i = 1; i < user.getInstancedPlaceCost().getInstanceCost().size(); i++) {
            if (checkID == user.getInstancedPlaceCost().getInstanceCost().get(i).getEffectID()) {
                return user.getInstancedPlaceCost().getInstanceCost().get(i - 1);
            }
        }
        for (int i = 1; i < user.getInstancedPlaceCost().getInstanceEffectChanges().size(); i++) {
            if (checkID == user.getInstancedPlaceCost().getInstanceEffectChanges().get(i).getEffectID()) {
                return user.getInstancedPlaceCost().getInstanceEffectChanges().get(i - 1);
            }
        }
        // Checking the user's EffectInstance
        for (EffectInstance effect : user.getInstancedEffects()) {
            for (int i = 1; i < effect.getInstanceCost().size(); i++) {
                if (checkID == effect.getInstanceCost().get(i).getEffectID()) {
                    return effect.getInstanceCost().get(i - 1);
                }
            }
            for (int i = 1; i < effect.getInstanceEffectChanges().size(); i++) {
                if (checkID == effect.getInstanceEffectChanges().get(i).getEffectID()) {
                    return effect.getInstanceEffectChanges().get(i - 1);
                }
            }
        }
        return null;
    }

    /**
     * Abbreviation for getChoices that targets all the change's choices and automatically generates information.
     * This should be the base one used.
     */
    private List<List<GameChoiceChange>> getChoiceChanges(EffectChangeInstance change, List<ContinuousInstance> activeContinuous) {
        return getChoiceChanges(change, state.getLivingObjects(), List.of(), activeContinuous);
    }

    /**
     * Abbreviation for getChoices that only generates things which target the given target, and automatically generates information.
     */
    private List<List<GameChoiceChange>> getChoiceChangesOfTarget(EffectChangeInstance change, LivingObject target, List<ContinuousInstance> activeContinuous) {
        return getChoiceChanges(change, List.of(target), List.of(), activeContinuous);
    }

    /**
     * Given an EffectChangeInstance and information, converts that change into a List List GameChoiceChange
     * that represents every possible change for that effect in the order that they're done, with the given information.
     * Does not convert any effectChangeType that is used by the system.
     * @param setInformation adds the given information to the List Integer choiceInfo in each gameChoiceChange.
     */
    private List<List<GameChoiceChange>> getChoiceChanges(EffectChangeInstance change,
                                                          List<LivingObject> wantedTargets,
                                                          List<Integer> setInformation,
                                                          List<ContinuousInstance> activeContinuous) {
        final List<List<GameChoiceChange>> choiceList = new ArrayList<>();

        final ControllerInstance controller = sideIDtoController(change.getUser().getUser());
        final LivingObject user = change.getUser();
        final List<LivingObject> availableTargets = findChoicesFromList(LivingObject.class, wantedTargets, change, activeContinuous);
        final int strength = change.getStrengthInstance().getValue(state, sideIDtoController(user.getUser()), change, user, activeContinuous);

        setChoices(choiceList, change, controller, user, availableTargets, strength, setInformation, activeContinuous);

        return choiceList;
    }

    /// Helper function for getChoices. Adds List GameChoiceChange to choiceList based on instance type of the given change.
    /// Also loops through choiceList to add additional setInformation, so should never add to the same list twice.
    private void setChoices(List<List<GameChoiceChange>> choiceList,
                            EffectChangeInstance change,
                            ControllerInstance controller,
                            LivingObject user,
                            List<LivingObject> availableTargets,
                            int strength,
                            List<Integer> setInformation,
                            List<ContinuousInstance> activeContinuous) {
        final EffectChangeType currentType = change.getType();

        switch (currentType) {
            // Effects which can only be done on one of the available targets, and don't need additional information other than their strength.
            case Damage, Restore -> {
                for (LivingObject l : availableTargets) {
                    choiceList.add(List.of(
                        new GameChoiceChange(
                            change,
                            l,
                            List.of(
                                strength
                            )
                        )
                    ));
                }
            }

            case MoveForwards -> choiceList.add(getMovementChoice(availableTargets, change, activeContinuous, strength,
                List.of(Direction.FORWARD)));

            case MoveBackwards -> choiceList.add(getMovementChoice(availableTargets, change, activeContinuous, strength,
                List.of(Direction.BACKWARD)));

            case MoveLeft -> choiceList.add(getMovementChoice(availableTargets, change, activeContinuous, strength,
                List.of(Direction.LEFT)));

            case MoveRight -> choiceList.add(getMovementChoice(availableTargets, change, activeContinuous, strength,
                List.of(Direction.RIGHT)));

            case MoveLeftOrRight -> choiceList.add(getMovementChoice(availableTargets, change, activeContinuous, strength,
                List.of(Direction.LEFT, Direction.RIGHT)));

            case MoveForwardsOrBackwards -> choiceList.add(getMovementChoice(availableTargets, change, activeContinuous, strength,
                List.of(Direction.FORWARD, Direction.BACKWARD)));

            case MoveAnyDirection -> choiceList.add(getMovementChoice(availableTargets, change, activeContinuous, strength,
                List.of(Direction.LEFT, Direction.RIGHT, Direction.FORWARD, Direction.BACKWARD)));

            case MoveDeclaredDirection -> choiceList.add(getMovementChoice(availableTargets, change, activeContinuous, strength,
                List.of(user.getDeclaredDirection())));

            // Effects which are done on every available target. In resolution, these just call their associated change on every target.
            case DamageAll -> {
                choiceList.add(List.of(
                    new GameChoiceChange(
                        change,
                        availableTargets.stream().filter(c -> isDamageable(c, activeContinuous)).toList(),
                        List.of(
                            strength
                        )
                    )
                ));
            }

            case RestoreAll -> {
                choiceList.add(List.of(
                    new GameChoiceChange(
                        change,
                        availableTargets,
                        List.of(
                            strength
                        )
                    )
                ));
            }

            case TargetAll -> {
                choiceList.add(List.of(
                    new GameChoiceChange(
                        change,
                        availableTargets.stream().filter(c -> isTargetable(c, activeContinuous)).toList(),
                        List.of(
                            strength
                        )
                    )
                ));
            }

            case MoveAllForwards -> {
                choiceList.add(List.of(
                    new GameChoiceChange(
                        change,
                        availableTargets.stream().filter(c -> isAbleToMoveAnyDirection(c, Direction.FORWARD, activeContinuous)).toList(),
                        List.of(
                            strength
                        )
                    )
                ));
            }

            case MoveAllDeclared -> {
                choiceList.add(List.of(
                    new GameChoiceChange(
                        change,
                        availableTargets.stream().filter(c -> isAbleToMoveAnyDirection(c, user.getDeclaredDirection(), activeContinuous)).toList(),
                        List.of(
                            strength
                        )
                    )
                ));
            }

            case ShroudAll -> {
                choiceList.add(List.of(
                    new GameChoiceChange(
                        change,
                        availableTargets.stream().filter(l -> !l.isShrouded()).toList(), // Cannot shroud unshrouded targets
                        List.of(
                            strength
                        )
                    )
                ));
            }

            // Effects which are done on a number of available targets, as determined by strength.

            // Multiple things in one choice, but not on the same target.
            // Strength determines how long the single added list is - information for these choices is an empty list.
            case UniqueTarget, Reveal, Shuffle, SendToDiscard, SendToDestroyed, Destroy, SendToDecisiveDeck, Displace -> {
                choiceList.add(getPlayerChoiceOfUniqueChoices(controller, change, strength, availableTargets, activeContinuous));
            }

            case HalveHealthRoundedUp, HalveHealthRoundedDown -> {
                choiceList.add(getPlayerChoiceOfUniqueChoices(controller, change, strength, availableTargets, activeContinuous,
                    l -> l.getPosition().getBoardLocation() == BoardLocation.FIELD));
            }

            case Search -> {
                choiceList.add(getPlayerChoiceOfUniqueChoices(controller, change, strength, availableTargets, activeContinuous,
                    l -> l.getPosition().getBoardLocation() == BoardLocation.DECK_ONE || l.getPosition().getBoardLocation() == BoardLocation.DECK_TWO));
            }

            case Unshroud -> {
                choiceList.add(getPlayerChoiceOfUniqueChoices(controller, change, strength, availableTargets, activeContinuous,
                    l -> l.isShrouded() && l.getPosition().getBoardLocation() == BoardLocation.FIELD));
            }

            case RandomUniqueTarget -> {
                choiceList.add(getPlayerChoiceOfUniqueChoices(getRandom(controller), change, strength, availableTargets, activeContinuous));
            }

            case AdjacentTarget -> {
                List<GameChoiceChange> choices = new ArrayList<>();
                setSingleTargetChoice(controller, choices, strength, change, availableTargets, activeContinuous,
                    l -> choices.stream().anyMatch(t -> t.getTarget().get(0).getPosition().isAdjacent(l.getPosition())));
                choiceList.add(choices);
            }

            case NonAdjacentTarget -> {
                List<GameChoiceChange> choices = new ArrayList<>();
                setSingleTargetChoice(controller, choices, strength, change, availableTargets, activeContinuous,
                    l -> choices.stream().noneMatch(t -> t.getTarget().get(0).getPosition().isAdjacent(l.getPosition())));
                choiceList.add(choices);
            }

            case AdjacentToOriginalTarget -> {
                List<GameChoiceChange> choices = new ArrayList<>();
                setSingleTargetChoice(controller, choices, strength, change, availableTargets, activeContinuous,
                    l -> choices.get(0).getTarget().get(0).getPosition().isAdjacent(l.getPosition()));
                choiceList.add(choices);
            }

            case SameLaneTarget -> {
                List<GameChoiceChange> choices = new ArrayList<>();
                setSingleTargetChoice(controller, choices, strength, change, availableTargets, activeContinuous,
                    l -> choices.get(0).getTarget().get(0).getPosition().getLane() == l.getPosition().getLane());
                choiceList.add(choices);
            }

            case SameFileTarget -> {
                List<GameChoiceChange> choices = new ArrayList<>();
                setSingleTargetChoice(controller, choices, strength, change, availableTargets, activeContinuous,
                    l -> choices.get(0).getTarget().get(0).getPosition().getFile() == l.getPosition().getFile());
                choiceList.add(choices);
            }

            // Multiple things in one choice, including multiple times on the same target.
            case MultiTarget -> {
                choiceList.add(getPlayerChoiceOfChoices(controller, change, strength, activeContinuous, availableTargets));
            }

            case RandomMultiTarget -> {
                choiceList.add(
                    getPlayerChoiceOfChoices(getRandom(controller), change, strength, activeContinuous, availableTargets)
                );
            }

            // May only discard cards that are in hand.
            case Discard, aftermathDiscard -> {
                choiceList.add(getPlayerChoiceOfUniqueChoices(controller, change, strength, availableTargets, activeContinuous,
                    l -> BoardLocation.inHand(l.getPosition().getBoardLocation())));
            }

            case Place, PlaceShrouded -> {
                choiceList.add(getPlacementChoice(controller, availableTargets, change, strength, true, activeContinuous,
                    z -> true,
                    c -> true)
                );
            }

            case PlaceAdjacentToThisCard -> {
                choiceList.add(getPlacementChoice(controller, availableTargets, change, strength, true, activeContinuous,
                    z -> z.getPosition().isAdjacent(user.getPosition()), // Filters for adjacent zones
                    c -> true)
                );
            }

            case PlaceOnTarget -> {
                choiceList.add(getPlacementChoice(controller, availableTargets, change, strength, true, activeContinuous,
                    z -> user.getTarget().stream().anyMatch(t -> t.isOnFieldTarget(z)), // Filters for targeted zones
                    c -> true)
                );
            }

            case PlaceWithNoCost -> {
                choiceList.add(getPlacementChoice(controller, availableTargets, change, strength, false, activeContinuous,
                    z -> true,
                    c -> true)
                );
            }

            case PlaceWithNoCostOnTarget -> {
                choiceList.add(getPlacementChoice(controller, availableTargets, change, strength, false, activeContinuous,
                    z -> user.getTarget().stream().anyMatch(t -> t.isOnFieldTarget(z)),  // Filters for targeted zones
                    c -> true)
                );
            }

            case PlaceUsingYourCardsOnField, PlaceUsingHand, PlaceUsingDestroyed, PlaceUsingDiscard, PlaceUsingDisplaced,
                 PlaceUsingDeck, PlaceUsingDecisivePile, PlaceUsingTarget, PlaceUsingCardsOnField, PlaceUsingOpponentsCardsOnField,
                 PlaceUsingZonesYouControl -> {
                choiceList.add(getUsedPlacementChoice(controller, availableTargets, change, strength, activeContinuous,
                    z -> true,
                    c -> true,
                    u -> true));
            }

            case PlaceUsingHandOrWithEffect -> {
                choiceList.add(getUsedOrWithMaterialPlacementChoice(controller, availableTargets, change, strength, activeContinuous,
                    z -> true,
                    c -> true,
                    u -> true));
            }

            case PlaceMinusStrengthMaterialMin0 -> {
                choiceList.add(getPlacementChoice(controller, availableTargets, change, 1, true, activeContinuous,
                    z -> true,
                    c -> true,
                    m -> -strength,
                    l -> l < 0 ? 0 : l)); // Min 0
            }

            case PlaceMinusStrengthMaterialNoMin -> {
                choiceList.add(getPlacementChoice(controller, availableTargets, change, 1, true, activeContinuous,
                    z -> true,
                    c -> true,
                    m -> -strength,
                    l -> l)); // No min
            }

            case PlaceConspiracyToken, PlaceTreeToken, PlaceAirToken, PlaceScrapToken, PlaceSporeToken, PlaceShrubToken, PlaceSpiritToken,
                 PlacePilotFishToken, PlaceHillToken, PlaceEchoToken, PlaceRatToken, PlaceDemonToken, PlaceImitationToken,
                 PlaceLabSubject00Token, PlaceSoulToken, PlaceOpenMindToken, PlacePureEnergyToken, PlaceTravestyToken,
                 PlaceTargetNameToken -> {
                final List<ZoneInstance> zoneTargets = ensureMatches(ZoneInstance.class, availableTargets);
                choiceList.add(getTokenPlacementChoice(controller, change, strength, activeContinuous,
                    zoneTargets::contains, // The zones to place on must be in the target of the placement effect.
                    c -> true,
                    u -> true));
            }

            case PlaceAnyAmount -> {
                final List<ZoneInstance> zonesToPlace = getZoneChoices(change, activeContinuous); // Ensures that zones are being targeted
                final CommanderInstance placingCommander = state.getCommander(change.getUser().getUser());
                List<GameChoiceChange> placements = new ArrayList<>();
                int totalMaterialPaid = 0;

                // While things can still be placed
                while (isPlaceableCardInList(ensureMatches(CardInstance.class, availableTargets.stream()
                       // Filters for cards not already placed
                    .filter(l -> placements.stream().noneMatch(g -> g.getTarget().contains(l))).toList()), activeContinuous)
                    // And the player wants to place
                    && getPlayerOption(sideIDtoController(change.getUser().getUser()), new GameChoiceChange(
                    change,
                    placements.stream().map(GameChoiceChange::getTarget).flatMap(List::stream).toList(),
                    List.of()))) { // Option targets previous cards placed

                    // Make a choice for where the player wants to place
                    GameChoiceChange choice = getPlayerChangeChoice(controller, List.of(addSinglePlacement(placements, controller,
                        ensureMatches(CardInstance.class, availableTargets), List.of(), change, true, false,
                        totalMaterialPaid, activeContinuous, zonesToPlace, placingCommander,
                        c -> true,
                        u -> true,
                        m -> -1 * strength, // Subtracts strength from material cost of each card
                        l -> l < 0 ? 0 : l)) // Always min 0
                    ).get(0);
                    placements.add(choice);
                    totalMaterialPaid += choice.getChoiceInfo().get(1) // Base material cost
                        + choice.getChoiceInfo().get(3); // Material cost increase based on cards used
                }
                choiceList.add(placements);
            }

            case RemoveEnergyCounters, RemoveBountyCounters, RemoveScrapCounters, RemoveTideCounters,
                 RemoveRageCounters, RemoveSturdyCounters, RemoveSiGNLCounters, RemoveTravelCounters, RemoveFervorCounters,
                 RemoveStorageCounters, RemoveUndeadCounters, RemoveVirtueCounters, RemoveSinCounters, RemoveDreamCounters,
                 RemoveInjectionCounters, RemoveVeilCounters, RemoveOverchargeCounters, RemoveFortificationCounters,
                 RemoveFrozenCounters, RemoveFlameCounters, RemoveInfectionCounters, RemoveEtherealCounters,
                 RemoveMomentumCounters, RemoveRestoreEcho, RemoveDamageEcho -> {
                final CounterType counterToRemove = CounterType.convertCounter(currentType);
                List<GameChoiceChange> choices = new ArrayList<>();
                setSinglePlayerChoice(controller, choices, strength, change, availableTargets, activeContinuous,
                    o -> o.hasCounter(counterToRemove)
                        && o.getCounters().stream().filter(type -> type == counterToRemove).count() // o's counters
                        - choices.stream().filter(choice -> choice.getTarget().get(0) == o).count() // minus the number of times o was chosen to be removed from
                        > 0); // Cannot go below 0
                choiceList.add(choices);
            }

            case AddEnergyCounters, AddBountyCounters, AddScrapCounters, AddTideCounters, AddRageCounters, AddSturdyCounters,
                 AddSiGNLCounters, AddTravelCounters, AddFervorCounters, AddStorageCounters, AddUndeadCounters,
                 AddVirtueCounters, AddSinCounters, AddDreamCounters, AddInjectionCounters, AddVeilCounters,
                 AddOverchargeCounters, AddFortificationCounters, AddFrozenCounters, AddFlameCounters, AddInfectionCounters,
                 AddEtherealCounters, AddMomentumCounters, AddDamageEcho, AddRestoreEcho -> {
                List<GameChoiceChange> choices = new ArrayList<>();
                for (LivingObject choice : getLivingChoices(change, activeContinuous)) {
                    choices.add(new GameChoiceChange(
                        change,
                        choice,
                        List.of(
                            strength
                        )
                    ));
                }
                choiceList.add(choices);
            }

            case Choose1, DeclareNum, DeclareName, DeclareAttribute, DeclarePile, DeclareDirection -> {
                choiceList.add(List.of(
                    new GameChoiceChange(
                        change,
                        availableTargets, // Should only target the object that is choosing
                        List.of(
                            change.getStrength() // Selected Choice
                                .getValue(state, controller, change, activeContinuous)
                        )
                    )
                ));
            }

            case RandomChoose1, DeclareRandomDirection -> {
                choiceList.add(List.of(
                    new GameChoiceChange(
                        change,
                        availableTargets, // Should only target the object that is choosing
                        List.of(
                            change.getStrength() // Selected random choice
                                .getValue(state, getRandom(controller), change, activeContinuous)
                        )
                    ))
                );
            }

            // Effects which have no option for user input once used (only targets commander).
            case Excavate, Draw, GainMaterial, LoseMaterial, PayMaterial -> {
                for (CommanderInstance commander : ensureMatches(CommanderInstance.class, availableTargets)) // Done on every commander target.
                    choiceList.add(
                        List.of(new GameChoiceChange(
                            change,
                            commander, // Targets the using commander
                            List.of(
                                strength
                            )
                        ))
                    );
            }

            // Effects which have no option for user input once used and that don't target anything (target the state).
            case AddDrawPhase, AddBuildPhase, AddCombatPhase, AddAftermathPhase, SkipCurrentPhase,
                 RemoveDrawPhase, RemoveBuildPhase, RemoveCombatPhase, RemoveAftermathPhase -> {
                choiceList.add(List.of(new GameChoiceChange(
                    change,
                    (List<LivingObject>) null,
                    List.of(
                        strength
                    )
                )));
            }

            case selfPlacementEffect -> {
                choiceList.add(List.of(new GameChoiceChange(change, user, List.of(
                    setInformation.get(0) // instanceID of zone to place on
                ))));
            }

            default -> throw new IllegalArgumentException("Choice resolution for " + currentType + " doesn't exist. " +
                "It is either only used by other effects during resolution, or can only be called by the System.");
        }
        if (setInformation != null && !setInformation.isEmpty())
            for (List<GameChoiceChange> choices : choiceList)
                for (GameChoiceChange choice : choices)
                    choice.addChoiceInfo(setInformation);
    }

    /** Given a list of active ConstantContinuous, returns a list of active EffectInstance. */
    private List<EffectInstance> getAvailableEffects(List<ContinuousInstance> activeConstantContinuous) {
        final List<EffectInstance> usableEffects = new ArrayList<EffectInstance>();
        for (LivingObject user : state.getLivingObjects()) {
            for (EffectInstance effect : user.getInstancedEffects()) {
                if (effect.getType() != EffectType.ConstantContinuous) {
                    if (isEffectLegal(effect, activeConstantContinuous)) {
                        usableEffects.add(effect);
                    }
                }
            }
        }
        return usableEffects;
    }

    /* --- Choice Resolution Function Helpers --- */
    private boolean isTargetable(LivingObject check, List<ContinuousInstance> activeContinuous) {
        return isContinuousTypeOnObject(check, EffectChangeType.NotTargetable, activeContinuous);
    }

    private boolean isDamageable(LivingObject check, List<ContinuousInstance> activeContinuous) {
        return isContinuousTypeOnObject(check, EffectChangeType.NotDamageable, activeContinuous);
    }

    /** Returns each movement choice in 1 direction from the given direction that are available. */
    private List<GameChoiceChange> getMovementChoice(List<LivingObject> availableTargets,
                                                     EffectChangeInstance change,
                                                     List<ContinuousInstance> activeContinuous,
                                                     int strength,
                                                     List<Direction> directions) {
        List<GameChoiceChange> choices = new ArrayList<>();
        for (LivingObject choice : availableTargets) {
            for (Direction direction : directions) {
                if (isAbleToMoveAnyDirection(choice, direction, activeContinuous)) {
                    choices.add(new GameChoiceChange(
                        change,
                        choice,
                        List.of(
                            strength,
                            direction.toDeclared()
                        )
                    ));
                }
            }
        }
        return choices;
    }

    /** Returns the placement choice(s) a commander chooses that places Tokens, following other params. */
    private List<GameChoiceChange> getTokenPlacementChoice(ControllerInstance controller,
                                                           EffectChangeInstance change,
                                                           int strength,
                                                           List<ContinuousInstance> activeContinuous,
                                                           Predicate<ZoneInstance> zoneFilter,
                                                           Predicate<CardInstance> cardFilter,
                                                           Predicate<LivingObject> usedFilter) {
        final List<CardInstance> tokens = new ArrayList<>();
        for (int i = 0; i < strength; i++) {
            tokens.add(CardInstance.TOKEN(state.getCommander(change.getUser().getUser()), CardDefinition.getTokenID(change.getType()), state.getNextLivingID(), state.getNextEffectID()));
        }

        final List<LivingObject> usedThings = getUsableList(change.getType(), change.getUser(), activeContinuous);

        // Sets "Being Used By" for each usedCard first, as I plan to have tokens that are placed using cards.
        // This is unset by resolveAfterEffect.
        for (LivingObject beingUsed : usedThings) {
            final List<Integer> alreadyBeingUsedBy = new ArrayList<>(beingUsed.getUsedByEffectID()); // List of card's current "Used By"
            alreadyBeingUsedBy.add(change.getEffectID()); // Adds this effect's "Used By"
            beingUsed.setUsedByEffectID(alreadyBeingUsedBy);
        }

        final List<GameChoiceChange> choices = new ArrayList<>();
        setSinglePlacementChoice(choices, controller, tokens, List.of(), change, strength, true, null,
            activeContinuous, zoneFilter, cardFilter, usedFilter, m -> 0, l -> l);
        return choices;
    }

    /** Returns the placement choice(s) a commander chooses that can place cards without using OR uses cards from the given availableTargets, following other params. */
    private List<GameChoiceChange> getUsedOrWithMaterialPlacementChoice(ControllerInstance controller,
                                                                        List<LivingObject> availableTargets,
                                                                        EffectChangeInstance change,
                                                                        int strength,
                                                                        List<ContinuousInstance> activeContinuous,
                                                                        Predicate<ZoneInstance> zoneFilter,
                                                                        Predicate<CardInstance> cardFilter,
                                                                        Predicate<LivingObject> usedFilter) {
        return getUsedOrWithMaterialPlacementChoice(controller, availableTargets, change, strength, activeContinuous,
            zoneFilter, cardFilter, usedFilter, m -> 0, l -> l);
    }

    /** Returns the placement choice(s) a commander chooses that can place cards without using OR uses cards from the given availableTargets, following other params. */
    private List<GameChoiceChange> getUsedOrWithMaterialPlacementChoice(ControllerInstance controller,
                                                                        List<LivingObject> availableTargets,
                                                                        EffectChangeInstance change,
                                                                        int strength,
                                                                        List<ContinuousInstance> activeContinuous,
                                                                        Predicate<ZoneInstance> zoneFilter,
                                                                        Predicate<CardInstance> cardFilter,
                                                                        Predicate<LivingObject> usedFilter,
                                                                        Function<LivingObject, Integer> materialReduction,
                                                                        Function<Integer, Integer> materialLimit) {
        final List<LivingObject> usedThings = getUsableList(change.getType(), change.getUser(), activeContinuous);

        // Sets "Being Used By" for each usedCard first
        // This is unset by resolveAfterEffect.
        for (LivingObject beingUsed : usedThings) {
            final List<Integer> alreadyBeingUsedBy = new ArrayList<>(beingUsed.getUsedByEffectID()); // List of card's current "Used By"
            alreadyBeingUsedBy.add(change.getEffectID()); // Adds this effect's "Used By"
            beingUsed.setUsedByEffectID(alreadyBeingUsedBy);
        }

        final List<GameChoiceChange> choices = new ArrayList<>();
        setSinglePlacementChoice(choices, controller, ensureMatches(CardInstance.class, availableTargets), usedThings, change, strength,
            true, null, activeContinuous, zoneFilter, cardFilter, usedFilter, materialReduction, materialLimit);
        return choices;
    }

    /** Returns the placement choice(s) a commander chooses that uses cards from the given availableTargets, following other params. */
    private List<GameChoiceChange> getUsedPlacementChoice(ControllerInstance controller,
                                                          List<LivingObject> availableTargets,
                                                          EffectChangeInstance change,
                                                          int strength,
                                                          List<ContinuousInstance> activeContinuous,
                                                          Predicate<ZoneInstance> zoneFilter,
                                                          Predicate<CardInstance> cardFilter,
                                                          Predicate<LivingObject> usedFilter) {
        return getUsedPlacementChoice(controller, availableTargets, change, strength, activeContinuous,
            zoneFilter, cardFilter, usedFilter, m -> 0, l -> l);
    }

    /** Returns the placement choice(s) a commander chooses that uses cards from the given availableTargets, following other params. */
    private List<GameChoiceChange> getUsedPlacementChoice(ControllerInstance controller,
                                                          List<LivingObject> availableTargets,
                                                          EffectChangeInstance change,
                                                          int strength,
                                                          List<ContinuousInstance> activeContinuous,
                                                          Predicate<ZoneInstance> zoneFilter,
                                                          Predicate<CardInstance> cardFilter,
                                                          Predicate<LivingObject> usedFilter,
                                                          Function<LivingObject, Integer> materialReduction,
                                                          Function<Integer, Integer> materialLimit) {
        final List<LivingObject> usedThings = getUsableList(change.getType(), change.getUser(), activeContinuous);

        // Sets "Being Used By" for each usedCard first
        // This is unset by resolveAfterEffect.
        for (LivingObject beingUsed : usedThings) {
            final List<Integer> alreadyBeingUsedBy = new ArrayList<>(beingUsed.getUsedByEffectID()); // List of card's current "Used By"
            alreadyBeingUsedBy.add(change.getEffectID()); // Adds this effect's "Used By"
            beingUsed.setUsedByEffectID(alreadyBeingUsedBy);
        }

        final List<GameChoiceChange> choices = new ArrayList<>();
        setSinglePlacementChoice(choices, controller, ensureMatches(CardInstance.class, availableTargets), usedThings, change, strength,
            true, true, activeContinuous, zoneFilter, cardFilter, usedFilter, materialReduction, materialLimit);
        return choices;
    }

    /** Returns the placement choice(s) a commander chooses from the given availableTargets, following other params. */
    private List<GameChoiceChange> getPlacementChoice(ControllerInstance controller,
                                                      List<LivingObject> availableTargets,
                                                      EffectChangeInstance change,
                                                      int strength,
                                                      boolean payingCost,
                                                      List<ContinuousInstance> activeContinuous,
                                                      Predicate<ZoneInstance> zoneFilter,
                                                      Predicate<CardInstance> cardFilter) {
        return getPlacementChoice(controller, availableTargets, change, strength, payingCost, activeContinuous,
            zoneFilter, cardFilter, m -> 0, l -> l);
    }

    /** Returns the placement choice(s) a commander chooses from the given availableTargets, following other params. */
    private List<GameChoiceChange> getPlacementChoice(ControllerInstance controller,
                                                      List<LivingObject> availableTargets,
                                                      EffectChangeInstance change,
                                                      int strength,
                                                      boolean payingCost,
                                                      List<ContinuousInstance> activeContinuous,
                                                      Predicate<ZoneInstance> zoneFilter,
                                                      Predicate<CardInstance> cardFilter,
                                                      Function<LivingObject, Integer> materialReduction,
                                                      Function<Integer, Integer> materialLimit) {
        List<GameChoiceChange> choices = new ArrayList<>();
        setSinglePlacementChoice(choices, controller, ensureMatches(CardInstance.class, availableTargets), List.of(), change, strength, payingCost, false, activeContinuous,
            zoneFilter,
            cardFilter,
            u -> true,
            materialReduction,
            materialLimit);
        return choices;
    }

    /**
     * Standardizes placement choices. Adds to the given placements List the placement choices made.
     * "Used By" must be set before using this function, if placing cards that use cards.
     * @param strength the number of cards to be placed
     * @param payingCost whether the card must use its material cost
     * @param usingPlacement whether the placement is allowed to use cards, null if it could or could not use cards
     * @param zoneFilter which zones the placement effect could place the card
     * @param cardFilter which cards could be placed by the effect
     * @param usedFilter which cards can be used by the effect (usually in a specific pile)
     * @param materialReduction how much the material of a specific placement is changed by (increased or reduced, added to baseMaterial)
     * @param materialLimit applied after materialReduction, usually to limit the minimum of materialReduction.
     */
    private void setSinglePlacementChoice(List<GameChoiceChange> placements,
                                          ControllerInstance controller,
                                          List<CardInstance> availableTargets,
                                          List<LivingObject> usedThings,
                                          EffectChangeInstance change,
                                          int strength,
                                          boolean payingCost,
                                          Boolean usingPlacement,
                                          List<ContinuousInstance> activeContinuous,
                                          Predicate<ZoneInstance> zoneFilter,
                                          Predicate<CardInstance> cardFilter,
                                          Predicate<LivingObject> usedFilter,
                                          Function<LivingObject, Integer> materialReduction,
                                          Function<Integer, Integer> materialLimit) {
        final List<ZoneInstance> zonesToPlace = getZoneChoices(change, activeContinuous).stream().filter(zoneFilter).toList(); // Ensures that zones are being targeted
        final CommanderInstance placingCommander = state.getCommander(change.getUser().getUser());
        int totalMaterialPaid = 0;
        // For the number of things to be placed
        for (int i = 0; i < strength; i++) {
            GameChoiceChange choice = getPlayerChangeChoice(controller, List.of(addSinglePlacement(placements, controller,
                availableTargets, usedThings, change, payingCost, usingPlacement, totalMaterialPaid, activeContinuous, zonesToPlace,
                placingCommander, cardFilter, usedFilter, materialReduction, materialLimit))).get(0);
            placements.add(choice);
            totalMaterialPaid += choice.getChoiceInfo().get(2) // Actual Material Paid
                + choice.getChoiceInfo().get(4); // Material cost increase based on cards used
        }
    }

    /** Helper function for setSinglePlacementChoice. Returns every choice for one placement. */
    private List<GameChoiceChange> addSinglePlacement(List<GameChoiceChange> placements,
                                                      ControllerInstance controller,
                                                      List<CardInstance> availableTargets,
                                                      List<LivingObject> usedThings,
                                                      EffectChangeInstance change,
                                                      boolean payingCost,
                                                      Boolean usingPlacement,
                                                      int previousMaterialPaid,
                                                      List<ContinuousInstance> activeContinuous,
                                                      List<ZoneInstance> zonesToPlace,
                                                      CommanderInstance payingCommander,
                                                      Predicate<CardInstance> cardFilter,
                                                      Predicate<LivingObject> usedFilter,
                                                      Function<LivingObject, Integer> materialFunction,
                                                      Function<Integer, Integer> materialLimit) {
        final List<GameChoiceChange> onePlacement = new ArrayList<>();
        // For every card that can be placed
        for (CardInstance card : availableTargets.stream() // Matches cardFilter, cannot have been selected for placement before, and placement conditions met
            .filter(c -> cardFilter.test(c) && placements.stream().noneMatch(g -> g.getTarget().contains(c))
                && (!payingCost || isEffectLegal(c.getInstancedPlaceCost(), activeContinuous, change.getEffectID()))).toList()) {

            final VariableGameNum baseMaterial = getMaterialPaymentNum(card, activeContinuous);
            // For every zone it can be placed
            for (ZoneInstance zone : getPlaceableZones(card, zonesToPlace, activeContinuous)) {
                final int baseMaterialNum = payingCost ? baseMaterial == null ? 0 : baseMaterial // 0 if not paying cost or baseMaterial = null
                    .getValue(state, controller, change, zone, activeContinuous) // GetValue targets the zone the card will be placed on.
                    : 0;
                final int actualMaterialNum = materialLimit.apply(baseMaterialNum + materialFunction.apply(card));
                // If cards are used to place the card
                if (payingCost && (usingPlacement == null ? isPlacedUsingCards(card) : usingPlacement)) {
                    // For every combination of used cards that match the filter and match the card's material cost
                    for (List<LivingObject> usableThings : getUsedMaterialCombinations(change, usedThings.stream().filter(usedFilter).toList(), card, zone.getPosition(), activeContinuous)) {
                        final VariableGameNum materialPerUsed = getMaterialPaymentNum(card, activeContinuous);
                        final int materialMulti = materialPerUsed == null ?
                            0
                            : materialPerUsed.getValue(state, sideIDtoController(card.getUser()), change, activeContinuous);
                        final int materialIncrease = usableThings.size() * materialMulti;
                        // If the number of used cards doesn't cost too much material to let the card be placed
                        if (isMaterialPayable(previousMaterialPaid + actualMaterialNum + materialIncrease, payingCommander, change, activeContinuous)) {
                            // Create a choice for the card to be placed.
                            onePlacement.add(new GameChoiceChange(
                                change,
                                card,
                                Stream.of(List.of(
                                    zone.getInstanceID(), // instanceID of zone to place on
                                    baseMaterialNum, // Base material cost of the card, 0 if the card doesn't have a base material cost
                                    actualMaterialNum, // Actual material cost paid
                                    usableThings.size(), // Number of cards used
                                    materialIncrease // Total material cost increase based on cards used
                                ), usableThings.stream().map(LivingObject::getInstanceID).toList()).flatMap(List::stream).toList() // Every used card is after.
                            ));
                        }
                    }
                }
                // If no cards are used to place the card
                else {
                    // If the card being placed wouldn't exceed the placement material doesn't cost too much material to let the card be placed
                    if (isMaterialPayable(previousMaterialPaid + baseMaterialNum, payingCommander, change, activeContinuous)) {
                        // Create a choice for the card to be placed. (Same format as above)
                        onePlacement.add(new GameChoiceChange(
                            change,
                            card, // Card to be placed.
                            List.of(
                                zone.getInstanceID(), // instanceID of zone to place on
                                baseMaterialNum, // Base material cost of the card, 0 if not paying cost
                                actualMaterialNum, // Actual material cost paid
                                0, // Number of cards used
                                0 // Total material cost increase based on cards used
                            )
                        ));
                    }
                }
            }
        }
        return onePlacement;
    }

    /** Returns a list of every unique combination that can be made of the given list of LivingObject whose material adds to the material cost of the cardToPlace. */
    private List<List<LivingObject>> getUsedMaterialCombinations(EffectStipulationObject effectUsing,
                                                                 List<LivingObject> usable,
                                                                 LivingObject cardToPlace,
                                                                 Position placementPosition,
                                                                 List<ContinuousInstance> activeContinuous) {
        if (!isPlacedUsingCards(cardToPlace)) return List.of(List.of());
        List<List<LivingObject>> result = new ArrayList<>();
        final List<CardInstance> placementBlockingCards = state.field.getZoneAt(placementPosition).getCards().stream().filter(CardInstance::isOccupying).toList();
        final int n = usable.size();
        if (n == 0) return result;
        final int materialNeeded = getMaterialPlaceCost(cardToPlace, activeContinuous).getValue(state, sideIDtoController(cardToPlace.getUser()), effectUsing, cardToPlace, activeContinuous);

        if (placementBlockingCards.isEmpty() || new HashSet<>(usable).containsAll(placementBlockingCards)) {
            int firstMaterial = 0;
            for (LivingObject used : usable) {
                firstMaterial += getMaterialPlaceCost(used, activeContinuous).getValue(state, sideIDtoController(used.getUser()), effectUsing, effectUsing.getUser(), activeContinuous);
            }
            if (firstMaterial == materialNeeded) return List.of(usable);
        }

        int[] c = new int[n];
        int k = 0;

        while (k < n) {
            if (c[k] < k) {
                int swapIndex = k % 2 == 0 ? 0 : c[k];

                swap(usable, swapIndex, k);

                // Combination must contain every card in the position to place
                if (placementBlockingCards.isEmpty() || new HashSet<>(usable).containsAll(placementBlockingCards)) {
                    // Combination must add exactly to material needed
                    int totalMaterial = 0;
                    for (int i = 0; i < n; i++) {
                        totalMaterial += getMaterialPlaceCost(usable.get(i), activeContinuous)
                            .getValue(state, sideIDtoController(usable.get(i).getUser()), effectUsing, effectUsing.getUser(), activeContinuous);
                        if (totalMaterial > materialNeeded) break;
                    }
                    // Add it if so
                    if (totalMaterial == materialNeeded) result.add(usable);
                }
                c[k]++;
                k = 0; // Reset k to generate new permutations
            } else {
                // Reset counter and move to next index
                c[k] = 0;
                k++;
            }
        }

        return result;
    }

    /** Abbreviation for setSinglePlayerChoice that automatically succeeds on the first check
     * and also only allows unique targets in addition to targetFilter. */
    private void setSingleTargetChoice(ControllerInstance controller,
                                       List<GameChoiceChange> oneChange,
                                       int strength,
                                       EffectChangeInstance change,
                                       List<LivingObject> targets,
                                       List<ContinuousInstance> activeContinuous,
                                       Predicate<LivingObject> targetFilter) {
        setSinglePlayerChoice(controller, oneChange, strength, change, targets, activeContinuous,
            o -> oneChange.isEmpty() || (oneChange.stream().noneMatch(c -> c.getTarget().contains(o)) && targetFilter.test(o)));
    }

    /** For effects whose strength determines the number of targets that can be selected, and not how effective the choice is. */
    private List<GameChoiceChange> getPlayerChoiceOfUniqueChoices(ControllerInstance controller,
                                                                  EffectChangeInstance change,
                                                                  int strength,
                                                                  List<LivingObject> targets,
                                                                  List<ContinuousInstance> activeContinuous) {
        List<GameChoiceChange> choice = new ArrayList<>();
        setSinglePlayerChoice(controller, choice, strength, change, targets, activeContinuous, o -> choice.stream().noneMatch(c -> c.getTarget().contains(o)));
        return choice;
    }

    /** Abbreviation for getPlayerChoiceOfChoices that doesn't allow multiple of the same target to be selected, in addition to the given Predicate. */
    private List<GameChoiceChange> getPlayerChoiceOfUniqueChoices(ControllerInstance controller,
                                                                  EffectChangeInstance change,
                                                                  int strength,
                                                                  List<LivingObject> targets,
                                                                  List<ContinuousInstance> activeContinuous,
                                                                  Predicate<LivingObject> filter) {
        List<GameChoiceChange> choice = new ArrayList<>();
        setSinglePlayerChoice(controller, choice, strength, change, targets, activeContinuous, o -> filter.test(o) && choice.stream().noneMatch(c -> c.getTarget().contains(o)));
        return choice;
    }

    /** For effects whose strength determines the number of targets that can be selected, and not how effective the choice is. */
    private List<GameChoiceChange> getPlayerChoiceOfChoices(ControllerInstance controller,
                                                            EffectChangeInstance change,
                                                            int strength,
                                                            List<ContinuousInstance> activeContinuous,
                                                            List<LivingObject> targets) {
        List<GameChoiceChange> choice = new ArrayList<>();
        setSinglePlayerChoice(controller, choice, strength, change, targets, activeContinuous, o -> true);
        return choice;
    }

    /** For individual changes that can do multiple things in one choice, this function adds to oneChange
     * the sequential choices chosen by the controller, which follow the provided filter.
     * The filter should check the state of oneChange.
     */
    private void setSinglePlayerChoice(ControllerInstance controller,
                                       List<GameChoiceChange> oneChange,
                                       int strength,
                                       EffectChangeInstance change,
                                       List<LivingObject> targets,
                                       List<ContinuousInstance> activeContinuous,
                                       Predicate<LivingObject> filter) {
        // For the number of choices to add
        for (int i = 0; i < strength; i++) {
            // Generate a list of one choice. Add active continuous checks.
            List<GameChoiceChange> oneRemoval = new ArrayList<>();
            targets.stream().filter(filter).toList().forEach(o ->
                oneRemoval.add(new GameChoiceChange(change, o, List.of())));

            // Add the choice the player chooses to the given list
            oneChange.add(getPlayerChangeChoice(controller, List.of(oneRemoval)).get(0));
        }
    }

    /** Returns the Max Hand Size of the given commander for the aftermath discard. */
    private int getMaxHandSize(CommanderInstance check, List<ContinuousInstance> activeContinuous) {
        return 6 + getContinuousThatApplyOnObject(check, activeContinuous).stream()
            .filter(c -> c.effect().getTypeInstance() == EffectChangeType.IncreaseHandSize)
            .mapToInt(c -> c.effect().getStrength().getValue(state, sideIDtoController(check.getSide()), c.effect(), activeContinuous))
            .sum();
    }

    /** Returns a player's collection, which is the strength of Production continuous they have on their field. */
    private int getCollectionMaterial(CommanderInstance check, List<ContinuousInstance> activeContinuous) {
        return getAvailableEffects(activeContinuous).stream().filter(c -> check == state.getCommander(c.getUser().getUser()))
            .flatMap(e -> e.getInstanceEffectChanges().stream()
                .filter(c -> c.getType() == EffectChangeType.Production
                    && isEffectLegal(c, activeContinuous)))
            .mapToInt(p -> p.getStrength()
                .getValue(state, sideIDtoController(p.getUser().getUser()), p, activeContinuous))
            .sum();
    }

    /* --- TargetType Resolution Functions --- */
    /** Abbreviation to find zones specifically. */
    private List<ZoneInstance> getZoneChoices(EffectStipulationObject usingEffect, List<ContinuousInstance> activeConstantContinuous) {
        return findChoicesFromList(ZoneInstance.class, state.getZones(), usingEffect, activeConstantContinuous);
    }

    /** Abbreviation to find zones specifically. */
    private List<ZoneInstance> getZoneChoices(EffectStipulationObject usingEffect, List<LivingObject> targets, List<ContinuousInstance> activeConstantContinuous) {
        return findChoicesFromList(ZoneInstance.class, ensureMatches(ZoneInstance.class, targets), usingEffect, activeConstantContinuous);
    }

    /** Abbreviation to find commanders specifically. */
    private List<CommanderInstance> getCommanderChoices(EffectStipulationObject usingEffect, List<ContinuousInstance> activeConstantContinuous) {
        return findChoicesFromList(CommanderInstance.class, state.getCommanders(), usingEffect, activeConstantContinuous);
    }

    /** Abbreviation to find commanders specifically. */
    private List<CommanderInstance> getCommanderChoices(EffectStipulationObject usingEffect, List<LivingObject> targets, List<ContinuousInstance> activeConstantContinuous) {
        return findChoicesFromList(CommanderInstance.class, ensureMatches(CommanderInstance.class, targets), usingEffect, activeConstantContinuous);
    }

    /** Abbreviation to find cards specifically. */
    private List<CardInstance> getCardChoices(EffectStipulationObject usingEffect, List<ContinuousInstance> activeConstantContinuous) {
        return findChoicesFromList(CardInstance.class, state.getCards(), usingEffect, activeConstantContinuous);
    }

    /** Abbreviation to find cards specifically. */
    private List<CardInstance> getCardChoices(EffectStipulationObject usingEffect, List<LivingObject> targets, List<ContinuousInstance> activeConstantContinuous) {
        return findChoicesFromList(CardInstance.class, ensureMatches(CardInstance.class, targets), usingEffect, activeConstantContinuous);
    }

    /** Finds every LivingObject that matches the target. */
    private List<LivingObject> getLivingChoices(EffectStipulationObject usingEffect, List<ContinuousInstance> activeConstantContinuous) {
        return findChoicesFromList(LivingObject.class, state.getLivingObjects(), usingEffect, activeConstantContinuous);
    }

    /** Finds every LivingObject that matches the target. */
    private List<LivingObject> getLivingChoices(EffectStipulationObject usingEffect, List<LivingObject> targets, List<ContinuousInstance> activeConstantContinuous) {
        return findChoicesFromList(LivingObject.class, targets, usingEffect, activeConstantContinuous);
    }

    /** Returns whether the given check is targeted by the given effectStipulation. */
    private <T extends LivingObject> boolean isTargetedBy(Class<T> type,
                                                          T check,
                                                          ImmutableEffectStipulationObject usingEffect,
                                                          List<ContinuousInstance> activeContinuous) {
        return !findChoicesFromList(type, List.of(check), usingEffect, activeContinuous).isEmpty();
    }

    /**
     * Finds every available living thing (card, commander, or zone) that fits the
     * described target for an effectStipulation in the given list.
     */
    private <T extends LivingObject> List<T> findChoicesFromList(Class<T> type,
                                                                 List<T> list,
                                                                 ImmutableEffectStipulationObject usingEffect,
                                                                 List<ContinuousInstance> activeConstantContinuous) {
        final List<TargetType> targetType = usingEffect.getTargetInstance();
        if (targetType == null || targetType.isEmpty() || targetType.contains(TargetType.Gamestate)) return List.of();

        final LivingObject user = usingEffect.getUser();
        List<T> availableChoices = new ArrayList<T>(list);
        final List<TargetType> targetTypes = usingEffect.getTargetInstance();

        int i = 0;
        // First checks for pattern targets, which are always at the beginning of the target list.
        if (usingEffect.getUser().getPosition().getBoardLocation() == BoardLocation.FIELD) {
            final List<TargetType> patternTarget = new ArrayList<>(6); // usually actions don't have more than 6 params
            while (i < targetTypes.size() && TargetType.isPatternTarget(targetTypes.get(i))) {
                patternTarget.add(targetTypes.get(i));
                i++;
            }
            // convert to a list of position based on the user's position
            final List<Position> targetPositions = new ArrayList<Position>(patternTarget
                .stream()
                .map(target -> patternTargetToPosition(user.getUser(), target, user.getPosition(), activeConstantContinuous))
                .toList());

            availableChoices.removeIf(candidate -> candidate.getPosition().getBoardLocation() != BoardLocation.FIELD
                || !targetPositions.contains(candidate.getPosition()));
        }

        // Written targets
        while (i < targetTypes.size()) {
            final TargetType currentType = targetTypes.get(i);
            switch(currentType) {
                case Target ->
                    availableChoices.removeIf( candidate -> {
                        if (candidate.getPosition().getBoardLocation() == BoardLocation.FIELD) {
                            return user.getTarget().stream().noneMatch(t -> t.isOnFieldTarget(candidate));
                        }
                        else {
                            return user.getTarget().stream().noneMatch(t -> t.isOffFieldTarget(candidate));
                        }
                    });

                case ExceptUsersTargets ->
                    availableChoices.removeIf( candidate -> {
                        if (candidate.getPosition().getBoardLocation() == BoardLocation.FIELD) {
                            return user.getTarget().stream().anyMatch(t -> t.isOnFieldTarget(candidate));
                        }
                        else {
                            return user.getTarget().stream().anyMatch(t -> t.isOffFieldTarget(candidate));
                        }
                    });

                case DeclaredName ->
                    availableChoices.removeIf(candidate -> candidate.getOriginalID() != user.getDeclaredName());

                case DeclaredAttribute ->
                    availableChoices.removeIf(candidate -> candidate.getInstanceAttribute() != user.getDeclaredAttribute());

                case DeclaredPile ->
                    availableChoices.removeIf(candidate -> candidate.getPosition().getBoardLocation() != user.getDeclaredPile().position.getBoardLocation());

                case InDeclaredDirectionFromSelf -> {
                    switch (user.getDeclaredDirection()) {
                        case FORWARD -> {
                            final int file = user.getPosition().getFile();
                            availableChoices.removeIf(candidate -> user.getController() == SideID.ONE ?
                                candidate.getPosition().getFile() <= file
                                : candidate.getPosition().getFile() >= file);
                        }
                        case RIGHT -> {
                            final int lane = user.getPosition().getLane();
                            availableChoices.removeIf(candidate -> user.getController() == SideID.ONE ?
                                candidate.getPosition().getLane() <= lane
                                : candidate.getPosition().getLane() >= lane);
                        }
                        case BACKWARD -> {
                            final int file = user.getPosition().getFile();
                            availableChoices.removeIf(candidate -> user.getController() == SideID.ONE ?
                                candidate.getPosition().getFile() >= file
                                : candidate.getPosition().getFile() <= file);
                        }
                        case LEFT -> {
                            final int lane = user.getPosition().getLane();
                            availableChoices.removeIf(candidate -> user.getController() == SideID.ONE ?
                                candidate.getPosition().getLane() >= lane
                                : candidate.getPosition().getLane() <= lane);
                        }
                    }
                }

                // These are checked first and/or used by themselves
                case Self, OwningCommander, ControllingCommander, UsingCommander, OpponentCommander ->
                    availableChoices = new ArrayList<T>(ensureMatches(type, switch (currentType) {
                        case Self -> usingEffect.getUser();
                        case OwningCommander -> state.getCommander(user.getOwnerInstance());
                        case ControllingCommander -> state.getCommander(user.getController());
                        case UsingCommander -> state.getCommander(user.getUser());
                        case OpponentCommander -> state.getCommander(user.getUser().opponentOf());
                        default -> throw new IllegalArgumentException("How did we get here?");
                    }));

                case Commanders -> {
                    availableChoices.removeIf(c -> !(c instanceof CommanderInstance));
                }

                case NotSelf ->
                    availableChoices.removeIf(candidate -> candidate.getInstanceID() == user.getInstanceID());

                // Zone targets also include the zone's occupants. Unless it's ZonesOnly specifically.
                case ZonesOnly -> {
                    final List<ZoneInstance> zones = state.getZones();
                    availableChoices.removeIf(candidate -> {
                        final int candidateID = candidate.getInstanceID();
                        for (ZoneInstance zone : zones) {
                            if (candidateID == zone.getInstanceID()) return false;
                        }
                        return true;
                    });
                }

                case FieldZones, OccupiedZones, UnoccupiedZones, OpenFieldZones, AttackZones, FrontlineZones, BacklineZones,
                     ProductionZones, CloseProductionZones, FarProductionZones, ZonesAdjacentToThis, ZonesYouControl,
                     ZonesYouDontControl, ZonesInThisLane, ZonesInThisFile -> {

                    final List<ZoneInstance> zones = new ArrayList<>(state.getZones());
                    zones.removeIf(candidate -> switch(currentType) {
                        case FieldZones -> false;
                        case OccupiedZones -> !candidate.isOccupied();
                        case UnoccupiedZones -> candidate.isOccupied();
                        case OpenFieldZones -> !(candidate.getType() == ZoneType.OPEN_FIELD);
                        case AttackZones -> !(candidate.getType().isAttackZone());
                        case FrontlineZones -> !(candidate.getType() == ZoneType.FRONTLINE);
                        case BacklineZones -> !(candidate.getType() == ZoneType.BACKLINE);
                        case ProductionZones -> !(candidate.getType().isProductionZone());
                        case CloseProductionZones -> !(candidate.getType() == ZoneType.CLOSE_PRODUCTION);
                        case FarProductionZones -> !(candidate.getType() == ZoneType.FAR_PRODUCTION);
                        case ZonesAdjacentToThis -> !(candidate.getPosition().isAdjacent(user.getPosition()));
                        case ZonesYouControl -> candidate.getController() != user.getUser();
                        case ZonesYouDontControl -> candidate.getController() == user.getUser();
                        case ZonesInThisLane -> candidate.getPosition().getLane() != user.getPosition().getLane();
                        case ZonesInThisFile -> candidate.getPosition().getFile() != user.getPosition().getFile();
                        default -> false;
                    });
                    availableChoices.removeIf(candidate -> zones.stream()
                        .map(ZoneInstance::getPosition)
                        .noneMatch(pos -> pos.equals(candidate.getPosition())));
                }

                case ZonesOnYourSide, ZonesOnYourOpponentsSide -> {
                    // must first find the side
                    final List<ZoneInstance> zonesOnSide = new ArrayList<>(state.field.getSide(currentType == TargetType.ZonesOnYourSide ? user.getUser() : state.getCommander(user.getUser().opponentOf()).getSide()));

                    // then resolve
                    availableChoices.removeIf(candidate -> zonesOnSide.stream()
                        .map(ZoneInstance::getPosition)
                        .noneMatch(pos -> pos.equals(candidate.getPosition())));
                }

                case ZonesAdjacentToTarget, ZonesInTargetsLane, ZonesInTargetsFile -> {
                    // must first find the target(s)
                    final List<ZoneInstance> zoneTargets = new ArrayList<>(state.getZones());
                    zoneTargets.removeIf(candidate -> user.getTarget().stream().noneMatch(t -> t.isOnFieldTarget(candidate)));

                    // then resolve
                    final List<ZoneInstance> zones = new ArrayList<>(state.getZones());
                    switch(currentType) {
                        case ZonesAdjacentToTarget -> zones.removeIf(candidate -> zoneTargets.stream()
                            .noneMatch(z -> z.getPosition().isAdjacent(candidate.getPosition())));
                        case ZonesInTargetsLane -> zones.removeIf(candidate -> zoneTargets.stream()
                            .noneMatch(z -> z.getPosition().getLane() == candidate.getPosition().getLane()));
                        case ZonesInTargetsFile -> zones.removeIf(candidate -> zoneTargets.stream()
                            .noneMatch(z -> z.getPosition().getFile() == candidate.getPosition().getFile()));
                    }

                    availableChoices.removeIf(candidate -> zones.stream()
                        .map(ZoneInstance::getPosition)
                        .noneMatch(pos -> pos.equals(candidate.getPosition())));
                }

                case Cards, CardsExceptThisCard, CardsYouOwn, CardsYouDontOwn, CardsYouControl, CardsYouDontControl,
                     CardsYouUse, CardsYouDontUse,
                     AttackCards, ProductionCards, DecisiveCards, TokenCards,
                     HumanCards, MachineCards, RockCards, PlantCards, WindCards, WaterCards, FlameCards, BeastCards,
                     DivineCards, GhoulCards, EtherCards, RitualCards,
                     FaceUpCards, FaceDownCards, ShroudableCards, ShroudedCards, CardsShroudedThisTurn,
                     CardsNotShroudedThisTurn, RevealedCards, UnrevealedCards, DestroyedCards, NotDestroyedCards,
                     RidingCards, RodeCards, CardsOnTheField, CardsInHand, CardsInDeck, CardsInDecisiveDeck,
                     CardsInDestroyedPile, CardsInDiscardPile, CardsInDisplacedPile,
                     CardsNotOnTheField, CardsNotInHand, CardsNotInDeck, CardsNotInDecisiveDeck, CardsNotDestroyed,
                     CardsNotDiscarded, CardsNotDisplaced -> {
                    availableChoices.removeIf(candidate -> {
                        if (!(candidate instanceof CardInstance)) return true;
                        return switch (currentType) {
                            case Cards -> false;
                            case CardsExceptThisCard -> candidate.equals(user);
                            case CardsYouOwn -> candidate.getOwnerInstance() != user.getUser();
                            case CardsYouDontOwn -> candidate.getOwnerInstance() == user.getOwnerInstance();
                            case CardsYouControl -> candidate.getController() != user.getUser();
                            case CardsYouDontControl -> candidate.getController() == user.getUser();
                            case CardsYouUse -> candidate.getUser() != user.getUser();
                            case CardsYouDontUse -> candidate.getUser() == user.getUser();
                            case AttackCards -> {
                                final CardType cardType = candidate.getCardTypeInstance();
                                yield cardType == CardType.ATTACK || cardType == CardType.DECISIVE_ATTACK || cardType == CardType.TOKEN_ATTACK;
                            }
                            case ProductionCards -> {
                                final CardType cardType = candidate.getCardTypeInstance();
                                yield cardType == CardType.PRODUCTION || cardType == CardType.DECISIVE_PRODUCTION || cardType == CardType.TOKEN_PRODUCTION;
                            }
                            case DecisiveCards -> {
                                final CardType cardType = candidate.getCardTypeInstance();
                                yield cardType == CardType.DECISIVE_ATTACK || cardType == CardType.DECISIVE_PRODUCTION;
                            }
                            case TokenCards -> {
                                final CardType cardType = candidate.getCardTypeInstance();
                                yield cardType == CardType.TOKEN_ATTACK || cardType == CardType.TOKEN_PRODUCTION;
                            }
                            case HumanCards -> candidate.getInstanceAttribute() != CardAttribute.Human;
                            case MachineCards -> candidate.getInstanceAttribute() != CardAttribute.Machine;
                            case RockCards -> candidate.getInstanceAttribute() != CardAttribute.Rock;
                            case PlantCards -> candidate.getInstanceAttribute() != CardAttribute.Plant;
                            case WindCards -> candidate.getInstanceAttribute() != CardAttribute.Wind;
                            case WaterCards -> candidate.getInstanceAttribute() != CardAttribute.Water;
                            case FlameCards -> candidate.getInstanceAttribute() != CardAttribute.Flame;
                            case BeastCards -> candidate.getInstanceAttribute() != CardAttribute.Beast;
                            case DivineCards -> candidate.getInstanceAttribute() != CardAttribute.Divine;
                            case EtherCards -> candidate.getInstanceAttribute() != CardAttribute.Ether;
                            case RitualCards -> candidate.getInstanceAttribute() != CardAttribute.Ritual;
                            case ExceptHumanCards -> candidate.getInstanceAttribute() == CardAttribute.Human;
                            case ExceptMachineCards -> candidate.getInstanceAttribute() == CardAttribute.Machine;
                            case ExceptRockCards -> candidate.getInstanceAttribute() == CardAttribute.Rock;
                            case ExceptPlantCards -> candidate.getInstanceAttribute() == CardAttribute.Plant;
                            case ExceptWindCards -> candidate.getInstanceAttribute() == CardAttribute.Wind;
                            case ExceptWaterCards -> candidate.getInstanceAttribute() == CardAttribute.Water;
                            case ExceptFlameCards -> candidate.getInstanceAttribute() == CardAttribute.Flame;
                            case ExceptBeastCards -> candidate.getInstanceAttribute() == CardAttribute.Beast;
                            case ExceptDivineCards -> candidate.getInstanceAttribute() == CardAttribute.Divine;
                            case ExceptEtherCards -> candidate.getInstanceAttribute() == CardAttribute.Ether;
                            case ExceptRitualCards -> candidate.getInstanceAttribute() == CardAttribute.Ritual;
                            case FaceUpCards -> state.getCard(candidate.getInstanceID()).isFaceUp();
                            case FaceDownCards -> !state.getCard(candidate.getInstanceID()).isFaceUp();
                            case ShroudableCards -> !isShroudable(state.getCard(candidate.getInstanceID()), activeConstantContinuous);
                            case ShroudedCards -> !candidate.isShrouded();
                            case CardsShroudedThisTurn ->
                                justHappenedThisTurn(e -> e.getTarget() == candidate.getInstanceID()
                                    && e.getChange().getType().getSimultNum() == 29); // Shroud
                            case RevealedCards -> !candidate.isRevealed();
                            case UnrevealedCards -> candidate.isRevealed();
                            case DestroyedCards -> !candidate.isDestroyed();
                            case NotDestroyedCards -> candidate.isDestroyed();
                            case RidingCards -> candidate.getPosition().getRideState() != CardRideState.RIDER;
                            case RodeCards -> candidate.getPosition().getRideState() != CardRideState.RODE;

                            case CardsOnTheField -> candidate.getPosition().getBoardLocation() != BoardLocation.FIELD;
                            case CardsInHand ->
                                !(candidate.getPosition().getBoardLocation() == BoardLocation.HAND_ONE ||
                                    candidate.getPosition().getBoardLocation() == BoardLocation.HAND_TWO);
                            case CardsInDeck ->
                                !(candidate.getPosition().getBoardLocation() == BoardLocation.DECK_ONE ||
                                    candidate.getPosition().getBoardLocation() == BoardLocation.DECK_TWO);
                            case CardsInDecisiveDeck ->
                                !(candidate.getPosition().getBoardLocation() == BoardLocation.DECISIVE_PILE_ONE ||
                                    candidate.getPosition().getBoardLocation() == BoardLocation.DECISIVE_PILE_TWO);
                            case CardsInDestroyedPile ->
                                !(candidate.getPosition().getBoardLocation() == BoardLocation.DESTROYED_PILE_ONE ||
                                    candidate.getPosition().getBoardLocation() == BoardLocation.DESTROYED_PILE_TWO);
                            case CardsInDiscardPile ->
                                !(candidate.getPosition().getBoardLocation() == BoardLocation.DISCARD_PILE_ONE ||
                                    candidate.getPosition().getBoardLocation() == BoardLocation.DISCARD_PILE_TWO);
                            case CardsInDisplacedPile ->
                                !(candidate.getPosition().getBoardLocation() == BoardLocation.DISPLACED_PILE_ONE ||
                                    candidate.getPosition().getBoardLocation() == BoardLocation.DISPLACED_PILE_TWO);

                            case CardsNotOnTheField -> candidate.getPosition().getBoardLocation() == BoardLocation.FIELD;
                            case CardsNotInHand ->
                                candidate.getPosition().getBoardLocation() == BoardLocation.HAND_ONE ||
                                candidate.getPosition().getBoardLocation() == BoardLocation.HAND_TWO;
                            case CardsNotInDeck ->
                                candidate.getPosition().getBoardLocation() == BoardLocation.DECK_ONE ||
                                candidate.getPosition().getBoardLocation() == BoardLocation.DECK_TWO;
                            case CardsNotInDecisiveDeck ->
                                candidate.getPosition().getBoardLocation() == BoardLocation.DECISIVE_PILE_ONE ||
                                candidate.getPosition().getBoardLocation() == BoardLocation.DECISIVE_PILE_TWO;
                            case CardsNotDestroyed ->
                                candidate.getPosition().getBoardLocation() == BoardLocation.DESTROYED_PILE_ONE ||
                                candidate.getPosition().getBoardLocation() == BoardLocation.DESTROYED_PILE_TWO;
                            case CardsNotDiscarded ->
                                candidate.getPosition().getBoardLocation() == BoardLocation.DISCARD_PILE_ONE ||
                                candidate.getPosition().getBoardLocation() == BoardLocation.DISCARD_PILE_TWO;
                            case CardsNotDisplaced ->
                                candidate.getPosition().getBoardLocation() == BoardLocation.DISPLACED_PILE_ONE ||
                                candidate.getPosition().getBoardLocation() == BoardLocation.DISPLACED_PILE_TWO;
                            default -> throw new IllegalArgumentException("How did we get here?");
                        };
                    });
                }

                case WithFortificationCounters, WithFrozenCounters, WithFlameCounters, WithInfectionCounters,
                     WithEtherealCounters, WithMomentumCounters, WithEnergyCounters,
                     WithBountyCounters, WithScrapCounters, WithTideCounters, WithRageCounters,
                     WithSturdyCounters, WithSiGNLCounters, WithTravelCounters, WithFervorCounters,
                     WithStorageCounters, WithUndeadCounters, WithVirtueCounters, WithSinCounters,
                     WithDreamCounters, WithInjectionCounters, WithVeilCounters, WithOverchargeCounters -> {
                    availableChoices.removeIf(candidate -> !candidate.hasCounter(CounterType.convertCounter(currentType)));
                }

                case WithEchoCounters -> {
                    availableChoices.removeIf(candidate -> candidate.hasCounter(CounterType.getEchoCounters()));
                }

                case WithCounters -> {
                    availableChoices.removeIf(candidate -> candidate.getCounters().isEmpty());
                }

                case PlacementCostIsMet ->
                    availableChoices.removeIf(candidate -> !isEffectLegal(candidate.getInstancedPlaceCost(), activeConstantContinuous));

                case CurrentlyUsable ->
                    availableChoices.removeIf(candidate -> candidate.getUsedByEffectID().stream().noneMatch(id -> id == usingEffect.getEffectID()));

                case PlacementCostCanBeMetByUsersFieldCards, PlacementCostCanBeMetByUsersCardsInHand,
                     PlacementCostCanBeMetByUsersDestroyedCards, PlacementCostCanBeMetByUsersDiscardedCards,
                     PlacementCostCanBeMetByUsersDisplacedCards, PlacementCostCanBeMetByUsersDeck,
                     PlacementCostCanBeMetByUsersDecisivePileCards, PlacementCostCanBeMetByTargets,
                     PlacementCostCanBeMetByAnyFieldCards, PlacementCostCanBeMetByOpponentsFieldCards -> {

                    final List<CardInstance> cardsToBeUsed = getUsableList(currentType, user, activeConstantContinuous);

                    availableChoices.removeIf(candidate -> {
                        EffectChangeInstance materialPayment = getMaterialPayment(candidate, activeConstantContinuous);
                        if (materialPayment.getChangeConditions().size() < 2) return true;
                        switch (materialPayment.getChangeConditions().get(1).getType()) { // The type of thing available, based on the EffectChange getChangeCondition order.
                            case MaterialAvailable -> {
                                // Make a list of usable material
                                final List<Integer> availableMaterial = new ArrayList<Integer>(cardsToBeUsed.stream()
                                    .filter(c -> {
                                        List<T> given = ensureMatches(type, c);
                                        if (given.isEmpty()) return false;
                                        return c.getInstanceID() != candidate.getInstanceID() // A card can't use itself, no matter what.
                                        && isUsable(c, activeConstantContinuous)
                                        // A card may only use cards which match with its material available target.
                                        && isTargetedBy(type, given.get(0), materialPayment, activeConstantContinuous);
                                    })
                                    .map(c -> getMaterialPlaceCost(c, activeConstantContinuous).getValue(state, sideIDtoController(usingEffect.getUser().getUser()), usingEffect, candidate, activeConstantContinuous))
                                    .toList());

                                final int candidateMaterial = getMaterialPlaceCost(candidate, activeConstantContinuous)
                                    .getValue(state, sideIDtoController(usingEffect.getUser().getUser()), usingEffect, candidate, activeConstantContinuous);

                                return !canMakeExactSum(availableMaterial, candidateMaterial);
                            }
                            case CountersAvailable -> { // Specific counter types to check will be added as different effectChangeType, if needed
                                final int availableCounters = cardsToBeUsed.stream()
                                    .filter(c -> {
                                        List<T> given = ensureMatches(type, c);
                                        if (given.isEmpty()) return false;
                                        return c.getInstanceID() != candidate.getInstanceID() // A card can't use itself, no matter what.
                                            && isUsable(c, activeConstantContinuous)
                                            // A card may only use cards which match with its material available target.
                                            && isTargetedBy(type, given.get(0), materialPayment, activeConstantContinuous);
                                    })
                                    .map(CardInstance::getCounters)
                                    .flatMap(List::stream)
                                    .toList()
                                    .size();

                                final int neededCounters = candidate.getInstancedPlaceCost().getConditions().get(0).getCheck()
                                    .getValue(state, sideIDtoController(usingEffect.getUser().getUser()), usingEffect, candidate, activeConstantContinuous);
                                return neededCounters > availableCounters;
                            }
                            case HealthAvailable -> {
                                int availableHealth = cardsToBeUsed.stream()
                                    .filter(c -> {
                                        List<T> given = ensureMatches(type, c);
                                        if (given.isEmpty()) return false;
                                        return c.getInstanceID() != candidate.getInstanceID() // A card can't use itself, no matter what.
                                            && isUsable(c, activeConstantContinuous)
                                            && c.getPosition().getBoardLocation() == BoardLocation.FIELD // Can only take health from cards on the field
                                            // A card may only use cards which match with its material available target.
                                            && isTargetedBy(type, given.get(0), materialPayment, activeConstantContinuous);
                                    })
                                    .mapToInt(CardInstance::getHealthMax)
                                    .sum();

                                final int neededHealth = candidate.getInstancedPlaceCost().getConditions().get(0).getCheck()
                                    .getValue(state, sideIDtoController(usingEffect.getUser().getUser()), usingEffect, candidate, activeConstantContinuous);
                                return neededHealth > availableHealth;
                            }
                            default -> { return true; } // the card doesn't have a "uses cards" condition first, so it doesn't use cards to place, thus it needs to be removed.
                        }
                    });
                }

                case isPlaceable -> availableChoices.removeIf(a -> !isEffectLegal(a.getInstancedPlaceCost(), activeConstantContinuous)
                || isContinuousTypeOnObject(a, EffectChangeType.NotPlaceable, activeConstantContinuous));
                case isTargetable -> availableChoices.removeIf(a -> isContinuousTypeOnObject(a, EffectChangeType.NotTargetable, activeConstantContinuous));
                case isDamageable -> availableChoices.removeIf(a -> isContinuousTypeOnObject(a, EffectChangeType.NotDamageable, activeConstantContinuous));

                default -> throw new IllegalArgumentException("Target Type (" + currentType + ") not yet implemented.");
            }
            if (availableChoices.isEmpty()) break;
            i++;
        }
        return availableChoices;
    }

    /** Returns the position a pattern target would direct to with a given starting position and perspective. */
    private Position patternTargetToPosition(SideID perspective, TargetType pattern, Position startingPosition, List<ContinuousInstance> activeContinuous) {
        final int multi = perspective == SideID.ONE ? 1 : -1;
        final int lane = startingPosition.getLane() + switch(pattern) {
            case Zone3f3l, Zone2f3l, Zone1f3l, Zone3l, Zone1b3l, Zone2b3l, Zone3b3l -> 3 * multi;
            case Zone3f2l, Zone2f2l, Zone1f2l, Zone2l, Zone1b2l, Zone2b2l, Zone3b2l -> 2 * multi;
            case Zone3f1l, Zone2f1l, Zone1f1l, Zone1l, Zone1b1l, Zone2b1l, Zone3b1l -> multi;
            case Zone3f,   Zone2f,   Zone1f,  ZoneSelf, Zone1b,  Zone2b,   Zone3b   -> 0;
            case Zone3f1r, Zone2f1r, Zone1f1r, Zone1r, Zone1b1r, Zone2b1r, Zone3b1r -> -1 * multi;
            case Zone3f2r, Zone2f2r, Zone1f2r, Zone2r, Zone1b2r, Zone2b2r, Zone3b2r -> -2 * multi;
            case Zone3f3r, Zone2f3r, Zone1f3r, Zone3r, Zone1b3r, Zone2b3r, Zone3b3r -> -3 * multi;
            default -> throw new IllegalArgumentException("Given pattern for patternToPosition " + pattern + " is invalid.");
        };
        final int file = startingPosition.getFile() + switch(pattern) {
            case Zone3f3l, Zone3f2l, Zone3f1l, Zone3f, Zone3f1r, Zone3f2r, Zone3f3r -> 3 * multi;
            case Zone2f3l, Zone2f2l, Zone2f1l, Zone2f, Zone2f1r, Zone2f2r, Zone2f3r -> 2 * multi;
            case Zone1f3l, Zone1f2l, Zone1f1l, Zone1f, Zone1f1r, Zone1f2r, Zone1f3r -> multi;
            case Zone3l,   Zone2l,   Zone1l,  ZoneSelf, Zone1r,  Zone2r,   Zone3r   -> 0;
            case Zone1b3l, Zone1b2l, Zone1b1l, Zone1b, Zone1b1r, Zone1b2r, Zone1b3r -> -1 * multi;
            case Zone2b3l, Zone2b2l, Zone2b1l, Zone2b, Zone2b1r, Zone2b2r, Zone2b3r -> -2 * multi;
            case Zone3b3l, Zone3b2l, Zone3b1l, Zone3b, Zone3b1r, Zone3b2r, Zone3b3r -> -3 * multi;
            default -> throw new IllegalArgumentException("Given pattern for patternToPosition " + pattern + " is invalid.");
        };

        return Position.Field(lane, file, CardRideState.NORMAL);
    }

    /** Abbreviation for getPlaceableZones that checks every zone in the GameState, instead of some subset. */
    private List<ZoneInstance> getPlaceableZones(LivingObject card, List<ContinuousInstance> activeContinuous) {
        return getPlaceableZones(card, state.getZones(), activeContinuous);
    }

    /** Returns the zones from the given list in which a card can be placed. */
    private List<ZoneInstance> getPlaceableZones(LivingObject card, List<ZoneInstance> zones, List<ContinuousInstance> activeContinuous) {
        final EffectChangeInstance selfPlacementEffect = getSelfPlacementEffect(card);
        if (selfPlacementEffect == null) {
            return zones.stream()
                .filter(zone -> !zone.isOccupied()) // The check for correct zones (being on your side, etc.) is done by the card.
                .toList();
        }
        else return findChoicesFromList(ZoneInstance.class, zones.stream()
            .filter(zone -> !zone.isOccupied())
            .toList(),
            selfPlacementEffect, activeContinuous);
    }

    private boolean isPlaceableCardInList(List<CardInstance> cards, List<ContinuousInstance> activeContinuous) {
        return cards.stream().anyMatch(c -> isEffectLegal(c.getInstancedPlaceCost(), activeContinuous));
    }

    /** Returns whether a card has an "Unshroud" effect that isn't negated, and can be done with activeContinuous. */
    private boolean isShroudable(CardInstance check, List<ContinuousInstance> activeContinuous) {
        return check.getInstancedEffects().stream()
            .anyMatch(candidate -> candidate.getType() == EffectType.Unshroud
                && !(candidate.isNegated() || candidate.getUser().isNegated()));
    }

    /** Returns whether a card's placement condition includes MaterialAvailable, CountersAvailable, or HealthAvailable. */
    private boolean isPlacedUsingCards(LivingObject check) {
        return check.getInstancedPlaceCost().getInstanceConditions().stream()
            .anyMatch(effect -> ConditionType.isConditionForUse(effect.getType()));
    }

    /** Returns if the given effectObject is in/is an action which deals damage and/or adds counters. */
    private boolean isAttack(EffectObject action) {
        return action.getEffectFrom().getType() == EffectType.Action
            && action.getEffectFrom().getInstanceEffectChanges().stream()
            .anyMatch(e -> {
                final int effectSimult = e.getTypeInstance().getSimultNum();
                return effectSimult == 19 || effectSimult == 20 || effectSimult == 21 || effectSimult == 22;
            });
    }

    /** Returns if check is usable for placement, based on the given usedByEffectID. */
    private boolean isUsable(LivingObject check, int usedByEffectID) {
        return check.getUsedByEffectID().contains(usedByEffectID);
    }

    private List<LivingObject> getUsableList(EffectChangeType used, LivingObject user, List<ContinuousInstance> activeContinuous) {
        //noinspection unchecked
        return (List<LivingObject>) switch(used) { // Unchecked cast.
            case PlaceUsingZonesYouControl -> state.field.getZones().stream()
                .filter(z -> z.getController() == user.getUser())
                .toList();
            case PlaceUsingYourCardsOnField -> state.field.getCards().stream()
                .filter(c -> c.getController() == user.getUser()).toList();
            case PlaceUsingHand, PlaceUsingHandOrWithEffect -> state.getCommander(user.getUser()).getHand();
            case PlaceUsingDestroyed -> state.getCommander(user.getUser()).getDestroyedPile();
            case PlaceUsingDiscard ->  state.getCommander(user.getUser()).getDiscardPile();
            case PlaceUsingDisplaced ->  state.getCommander(user.getUser()).getDisplacedPile();
            case PlaceUsingDeck -> state.getCommander(user.getUser()).getDeck();
            case PlaceUsingDecisivePile -> state.getCommander(user.getUser()).getDecisivePile();
            case PlaceUsingTarget -> state.getCards().stream()
                .filter(card -> {
                    if (card.getPosition().getBoardLocation() == BoardLocation.FIELD)
                        return user.getTarget().stream().anyMatch(t -> t.isOnFieldTarget(card));
                    else return user.getTarget().stream().anyMatch(t -> t.isOffFieldTarget(card));
                }).toList();
            case PlaceUsingCardsOnField -> state.field.getCards();
            case PlaceUsingOpponentsCardsOnField -> state.field.getCards().stream()
                .filter(c -> c.getUser() == user.getUser().opponentOf()).toList();
            default -> throw new IllegalArgumentException("How did we get here?");
        };
    }

    private List<CardInstance> getUsableList(TargetType used, LivingObject user, List<ContinuousInstance> activeContinuous) {
        return switch(used) {
            case PlacementCostCanBeMetByUsersFieldCards -> state.field.getCards().stream()
                .filter(c -> c.getUser() == user.getUser()).toList();
            case PlacementCostCanBeMetByUsersCardsInHand -> state.getCommander(user.getUser()).getHand().contents;
            case PlacementCostCanBeMetByUsersDestroyedCards -> state.getCommander(user.getUser()).getDestroyedPile().contents;
            case PlacementCostCanBeMetByUsersDiscardedCards ->  state.getCommander(user.getUser()).getDiscardPile().contents;
            case PlacementCostCanBeMetByUsersDisplacedCards ->  state.getCommander(user.getUser()).getDisplacedPile().contents;
            case PlacementCostCanBeMetByUsersDeck -> state.getCommander(user.getUser()).getDeck().contents;
            case PlacementCostCanBeMetByUsersDecisivePileCards -> state.getCommander(user.getUser()).getDecisivePile().contents;
            case PlacementCostCanBeMetByTargets ->
                state.getCards().stream()
                    .filter(card -> {
                        if (card.getPosition().getBoardLocation() == BoardLocation.FIELD)
                            return user.getTarget().stream().anyMatch(t -> t.isOnFieldTarget(card));
                        else return user.getTarget().stream().anyMatch(t -> t.isOffFieldTarget(card));
                    }).toList();
            case PlacementCostCanBeMetByAnyFieldCards -> state.field.getCards();
            case PlacementCostCanBeMetByOpponentsFieldCards -> state.field.getCards().stream()
                .filter(c -> c.getUser() == user.getUser().opponentOf()).toList();
            default -> throw new IllegalArgumentException("How did we get here?");
        };
    }

    /* --- Effect Information Functions --- */
    /** Returns whether the given EffectInstance deals damage to whatever it targets. */
    private boolean isDamagingToTarget(EffectInstance check) {
        return check.getInstanceEffectChanges().stream().anyMatch(
            e -> e.getTypeInstance().getSimultNum() == 22 // Damage
                    && e.getTargetInstance().contains(TargetType.Target)
        );
    }

    /** Returns the selfPlacementEffect of the given LivingObject, null otherwise. */
    private EffectChangeInstance getSelfPlacementEffect(LivingObject check) {
        for (EffectChangeInstance change : check.getInstancedPlaceCost().getInstanceEffectChanges()) {
            if (change.getType() == EffectChangeType.selfPlacementEffect) {
                return change;
            }
        }
        return null;
    }

    /** Returns the VariableGameNum which denotes a card's written / modified material cost, heeding activeContinuous. */
    private VariableGameNum getMaterialPlaceCost(LivingObject check, List<ContinuousInstance> activeContinuous) {
        return getAllMaterialChange(check, activeContinuous).get(1).getStrengthInstance(); // The first number mentioned is the material cost.
    }

    /** Returns a list of every EffectChangeInstance which denotes a card's written / modified material cost,
     * heeding activeContinuous. */
    private List<EffectChangeInstance> getAllMaterialChange(LivingObject check, List<ContinuousInstance> activeContinuous) {
        List<EffectChangeInstance> materialChanges = new ArrayList<>();
        for (EffectChangeInstance change : check.getInstancedPlaceCost().getInstanceCost()) {
            switch (change.getType()) {
                case PayMaterial, DisplaceMaterial,
                     DiscardMaterial, DestroyMaterial,
                     ReturnMaterialToHand, ShuffleMaterial,
                     SendMaterialToDecisiveDeck, AddRageCountersToMaterial ->
                    materialChanges.add(change);
            }
        }
        return materialChanges;
    }

    private List<EffectChangeInstance> getAllUsedChange(LivingObject check, List<ContinuousInstance> activeContinuous) {
        List<EffectChangeInstance> materialChanges = new ArrayList<>();
        for (EffectChangeInstance change : check.getInstancedPlaceCost().getInstanceCost()) {
            switch (change.getType()) {
                case DisplaceMaterial,
                     DiscardMaterial, DestroyMaterial,
                     ReturnMaterialToHand, ShuffleMaterial,
                     SendMaterialToDecisiveDeck, AddRageCountersToMaterial ->
                    materialChanges.add(change);
            }
        }
        return materialChanges;
    }

    /** Returns the VariableGameNum that is the base material cost of the check, heeding activeContinuous. May be null. */
    private VariableGameNum getMaterialPaymentNum(LivingObject check, List<ContinuousInstance> activeContinuous) {
        final EffectChangeInstance materialEffect = getMaterialPayment(check, activeContinuous);
        if (materialEffect == null) return null;
        return materialEffect.getStrengthInstance();
    }

    /** Returns the EffectChangeInstance that pays material cost for check, heeding activeContinuous. May be null. */
    private EffectChangeInstance getMaterialPayment(LivingObject check, List<ContinuousInstance> activeContinuous) {
        for (EffectChangeInstance change : check.getInstancedPlaceCost().getInstanceCost()) {
            if (change.getType() == EffectChangeType.PayMaterial) {
                return change;
            }
        }
        return null;
    }

    /* --- Continuous Functions --- */
    /** Returns whether the given EffectChange is in the given List ContinuousInstance affecting the object. */
    private boolean isContinuousTypeOnObject(LivingObject check, EffectChangeType type, List<ContinuousInstance> activeContinuous) {
        return getContinuousThatApplyOnObject(check, activeContinuous).stream().anyMatch(e -> e.effect().getType() == type);
    }

    /** Returns whether any given EffectChange is in the given List ContinuousInstance affecting the object. */
    private boolean isAnyContinuousTypeOnObject(LivingObject check, List<EffectChangeType> types, List<ContinuousInstance> activeContinuous) {
        return getContinuousThatApplyOnObject(check, activeContinuous).stream().anyMatch(e -> types.contains(e.effect().getType()));
    }

    /**
     * Returns every ConstantContinuous from the given list of continuous that applies on the given LivingObject,
     * and also every effectChangeInstance with duration from the state that applies on the given LivingObject.
     * Does not check the conditions of the continuous in the list - just checks if the livingObject fits the targetType(s),
     * or is given as a target.
     */
    public List<ContinuousInstance> getContinuousThatApplyOnObject(LivingObject check, List<ContinuousInstance> activeContinuous) {
        return getContinuousThatApplyOnObject(check, activeContinuous, e -> true);
    }

    /**
     * Returns every ConstantContinuous from the given list of continuous that applies on the given LivingObject,
     * and also every effectChangeInstance with duration from the state that applies on the given LivingObject.
     * Does not check the conditions of the continuous in the list - just checks if the livingObject fits the targetType(s),
     * or is given as a target.
     */
    public List<ContinuousInstance> getContinuousThatApplyOnObject(LivingObject check, List<ContinuousInstance> activeContinuous, Predicate<ContinuousInstance> filter) {
        return activeContinuous.stream().filter(o -> (o.target() == null && isTargetedBy(LivingObject.class, check, o.effect(), activeContinuous)) || o.target().contains(check))
            .filter(filter)
            .toList();
    }

    /** Returns ActiveContinuous, that is, a list of every continuous that is used, including optional continuous that affect the current change. */
    private List<ContinuousInstance> getActiveOptionalContinuous(EffectObject effect, List<ContinuousInstance> activeConstantContinuous) {
        if (effect == null) return activeConstantContinuous;
        List<ContinuousInstance> activeContinuous = new ArrayList<>();

        for (EffectInstance chosenContinuous : state.getInstancedEffects().stream()
            .filter(e -> e.getType() == EffectType.OptionalContinuous
                && isEffectLegal(e, activeConstantContinuous)
                && isOptionalContinuousApplicable(e, effect, activeConstantContinuous)).toList()) {

            // If the commander chose to use the continuous effect
            if (getPlayerOption(sideIDtoController(chosenContinuous.getUser().getUser()), new GameChoice(chosenContinuous, List.of()))) {
                List<GameEvent> eventList = new ArrayList<>();
                // Pay the cost of the continuous and add it to active continuous
                chosenContinuous.getInstanceCost().forEach(cost ->
                    resolveGameChange(getChoiceChanges(cost, activeContinuous), eventList));
                resolveLogAdding(eventList);
                activeContinuous.addAll(chosenContinuous.getInstanceEffectChanges().stream()
                    .map(e -> { return new ContinuousInstance(e, findChoicesFromList(LivingObject.class, state.getLivingObjects(), e, activeConstantContinuous), List.of()); })
                    .toList());
            }
        }

        // Then return all the continuous
        activeContinuous.addAll(activeConstantContinuous);
        return activeContinuous;
    }

    /** Returns whether an optional continuous can affect the given EffectObject, and if the continuous targets the effect's user. */
    private boolean isOptionalContinuousApplicable(EffectInstance optionalContinuous, EffectObject changeableEffect, List<ContinuousInstance> activeContinuous) {
        if (optionalContinuous.getEffectFrom().getType() != EffectType.OptionalContinuous) throw new IllegalArgumentException("Attempted to check optional conditions of non-optional effect: " + optionalContinuous);
        for (EffectChangeInstance change : optionalContinuous.getInstanceEffectChanges()) {
            if (!EffectChangeType.optionalContinuousList.contains(change.getType())) continue;

            if (changeableEffect instanceof EffectChangeInstance e) {
                if (!getEffectChangeContinuous(e, activeContinuous).isEmpty()) return true;
            } else if (changeableEffect instanceof EffectInstance e) {
                if (!getEffectContinuous(e, activeContinuous).isEmpty()) return true;
            } else if (changeableEffect instanceof ConditionInstance e) {
                if (!getConditionContinuous(e , activeContinuous).isEmpty()) return true;
            }
        }
        return false;
    }

    /** Returns the current list of active EffectChangeInstance that are Continuous or in the list of the state's continuous with duration, except OptionalContinuous. */
    private List<ContinuousInstance> getActiveContinuous() {
        final List<ContinuousInstance> allContinuous = new ArrayList<>();
        for (LivingObject user : state.getLivingObjects()) { // returns LivingObjects in decreasing priority
            for (EffectInstance effect : user.getInstancedEffects()) {
                if (effect.getType() == EffectType.ConstantContinuous) {
                    if (isEffectLegal(effect, allContinuous)) {
                        allContinuous.addAll(effect.getInstanceEffectChanges().stream()
                            .map(e ->
                                new ContinuousInstance(e, findChoicesFromList(LivingObject.class, state.getLivingObjects(), e, allContinuous), List.of()))
                            .toList());
                    }
                }
            }
        }
        return Stream.of(allContinuous, state.getDurationContinuous())
            .flatMap(List::stream)
            .sorted(Comparator.comparingInt(e -> -e.effect().getUser().getPriority()))
            .toList();
    }

    /* --- Just Happened functions --- */
    /** Returns the number of times the predicate is matched with GameEvent in the state's JustHappened. */
    private boolean justHappened(Predicate<? super GameEvent> condition) {
        return occursInGameEvents(condition, state.getJustHappened());
    }

    /** Returns the number of times the predicate is matched with GameEvent in the state's JustHappened. */
    private int justHappenedNum(Predicate<? super GameEvent> condition) {
        return occursInGameEventsNum(condition, state.getJustHappened());
    }

    /** Returns whether the predicate is met for any GameEvent in the current phase. */
    private boolean justHappenedThisPhase(Predicate<? super GameEvent> condition) {
        return occursInList(currentPhase(room.getLog()).stream().flatMap(List::stream).toList(), condition);
    }

    /** Returns whether the predicate is met for any GameEvent in the current turn. */
    private boolean justHappenedThisTurn(Predicate<? super GameEvent> condition) {
        return occursInList(room.getLog().getLog(state.turn).stream().flatMap(List::stream).toList(), condition);
    }

    /** Returns whether the predicate is met for any GameEvent over the entire game. */
    private boolean justHappenedThisGame(Predicate<? super GameEvent> condition) {
        return occursInList(room.getLog().getLog().stream().flatMap(List::stream).toList(), condition);
    }

    /** Returns whether the predicate is met for any GameEvent over the most recent simultaneous effects. */
    private boolean justHappenedLastSimultaneous(Predicate<? super GameEvent> condition) {
        return occursInList(currentSimultaneous(room.getLog()), condition);
    }

    /** Returns the number of times the predicate is met for the most recent simultaneous effects. */
    private int justHappenedLastSimultaneousNum(Predicate<? super GameEvent> condition) {
        return Math.toIntExact(currentSimultaneous(room.getLog()).stream().filter(condition).count());
    }

    /** Returns whether the predicate is met for any GameEvent in the given List List GameEvent. */
    private boolean occursInGameEvents(Predicate<? super GameEvent> condition, List<List<GameEvent>> check) {
        return occursInList(check.stream().flatMap(List::stream).toList(), condition);
    }

    /** Returns whether the number of times the predicate is met for any GameEvent in the given List List GameEvent. */
    private int occursInGameEventsNum(Predicate<? super GameEvent> condition, List<List<GameEvent>> check) {
        return numOccursInList(check.stream().flatMap(List::stream).toList(), condition);
    }

    /** Returns a list of every living object that was a target of something that JustHappened, filtered by the filter. */
    private List<LivingObject> getAllTargetsJustHappened(Predicate<? super GameEvent> filter) {
        return state.getJustHappened().stream().flatMap(List::stream)
            .filter(filter)
            .map(g -> state.getLivingObject(g.getTarget()))
            .toList();
    }

    /* --- Helper Functions --- */
    /** Returns the Controller associated with the given SideID. */
    private ControllerInstance sideIDtoController(SideID side) {
        if (side == null || side == SideID.NEUTRAL) return null;
        return side == SideID.ONE ? room.getController1() : room.getController2();
    }

    /** Returns true the given predicate of T is met by any T in the given list. */
    private static <T> boolean occursInList(List<T> events, Predicate<? super T> condition) {
        return events.stream().anyMatch(condition);
    }

    /** Returns the number of times a given predicate of T is met by elements of T in the list. */
    private static <T> int numOccursInList(List<T> events, Predicate<? super T> condition) {
        return Math.toIntExact(events.stream().filter(condition).count());
    }

    /** Returns all GameEvent from the given log's most recent simultaneous event. */
    private static List<GameEvent> currentSimultaneous(Log log) {
        return log.getLog().get(log.getLog().size() - 1);
    }

    /** Returns all GameEvent in the given log's most recent phase. */
    private static List<List<GameEvent>> currentPhase(Log log) {
        List<List<GameEvent>> logInfo = log.getLog();
        int i = logInfo.size() - 1;
        while (i > 0) {
            if (EffectChangeType.isBeginning(logInfo.get(i).get(0).getChange().getType())) break;
            i--;
        }
        final List<List<GameEvent>> events = new ArrayList<>();
        while (i < logInfo.size()) {
            events.add(logInfo.get(i));
            i++;
        }
        return events;
    }

    /** Return true if some subset of values sums exactly to target, without any duplicate of values. */
    private static boolean canMakeExactSum(List<Integer> values, int target) {
        if (target <= 0) return false;
        boolean[] reachable = new boolean[target + 1];
        reachable[0] = true;
        for (int v : values) {
            if (v <= 0 || v > target) continue;
            for (int s = target; s >= v; s--) {
                if (reachable[s - v]) reachable[s] = true;
            }
            if (reachable[target]) return true;
        }
        return false;
    }

    /**
     * If the type given doesn't match with the livingObject's actual type (card, zone, commander), returns an empty list.
     * Otherwise, returns a list of obj.
     */
    private static <T extends LivingObject> List<T> ensureMatches(Class<T> type, LivingObject obj) {
        return type.isInstance(obj) ? List.of(type.cast(obj)) : List.of();
    }

    /** Returns every object in the list whose class matches with the given class. */
    private static <T extends LivingObject> List<T> ensureMatches(Class<T> type, List<LivingObject> check) {
        List<T> matchingObjects = new ArrayList<>();
        for (LivingObject l : check) {
            final List<T> element = ensureMatches(type, l);
            if (element.size() == 1) matchingObjects.add(element.get(0));
        }
        return matchingObjects;
    }

    /** Swaps two elements in a list based on index number. */
    private static <T> void swap(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}
