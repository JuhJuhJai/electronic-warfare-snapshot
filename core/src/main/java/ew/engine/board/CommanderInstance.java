package ew.engine.board;

import ew.engine.cards.CardType;
import ew.playerData.Commander;
import ew.engine.cards.CardAttribute;
import ew.engine.cards.effects.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Everything that one commander owns: resources, hand, and off-field piles.
 */
public final class CommanderInstance implements LivingObject {

    final long playerCommanderID;

    public static final int STARTING_HEALTH = 20;
    public static final int STARTING_COLLECTION_BASE = 10;

    final SideID sideID;
    final Position position;
    final int instanceID;

    final String name;
    int health = STARTING_HEALTH;
    final int priority;
    int material = 0;
    int collectionBase = STARTING_COLLECTION_BASE;
    int bonusCollection = 0;
    final List<CounterType> counters = new ArrayList<>();
    List<EffectInstance> instancedEffects = new ArrayList<>();


    List<TargetInstance> target = new ArrayList<>();
    List<Integer> usedByEffectID;
    int Xvariable = 0;
    int Qvariable = 0;
    int declaredNum = 0;
    int declaredName = 0;
    int declaredAttribute = 0;
    int declaredPile = 0;
    int declaredDirection = 0;
    List<Integer> declaredChoice = new ArrayList<>(2);

    // Piles. Index 0 is the top of the pile, which only mostly matters for the draw pile. Cards can gain a rider in every pile.
    final List<CardInstance> hand          = new ArrayList<>(11);
    final List<CardInstance> drawPile      = new ArrayList<CardInstance>(60);
    final List<CardInstance> destroyedPile = new ArrayList<CardInstance>(); // CR4.2.2
    final List<CardInstance> discardPile   = new ArrayList<CardInstance>(); // CR4.2.3
    final List<CardInstance> displacedPile = new ArrayList<CardInstance>(); // CR4.2.5
    final List<CardInstance> decisivePile  = new ArrayList<CardInstance>(2); // CR4.2.4 (0-2 decisive cards)

    boolean passed = false; // unable to place any more cards by opportunity to play once passing.
    boolean negated = false;
    boolean opponentVis = true;
    boolean revealed = false;
    boolean isDestroyed = false; // et to true when health is zero.

    public CommanderInstance(Commander player, AtomicInteger livingID, AtomicInteger nextEffectIDs) {
        this.name = player.getName();
        this.playerCommanderID = player.getPlayerID();
        this.instanceID = livingID.getAndIncrement();
        this.sideID = instanceID == 1 ? SideID.ONE : SideID.TWO;
        this.position = sideID == SideID.ONE ? Position.COMMANDER_ONE() : Position.COMMANDER_TWO();
        this.priority = instanceID;
        for (Effect effect : effects) this.instancedEffects.add(new EffectInstance(effect, this, nextEffectIDs));
    }

    @Override
    public String toString() {
        return "Commander " + name + " side " + sideID + " with " + health + " health.";
    }

