package ew.engine.board;

import ew.engine.resolver.GameChoice;
import ew.engine.resolver.MatchFormat;
import ew.playerData.CardStored;
import ew.playerData.Commander;
import ew.playerData.Deck;
import ew.server.GameResult;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The complete, authoritative state of one match: the shared board, both commanders, and the
 * global turn bookkeeping. This object holds GROUND TRUTH.
 * There are NO legality checks in this class, or any object this class uses, just getters and setters.
 * From any gamestate, the resolver should be able to determine the next action that needs to be taken.
 */
public final class GameState {

    public final AtomicInteger nextLivingID = new AtomicInteger(1); // gamestate is 0, commander 1 is 1, commander 2 is 2
    public final AtomicInteger nextEffectID = new AtomicInteger(1);
    public final AtomicInteger nextPriority = new AtomicInteger(1);
    public final MatchFormat format;
    public boolean gameGoing = false;
    List<GameResult> gameResult;

    public final Field field;
    public final CommanderInstance playerOne;
    public final CommanderInstance playerTwo;

    public int turn = 0;
    public Phase phase = Phase.SETUP;
    public TimingPoint timingPoint = TimingPoint.D0;
    public final List<Phase> originalPhaseOrder = List.of(Phase.DRAW, Phase.BUILD, Phase.COMBAT, Phase.AFTERMATH);
    List<Phase> phaseOrderInstance = new ArrayList<>(originalPhaseOrder); // phases can be added, so this is instanced

    // There's a world where these are split.
    /** Base collection for commander 1 */
    public int baseCollection1 = 10;
    /** Base collection for commander 2. */
    public int baseCollection2 = 10;

    /** Who currently holds the Opportunity to Play during Build (CR8). */
    public SideID opportunityHolder = SideID.NEUTRAL;
    /** Who receives the FIRST Opportunity to Play this turn; alternates each turn (CR6.2.1). */
    public SideID firstOpportunity = SideID.ONE;
    /** The current commander with a choice to do something in a Trigger Chain. Changes with each selection. */
    public SideID triggerOpportunity; // Resets to the player who has the first opportunity
    public SideID firstTriggerOpportunity = SideID.ONE; // There's a world where this is different from firstOpportunity.
    /** Who passed first in the final consecutive passes; picks the first lane in Combat (CR10.1.1). */
    public SideID firstToPass = SideID.NEUTRAL;
    public SideID winner = SideID.NEUTRAL;
    /** What's JUST HAPPENED in the gamestate, as a list of effect changes. Applies for Triggers.
     * Simultaneous Effects are put into the same list. */
    List<List<GameEvent>> justHappened;
    /** Stores effectChange that last for an amount of time */
    List<EffectChangeInstance> effectsWithDuration = new ArrayList<>();
    /** Stores effects that have been used for OncePerTurn and Exclusive effects. */
    List<EffectInstance> effectsUsedThisTurn = new ArrayList<>();

    // For Combat substate (selected/resolved lanes, action lists). Null while it's not the combat phase.
    public Integer selectedLane;
    List<Integer> resolvedLanes;
    List<GameChoice> playerOneActionList;
    List<GameChoice> playerTwoActionList;

    /** Most effects can't be used between the cost and effect of another effect, except triggers that unshroud and Continuous effects.*/
    public boolean isBetweenEffects = false;
    public boolean isInTriggerChain = false;

    /** Also initializes commander decks. */
    public GameState(MatchFormat format, Commander goingFirst, Deck goingFirstDeck, Commander goingSecond, Deck goingSecondDeck) {
        this.format = format;
        this.playerOne = new CommanderInstance(goingFirst, nextLivingID, nextEffectID);
        for (Map.Entry<CardStored, Integer> entry : goingFirstDeck.getCards().entrySet()) {
            for (int i = entry.getValue(); i > 0; i--) {
                playerOne.getDeck().contents.add(new CardInstance(playerOne, entry.getKey(), nextLivingID, nextEffectID));
            }
        }

        this.playerTwo = new CommanderInstance(goingSecond, nextLivingID, nextEffectID);
        for (Map.Entry<CardStored, Integer> entry : goingSecondDeck.getCards().entrySet()) {
            for (int i = entry.getValue(); i > 0; i--) {
                playerTwo.getDeck().contents.add(new CardInstance(playerTwo, entry.getKey(), nextLivingID, nextEffectID));
            }
        }
        field = new Field(nextLivingID);
    }

    public CommanderInstance getCommander(SideID id) { return id == SideID.ONE ? playerOne : playerTwo; }
    public List<Integer> getResolvedLanes() { return resolvedLanes; }
    public List<GameChoice> getPlayerOneActionList() { return playerOneActionList; }
    public List<GameChoice> getPlayerTwoActionList() { return playerTwoActionList; }

    public List<Phase> getPhaseOrderInstance()   { return phaseOrderInstance; }
    public List<List<GameEvent>> getJustHappened() { return justHappened; }
    public List<EffectChangeInstance> getEffectsWithDuration() { return effectsWithDuration; }
    public List<EffectInstance> getEffectsUsedThisTurn() { return effectsUsedThisTurn; }

    public AtomicInteger getNextLivingID() { return nextLivingID; }
    public AtomicInteger getNextEffectID() { return nextEffectID; }
    public AtomicInteger getPriority() { return nextPriority; }
    public List<GameResult> getGameResult() { return gameResult; }

    public void setWinner(SideID winner)        { this.winner = winner; }
    public void setSelectedLane(int laneSelection) { this.selectedLane = laneSelection; }
    public void setResolvedLanes(List<Integer> lanes) { this.resolvedLanes = lanes; }
    public void addResolvedLane(int lane) { this.resolvedLanes.add(lane); }
    public void setPlayerOneActionList(List<GameChoice> actionList) { this.playerOneActionList = actionList; }
    public void setPlayerTwoActionList(List<GameChoice> actionList) { this.playerTwoActionList = actionList; }
    /** Removes the first action from both player's action lists. */
    public void removeOneAction() { this.playerOneActionList.remove(0); this.playerTwoActionList.remove(0); }

    public void setOriginalPhaseOrder(ArrayList<Phase> phaseOrderInstance) { this.phaseOrderInstance = phaseOrderInstance; }
    public void addJustHappened(List<GameEvent> simultaneousEffects) { justHappened.add(simultaneousEffects); }
    public void addJustHappened(GameEvent effect) { justHappened.add(List.of(effect)); }
    public void clearJustHappened() { this.justHappened = new ArrayList<>(); }
    public void setEffectsWithDuration(List<EffectChangeInstance> newEffects) { this.effectsWithDuration = newEffects; }
    public void addEffectWithDuration(EffectChangeInstance effectWithDuration) { this.effectsWithDuration.add(effectWithDuration); }
    public void setEffectsUsedThisTurn(List<EffectInstance> newEffects) { this.effectsUsedThisTurn = newEffects; }
    /** Sets selectedLane, resolvedLanes, playerOneActionList, and playerTwoActionList to null. */
    public void resetCombatSubstate() { selectedLane = null; resolvedLanes = null; playerOneActionList = null; playerTwoActionList = null; }
    public void setGameResult(List<GameResult> result) { this.gameResult = result; }

    public List<CardInstance> getCardsFromTopOfDeck(SideID deckSide, int numOfCards) {
        return getCommander(deckSide).getCardsFromTopOfDeck(numOfCards);
    }

    /**
     * Removes a card from its old position,
     * adds the card to the given position,
     * and updates the card's position.
     * Throws an IllegalStateException if the card wasn't removed from its original position.
     */
    public void moveCard(CardInstance card, Position newPosition) {
        if (card.getPosition().getBoardLocation() == BoardLocation.FIELD) {
            if (newPosition.getBoardLocation() == BoardLocation.FIELD) {
                if (!field.moveCard(card, newPosition)) {
                    throw new IllegalStateException("Card not removed from previous field position.");
                }
            }
            else {
                field.removeCard(card.getInstanceID());
                getCommander(card.getUser()).moveCard(card, newPosition);
            }
        }
        else {
            if (newPosition.getBoardLocation() == BoardLocation.FIELD) {
                getCommander(card.getUser()).removeCard(card, card.getPosition().getBoardLocation());
                field.moveCard(card, newPosition);
            }
            else if (!getCommander(card.getUser()).moveCard(card, newPosition)) {
                throw new IllegalStateException("Card not removed from original pile / hand position.");
            }
        }
    }

    /** Removes the given card from the game. It just removes the card. */
    public void removeFromGame(CardInstance card) {
        if (card.getPosition().getBoardLocation() == BoardLocation.FIELD) {
            field.removeCard(card.getInstanceID());
        }
        else {
            getCommander(card.getUser()).removeCard(card, card.getPosition().getBoardLocation());
        }
    }