    // Low level state getters.
    public SideID getSide()         { return sideID; }
    public long getPlayerCommanderID() { return playerCommanderID; }
    public int getMaterial()        { return material; }
    public LivingObject getSelf()   { return this; }
    public String getOriginalName()         { return name; }
    public String getNameInstance()         { return name; }
    public int getHealthInstance()  { return health; }
    public int getHealth()  { return STARTING_HEALTH; }
    public CardAttribute getOriginalAttribute() { return CardAttribute.None; }
    public CardAttribute getInstanceAttribute() { return CardAttribute.None; }
    public CardType getCardType()               { return CardType.COMMANDER; }
    public CardType getCardTypeInstance()       { return CardType.COMMANDER; }
    public List<CounterType> getCounters()      { return counters; }
    public boolean hasCounter(CounterType counter) { return counters.contains(counter); }
    /** True if any counter in the list matches with a counterType of the obj */
    public boolean hasCounter(List<CounterType> counters) { return !counters.stream().filter(this.counters::contains).toList().isEmpty(); }
    /** Returns the decreasing number of BASE collection. */
    public int getCollectionBase()  { return collectionBase; }
    /** Does NOT check cards, only returns the set number. */
    public int getBonusCollection() { return bonusCollection; }
    public boolean isPassed()         { return passed; }
    public List<Effect> getEffects()     { return effects; }
    public List<EffectInstance> getInstancedEffects() { return instancedEffects; }
    public boolean isNegated()          { return negated; }
    public int getX()                   { return Xvariable; }
    public int getQ()                   { return Qvariable; }
    public int getDeclaredNum()         { return declaredNum; }
    public int getDeclaredName()        { return declaredName; }
    public int getDeclaredAttribute()   { return declaredAttribute; }
    public int getDeclaredPile()        { return declaredPile; }
    public int getDeclaredDirection()   { return declaredDirection; }
    public List<Integer> getDeclaredChoices()      { return declaredChoice; }
    public SideID getOwner()            { return sideID; }
    public SideID getController()       { return sideID; }
    public SideID getUser()             { return sideID; }
    public int getOriginalID()          { return instanceID * -1; }
    public int getInstanceID()          { return instanceID; }
    public int getPriority()            { return priority; }
    public List<TargetInstance> getTarget() { return target; }
    public Position getPosition()           { return position; }
    // for a commander, being visible to the other commander means they can see pile contents.
    public boolean getCommanderOneVis()    { return sideID == SideID.ONE || opponentVis; }
    public boolean getCommanderTwoVis()    { return sideID == SideID.TWO || opponentVis; }
    public boolean isFaceUp()           { return true; }
    public boolean isRevealed()         { return revealed; }
    public boolean isShrouded()         { return false; }
    public boolean wasShroudedThisTurn() { return false; }
    public Position getUndisplacementLocation() { return null; }
    public EffectDurationInstance getUndisplacementTime() { return null; }
    public boolean isRode()             { return false; }
    public boolean isRider()            { return false; }
    public boolean isDestroyed()        { return isDestroyed; } // Winning the game checks isDestroyed.
    public boolean isExcavated()        { return false; }
    public boolean isSearched()         { return false; }
    public boolean isToken()            { return false; }
    public boolean isDecisive()         { return false; }
    public boolean isTokenInstance()    { return false; }
    public boolean isDecisiveInstance() { return false; }
    public boolean isOccupying()        { return false; }
    public Effect getPlaceCost() { return null; }
    public EffectInstance getInstancedPlaceCost() { return null; }
    public List<Integer> getUsedByEffectID() { return usedByEffectID; }

    public List<CardInstance> getHand()          { return hand; }
    public List<CardInstance> getDeck()          { return drawPile; }
    public List<CardInstance> getDestroyedPile() { return destroyedPile; }
    public List<CardInstance> getDiscardPile()   { return discardPile; }
    public List<CardInstance> getDisplacedPile() { return displacedPile; }
    public List<CardInstance> getDecisivePile()  { return decisivePile; }

    /** Doesn't return cards a commander controls on the field. Use field.getCards() for that. */
    public List<CardInstance> getCards() {
        return Stream.of(hand, drawPile, destroyedPile, discardPile, displacedPile, decisivePile)
            .flatMap(List::stream)
            .toList();
    }

    public List<CardInstance> getCardsFromTopOfDeck(int numOfCards) {
        if (numOfCards < 0) throw new IllegalArgumentException("Must get 0 or more cards from the top of the deck.");
        return drawPile.subList(0, numOfCards - 1);
    }