    /** Returns null if the effectID isn't found. */
    public EffectChangeInstance getChange(int effectID) {
        try {
            return getEffectChanges().stream()
                .filter(c -> c.getEffectID() == effectID)
                .findAny().orElseThrow();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
    public List<EffectChangeInstance> getEffectChanges() {
        return getLivingObjects().stream()
            .flatMap(obj -> obj.getInstancedEffects().stream().map(EffectInstance::getInstanceEffectChanges))
            .flatMap(List::stream)
            .toList();
    }
    public EffectInstance getInstancedEffect(int effectID) {
        for (EffectInstance effect : getInstancedEffects()) {
            if (effect.getEffectID() == effectID) return effect;
        }
        throw new IllegalArgumentException("No effect has ID: " + effectID);
    }
    public List<EffectInstance> getInstancedEffects() {
        return getLivingObjects().stream()
            .flatMap(obj -> obj.getInstancedEffects().stream())
            .toList();
    }
    public List<CardInstance> getCards() {
        return Stream.of(field.getCards(), playerOne.getCards(), playerTwo.getCards())
            .flatMap(List::stream)
            .sorted(Comparator.comparingInt(LivingObject::getPriority).reversed()) // goes through decreasing priority
            .toList();
    }
    public List<CommanderInstance> getCommanders() {
        return new ArrayList<>(List.of(playerTwo, playerOne)); // priority backwards
    }
    public List<ZoneInstance> getZones() {
        return field.getZones().stream()
            .sorted(Comparator.comparingInt(LivingObject::getPriority).reversed())
            .toList();
    }
    /** Returns LivingObjects in decreasing Priority */
    public List<LivingObject> getLivingObjects() {
        return Stream.of(getCards(), getCommanders(), getZones())
            .flatMap(List::stream)
            .map(LivingObject::getSelf)
            .sorted(Comparator.comparingInt(LivingObject::getPriority).reversed())
            .toList();
    }

    public List<LivingObject> getLivingObjects(Predicate<LivingObject> filter) {
        return getLivingObjects().stream().filter(filter).toList();
    }

    /** May return null. */
    public LivingObject getLivingObject(int instanceID) {
        if (instanceID == 0) return null;
        for (LivingObject check : getLivingObjects()) if (check.getInstanceID() == instanceID) return check;
        return null;
    }
    /** Returns a List of LivingObject whose originalID is the same. Use for "Exclusive" which checks across all cards with the same type. */
    public List<LivingObject> getOriginalLiving(int originalID) {
        if (originalID == 0) return null;
        List<LivingObject> originals = new ArrayList<>();
        for (LivingObject check : getLivingObjects()) if (check.getOriginalID() == originalID) originals.add(check);
        if (originals.isEmpty()) throw new IllegalArgumentException("No LivingObject exist with original ID " + originalID);
        return originals;
    }
    public CardInstance getCard(int cardID) {
        for (CardInstance card : getCards()) if (card.getInstanceID() == cardID) return card;
        throw new IllegalArgumentException("CardID not found: " + cardID);
    }

    public GameStateView getSpectatorView() { return new GameStateView(this, SideID.NEUTRAL); }
    public GameStateView getPlayerOneView() { return new GameStateView(this, playerOne.getSide()); }
    public GameStateView getPlayerTwoView() { return new GameStateView(this, playerTwo.getSide()); }

    /**
     * Throws an exception if any of the following are true:
     * 1. More than 2 of the same livingObject or effect exist (have the same instanceID)
     * 2. More than 2 LivingObject have the same priority
     * 3. More than 2 cards are in a zone or 2 cards are in a zone and aren't a rider / rode pair
     * 4. A card's attribute is "None"
     * 5. A card in a pile / the hand has a controller other than SideID.Neutral
     * 6. A card has the incorrect BoardPosition for where it is (e.g. a card's Position variable having HAND_ONE while its on the field.)
     */
    public void checkLegalGamestate() {
        // 1 & 2. the key is the instanceID, the value is the Priority
        {
            HashMap<Integer, Integer> checkedObjects = new HashMap<>();
            for (LivingObject check : getLivingObjects()) {
                if (checkedObjects.containsKey(check.getInstanceID()))
                    throw new IllegalStateException("InstanceID of " + check.getNameInstance() + ", " + check.getInstanceID() + ", conflicts with another LivingObject.");
                if (checkedObjects.containsValue(check.getPriority()))
                    throw new IllegalStateException("Priority of " + check.getNameInstance() + ", " + check.getPriority() + ", conflicts with another LivingObject.");
                checkedObjects.put(check.getInstanceID(), check.getPriority());
            }
        }

        {
            HashMap<Integer, Boolean> checkedEffects = new HashMap<>();
            for (EffectInstance check : getInstancedEffects()) {
                if (checkedEffects.containsKey(check.getEffectID()))
                    throw new IllegalStateException("InstanceID of " + check + " conflicts with another effect.");
                checkedEffects.put(check.getEffectID(), true);
            }
        }

        // 3.
        for (ZoneInstance zone : getZones()) {
            ZoneInstance checkZone = new ZoneInstance(zone.getPosition(), zone.getType(), zone.getSide(), zone.getInstanceID());
            zone.getCards().forEach(checkZone::addCard);
            checkZone.getCards().removeIf(cardInstance -> !cardInstance.occupiesZone);
            if (checkZone.getCards().size() > 2) throw new IllegalStateException("Zone " + checkZone + " has more than two cards.");
            if (checkZone.getCards().size() == 2) {
                if (checkZone.getCards().get(0).isRider()) if (!checkZone.getCards().get(1).isRode()) throw new IllegalStateException("Zone " + checkZone + " hsa a rider without a rode card.");
                else if (checkZone.getCards().get(0).isRode()) if (!checkZone.getCards().get(1).isRider()) throw new IllegalStateException("Zone " + checkZone + " has a rode card without a rider.");
                else throw new IllegalStateException("Zone " + checkZone + " has 2 cards without a rider or rode card.");
            }
        }
    }
}