    // Low level state setters. Exceptions may be thrown for LivingObject cases that do not apply to Commanders.
    public void setHealthInstance(int v)         { this.health = v; }
    public void setOpportunities(List<EffectInstance> newOpportunities) { this.instancedEffects = newOpportunities; }
    public void setMaterial(int v)          { this.material = v; }
    public void setCollectionBase(int v)    { this.collectionBase = v; }
    public void setBonusCollection(int v)   { this.bonusCollection = v; }
    public void setPassed(boolean v)        { this.passed = v; }
    public void addCounter(CounterType counter, int amount) {
        if (amount < 0) return;
        while (amount > 0) {
            this.counters.add(counter);
            amount--;
        }
    }
    public void removeCounter(CounterType counter, int amount) {
        for (int i = 0; i < counters.size(); i++) {
            if (counters.get(i) == counter) {
                counters.remove(i);
                amount--;
                if (amount == 0) break;
                i--;
            }
        }
    }
    public void setTypeInstance(CardType newType) { throw new ClassCastException("Attempted to set " + this + " CardType."); }
    public void setName(String newName) { throw new ClassCastException("Attempted to set " + this + " name."); }
    public void setAttributeInstance(CardAttribute attribute) { throw new ClassCastException("Attempted to set " + this + " attribute."); }
    public void setPriority(int priority) { throw new ClassCastException("Attempted to set " + this + " priority."); }
    public void setPosition(Position newPosition) { throw new ClassCastException("Attempted to set " + this + " position."); }
    // changing *commander* control is actually so fucking funny, but I won't allow it for now.
    public void setController(SideID newController) { throw new ClassCastException("Attempted to change " + this + " control"); }
    public void setUser(SideID newUser)     { throw new ClassCastException("Attempted to set " + this + " user."); }
    public void setX(int x)                 { Xvariable = x; }
    public void setQ(int q)                 { Qvariable = q; }
    public void setDeclaredNum(int d)       { declaredNum = d; }
    public void setDeclaredName(int d)      { declaredName = d; }
    public void setDeclaredAttribute(int d) { declaredAttribute = d; }
    public void setDeclaredPile(int d)      { declaredPile = d; }
    public void setDeclaredDirection(int d) { declaredDirection = d; }
    public void setDeclaredChoices(List<Integer> choice) { declaredChoice = choice; }
    public void addEffect(EffectInstance effect) { this.instancedEffects.add(effect); }
    public void removeEffect(int effectID) { for (int i = 0; i < instancedEffects.size(); i++) if (instancedEffects.get(i).getEffectID() == effectID) { instancedEffects.remove(i); return; }
        throw new IllegalArgumentException(sideID + " effect ID " + effectID + " not found to remove."); }
    public void resetEffects(AtomicInteger nextEffectIDs) {
        instancedEffects = new ArrayList<EffectInstance>();
        for (Effect effect : effects) instancedEffects.add(new EffectInstance(effect, this, nextEffectIDs));
    }
    //  Changing the visibility a commander has of themselves is also pretty interesting, but isn't allowed for now.
    public void setCommanderOneVis(boolean vis) { if (sideID == SideID.ONE) throw new ClassCastException("Attempted to change " + this + " visibility of themselves."); else opponentVis = vis; }
    public void setCommanderTwoVis(boolean vis) { if (sideID == SideID.ONE) opponentVis = vis; else throw new ClassCastException("Attempted to change " + this + " visibility of themselves."); }
    public void setFaceUp(boolean faceUp)       { throw new ClassCastException("Attempted to set " + this + " faceUp / faceDown status."); }
    public void setRevealed(boolean revealed)   { this.revealed = revealed; }
    public void setShrouded(boolean shrouded)   { throw new ClassCastException("Attempted to shroud " + this + "."); }
    public void setShroudedThisTurn(boolean shroudedThisTurn) { throw new ClassCastException("Attempted to set whether " + this + " was shrouded this turn."); }
    public void setUndisplacementPosition(Position pos) { throw new ClassCastException("Attempted to set " + this + " undisplacement position."); }
    public void setUndisplacementTime(EffectDurationInstance e) { throw new ClassCastException("Attempted to set " + this + " undisplacement time."); }
    // No cards currently negate the commander (rendering them unable to use their opportunity to play effects, except skipping, as that's not an effect).
    public void setNegated(boolean negated)         { this.negated = negated; }
    public void setZoneOccupation(boolean occupies) { throw new ClassCastException("Attempted to change " + this + " zone occupation."); }
    public void setExcavated(boolean excavated)     { throw new ClassCastException("Attempted to change " + this + " excavation state."); }
    public void setSearched(boolean searched)       { throw new ClassCastException("Attempted to change " + this + " searched occupation"); }
    public void setRideState(boolean riding, boolean ridden) { throw new ClassCastException("Attempted to set riding state of " + this + "."); }
    public void setDestroyedState(boolean destroyed) { this.isDestroyed = destroyed; }
    public void addTarget(TargetInstance target) { this.target.add(target); }
    public void addTarget(List<TargetInstance> target) { this.target.addAll(target); }
    public void clearTargets() { this.target = new ArrayList<>(); }
    public void setDecisiveState(boolean isDecisive) { throw new ClassCastException("Attempted to change decisive state of " + this); }
    public void setTokenState(boolean isToken) { throw new ClassCastException("Attempted to change token state of " + this); }
    public void setUsedByEffectID(List<Integer> usingEffectID) { this.usedByEffectID = usingEffectID; }

    public boolean equals(Object o) {
        if (!(o instanceof CommanderInstance other)) return false;
        return other.getInstanceID() == instanceID;
    }
    public int hashCode() {
        return Objects.hash(sideID, name);
    }

    /**
     * Adds a card to a specified position. Removes the original card from its position.
     * If you want to make a commander add one of their cards to the opponent, then make them add it themselves.
     * Does not change face-up face-down status of the card.
     */
    public boolean moveCard(CardInstance card, Position newPosition) {
        boolean wasRemoved = true;
        switch (newPosition.getBoardLocation()) {
            case DECK_ONE, DECK_TWO -> { // It's the match's job to shuffle after adding a card to the deck.
                try { removeCard(card.getInstanceID(), card.getPosition().getBoardLocation()); }
                catch (IllegalStateException e) {
                    wasRemoved = false;
                }
                drawPile.add(card);
                card.setPosition(newPosition);
            }

            case HAND_ONE, HAND_TWO -> {
                try { removeCard(card.getInstanceID(), card.getPosition().getBoardLocation()); }
                catch (IllegalStateException e) {
                    wasRemoved = false;
                }
                hand.add(card);
                card.setPosition(newPosition);
            }

            case DESTROYED_PILE_ONE, DESTROYED_PILE_TWO -> {
                try { removeCard(card.getInstanceID(), card.getPosition().getBoardLocation()); }
                catch (IllegalStateException e) {
                    wasRemoved = false;
                }
                destroyedPile.add(card);
                card.setPosition(newPosition);
            }

            case DISCARD_PILE_ONE , DISCARD_PILE_TWO -> {
                try { removeCard(card.getInstanceID(), card.getPosition().getBoardLocation()); }
                catch (IllegalStateException e) {
                    wasRemoved = false;
                }
                discardPile.add(card);
                card.setPosition(newPosition);
            }

            case DISPLACED_PILE_ONE, DISPLACED_PILE_TWO -> {
                try { removeCard(card.getInstanceID(), card.getPosition().getBoardLocation()); }
                catch (IllegalStateException e) {
                    wasRemoved = false;
                }
                displacedPile.add(card);
                card.setPosition(newPosition);
            }

            case DECISIVE_PILE_ONE, DECISIVE_PILE_TWO -> {
                try { removeCard(card.getInstanceID(), card.getPosition().getBoardLocation()); }
                catch (IllegalStateException e) {
                    wasRemoved = false;
                }
                decisivePile.add(card);
                card.setPosition(newPosition);
            }

            case FIELD -> {
                throw new IllegalArgumentException("Invalid place for a commander to add a card. (A commander doesn't add to the field)");
            }

            default -> throw new IllegalArgumentException(this + " cannot add a card to " + newPosition);
        }
        return wasRemoved;
    }

    /** Removes a card from a commander's lists if it's in the location. */
    public void removeCard(int cardID, BoardLocation location) throws IllegalStateException {
        switch(location) {
            case DECK_ONE, DECK_TWO -> { for (int i = 0; i < drawPile.size(); i++) if (drawPile.get(i).getInstanceID() == cardID) { drawPile.remove(i); return; }
                throw new IllegalStateException(cardID + " not found at " + location + " for " + this); }
            case HAND_ONE, HAND_TWO -> { for (int i = 0; i < hand.size(); i++) if (hand.get(i).getInstanceID() == cardID) { hand.remove(i); return; }
                throw new IllegalStateException(cardID + " not found at " + location + " for " + this); }
            case DESTROYED_PILE_ONE, DESTROYED_PILE_TWO -> { for (int i = 0; i < destroyedPile.size(); i++) if (destroyedPile.get(i).getInstanceID() == cardID) { destroyedPile.remove(i); return; }
                throw new IllegalStateException(cardID + " not found at " + location + " for " + this); }
            case DISCARD_PILE_ONE , DISCARD_PILE_TWO -> { for (int i = 0; i < discardPile.size(); i++) if (discardPile.get(i).getInstanceID() == cardID) { discardPile.remove(i); return; }
                throw new IllegalStateException(cardID + " not found at " + location + " for " + this); }
            case DISPLACED_PILE_ONE, DISPLACED_PILE_TWO -> { for (int i = 0; i < displacedPile.size(); i++) if (displacedPile.get(i).getInstanceID() == cardID) { displacedPile.remove(i); return; }
                throw new IllegalStateException(cardID + " not found at " + location + " for " + this); }
            case DECISIVE_PILE_ONE, DECISIVE_PILE_TWO -> { for (int i = 0; i < decisivePile.size(); i++) if (decisivePile.get(i).getInstanceID() == cardID) { decisivePile.remove(i); return; }
                throw new IllegalStateException(cardID + " not found at " + location + " for " + this); }
        }
    }

    /**
     * Changes the position of a card in the deck based on its ID.
     * A card's Position doesn't hold an index of where the card is in a pile, so
     * this is the only check for deck position.
     */
     public void changeCardDeckPosition(int cardID, int newIndex) {
        for (int i = 0; i < drawPile.size(); i++) {
            if (drawPile.get(i).getInstanceID() == cardID) {
                CardInstance temp = drawPile.get(i);
                drawPile.remove(i);
                drawPile.add(newIndex, temp);
                break;
            }
        }
     }

     /** Shuffles this commander's deck using the Fisher-Yates algorithm. */
     public void shuffleDeck(long shuffleRandomness) {
         Random random = new Random(shuffleRandomness);
         for (int i = drawPile.size() - 1; i > 0; i--) {
             int j = random.nextInt(i); // random index from 0 to i
             CardInstance temp = drawPile.get(i);
             drawPile.remove(i);
             drawPile.add(i, drawPile.get(j));
             drawPile.remove(j);
             drawPile.add(j, temp);
         }
     }

    /**
     * Precision = 5 minus the largest difference among the terminator piles, min 0
     * (CR13.12.1). The terminator piles are destroyed, discarded, and displaced.
     */
    public int getPrecision() {
        int d = destroyedPile.size();
        int x = discardPile.size();
        int p = displacedPile.size();
        int max = Math.max(d, Math.max(x, p));
        int min = Math.min(d, Math.min(x, p));
        return Math.max(0, 5 - (max - min));
    }

    /**
     * A commander's Opportunity to Play is an effect they have.
     * The base uses for an opportunity to play, which they automatically get, are:
     *  a) place 1 card from their hand by paying its placement cost
     *  b) place 1 face down decisive card or face up card in their decisive deck by paying its placement cost
     *  c) activate one Activatable effect whose activation condition is met and whose cost can be paid (not included, is an effect of the card)
     *  d) unshroud a shrouded card they own, if it hasn’t been shrouded this turn
     *  e) destroy a production card they control on a production zone
     *  f) Make a card they control ride a rideable card they control.
     */
    public final List<Effect> effects = new ArrayList<Effect>(Arrays.asList(
        Effect.ACTIVATABLE(
            "Place a card from your hand.",
            new ArrayList<Condition>(List.of(
                Condition.of(
                    ConditionType.MinThings,
                    List.of(TargetType.CardsInHand, TargetType.CardsYouUse, TargetType.PlacementCostIsMet),
                    1)
            )),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(
                    EffectChangeType.Place,
                    List.of(TargetType.CardsInHand, TargetType.CardsYouUse, TargetType.PlacementCostIsMet),
                    1)
            ))
        ),
        Effect.ACTIVATABLE(
            "Place a face down decisive card from your decisive deck.",
            new ArrayList<Condition>(List.of(
                Condition.of(
                    ConditionType.MinThings,
                    List.of(TargetType.CardsInDecisiveDeck, TargetType.CardsYouOwn, TargetType.DecisiveCards, TargetType.FaceDownCards, TargetType.PlacementCostIsMet),
                    1)
            )),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(
                    EffectChangeType.Place,
                    List.of(TargetType.CardsInDecisiveDeck, TargetType.CardsYouOwn, TargetType.DecisiveCards, TargetType.FaceDownCards, TargetType.PlacementCostIsMet),
                    1)
            ))
        ),
        Effect.ACTIVATABLE(
            "Place a face up card from your decisive deck.",
            new ArrayList<Condition>(List.of(
                Condition.of(
                    ConditionType.MinThings,
                    List.of(TargetType.CardsInDecisiveDeck, TargetType.CardsYouOwn, TargetType.FaceUpCards, TargetType.PlacementCostIsMet),
                    1)
            )),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(
                    EffectChangeType.Place,
                    List.of(TargetType.CardsInDecisiveDeck, TargetType.CardsYouOwn, TargetType.FaceUpCards, TargetType.PlacementCostIsMet),
                    1)
            ))
        ),
        Effect.ACTIVATABLE(
            "Unshroud a card you control that wasn't shrouded this turn.",
            new ArrayList<Condition>(List.of(
                Condition.of(
                    ConditionType.MinThings,
                    List.of(TargetType.CardsYouControl, TargetType.ShroudedCards, TargetType.CardsNotShroudedThisTurn),
                    1)
            )),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(
                    EffectChangeType.Unshroud,
                    List.of(TargetType.CardsYouControl, TargetType.ShroudedCards, TargetType.CardsNotShroudedThisTurn),
                    1)
            ))
        ),
        Effect.ACTIVATABLE(
            "Destroy a production card you control on a production zone.",
            new ArrayList<Condition>(List.of(
                Condition.of(
                    ConditionType.MinThings,
                    List.of(TargetType.CardsYouControl, TargetType.ProductionZones, TargetType.ProductionCards),
                    1)
            )),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(
                    EffectChangeType.Destroy,
                    List.of(TargetType.CardsYouControl, TargetType.ProductionZones, TargetType.ProductionCards),
                    1)
            ))
        ),
        Effect.ACTIVATABLE(
            "Make a card you control ride a rideable card adjacent to it, paying its rideable cost.",
            new ArrayList<Condition>(List.of(
                Condition.of(
                    ConditionType.MinThings,
                    List.of(TargetType.CardsYouControl, TargetType.CardsThatCanRideAnAdjacentCard),
                    1)
            )),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(
                    EffectChangeType.RideAdjacent,
                    List.of(TargetType.CardsYouControl, TargetType.CardsThatCanRideAnAdjacentCard),
                    1)
            ))
        )
    ));
}
