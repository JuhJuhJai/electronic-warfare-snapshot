package ew.engine.board;

import ew.engine.cards.CardAttribute;
import ew.engine.cards.CardType;
import ew.engine.cards.effects.*;
import ew.playerData.Commander;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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
    CardAttribute declaredAttribute = null;
    Pile declaredPile = null;
    Direction declaredDirection = null;
    List<Integer> declaredChoice = new ArrayList<>(2);

    // Piles. The top of the pile is the end of the pile.
    final Pile hand;
    final Pile drawPile;
    final Pile destroyedPile;
    final Pile discardPile;
    final Pile displacedPile;
    final Pile decisivePile;

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

        hand          = new Pile(PileType.Hand, sideID, new ArrayList<>(10));
        drawPile      = new Pile(PileType.Deck, sideID, new ArrayList<>(60));
        destroyedPile = new Pile(PileType.DestroyedPile, sideID, new ArrayList<>());
        discardPile   = new Pile(PileType.DiscardPile, sideID, new ArrayList<>());
        displacedPile = new Pile(PileType.DisplacedPile, sideID, new ArrayList<>());
        decisivePile  = new Pile(PileType.DecisiveDeck, sideID, new ArrayList<>(2)); // (0-2 decisive cards)
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
    public CardAttribute getDeclaredAttribute()   { return declaredAttribute; }
    public Pile getDeclaredPile()        { return declaredPile; }
    public Direction getDeclaredDirection()   { return declaredDirection; }
    public List<Integer> getDeclaredChoices()      { return declaredChoice; }
    public SideID getOwner()            { return sideID; }
    public SideID getController()       { return sideID; }
    public SideID getUser()             { return sideID; }
    public int getOriginalID()          { return -1; } // Commanders have the same originalID.
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
    public boolean isOriginallyToken()  { return false; }
    public boolean isDecisive()         { return false; }
    public boolean isTokenInstance()    { return false; }
    public boolean isDecisiveInstance() { return false; }
    public boolean isOccupying()        { return false; }
    public Effect getPlaceCost() { return null; }
    public EffectInstance getInstancedPlaceCost() { return null; }
    public List<Integer> getUsedByEffectID() { return usedByEffectID; }

    public Pile getHand()          { return hand; }
    public Pile getDeck()          { return drawPile; }
    public Pile getDestroyedPile() { return destroyedPile; }
    public Pile getDiscardPile()   { return discardPile; }
    public Pile getDisplacedPile() { return displacedPile; }
    public Pile getDecisivePile()  { return decisivePile; }

    /** Doesn't return cards a commander controls on the field. Use field.getCards() for that. */
    public List<CardInstance> getCards() {
        return Stream.of(hand, drawPile, destroyedPile, discardPile, displacedPile, decisivePile)
            .map(pile -> pile.contents)
            .flatMap(List::stream)
            .toList();
    }

    public List<CardInstance> getCardsFromTopOfDeck(int numOfCards) {
        if (numOfCards < 0) throw new IllegalArgumentException("Must get 0 or more cards from the top of the deck.");
        return drawPile.contents.subList(drawPile.contents.size() - 1 - numOfCards, drawPile.contents.size() - 1);
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
    public void setDeclaredAttribute(CardAttribute d) { declaredAttribute = d; }
    public void setDeclaredPile(Pile d)      { declaredPile = d; }
    public void setDeclaredDirection(Direction d) { declaredDirection = d; }
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
        Consumer<Pile> move = p -> {
            removeCard(card, card.getPosition().getBoardLocation());
            p.contents.add(card);
            card.setPosition(newPosition);
        };
        boolean wasRemoved = true;
        switch (newPosition.getBoardLocation()) {
            case DECK_ONE, DECK_TWO -> { // It's the match's job to shuffle after adding a card to the deck.
                move.accept(drawPile); }

            case HAND_ONE, HAND_TWO -> move.accept(hand);

            case DESTROYED_PILE_ONE, DESTROYED_PILE_TWO -> move.accept(destroyedPile);

            case DISCARD_PILE_ONE , DISCARD_PILE_TWO -> move.accept(discardPile);


            case DISPLACED_PILE_ONE, DISPLACED_PILE_TWO -> move.accept(displacedPile);


            case DECISIVE_PILE_ONE, DECISIVE_PILE_TWO -> move.accept(decisivePile);

            default -> throw new IllegalArgumentException(this + " cannot add a card to " + newPosition);
        }
        return wasRemoved;
    }

    /** Removes a card from a commander's lists if it's in the location. */
    public void removeCard(CardInstance card, BoardLocation location) throws IllegalStateException {
        Consumer<Pile> remove = p -> {
            for (int i = 0; i < p.contents.size(); i++) {
                if (p.contents.get(i).equals(card)) {
                    p.contents.remove(i);
                    return;
                }
            }
            throw new IllegalArgumentException(card + " not fount at " + location + " for " + this);
        };
        switch(location) {
            case DECK_ONE, DECK_TWO -> { remove.accept(drawPile); }
            case HAND_ONE, HAND_TWO -> { remove.accept(hand); }
            case DESTROYED_PILE_ONE, DESTROYED_PILE_TWO -> { remove.accept(destroyedPile); }
            case DISCARD_PILE_ONE , DISCARD_PILE_TWO -> { remove.accept(discardPile); }
            case DISPLACED_PILE_ONE, DISPLACED_PILE_TWO -> { remove.accept(displacedPile); }
            case DECISIVE_PILE_ONE, DECISIVE_PILE_TWO -> { remove.accept(decisivePile); }
        }
    }

    /**
     * Changes the position of a card in the deck based on its ID.
     * A card's Position doesn't hold an index of where the card is in a pile, so
     * this is the only check for deck position.
     */
     public void changeCardDeckPosition(int cardID, int newIndex) {
        for (int i = 0; i < drawPile.contents.size(); i++) {
            if (drawPile.contents.get(i).getInstanceID() == cardID) {
                CardInstance temp = drawPile.contents.get(i);
                drawPile.contents.remove(i);
                drawPile.contents.add(newIndex, temp);
                break;
            }
        }
     }

     /** Shuffles this commander's deck using the Fisher-Yates algorithm. */
     public void shuffleDeck(long shuffleRandomness) {
         Random random = new Random(shuffleRandomness);
         for (int i = drawPile.contents.size() - 1; i > 0; i--) {
             int j = random.nextInt(i); // random index from 0 to i
             CardInstance temp = drawPile.contents.get(i);
             drawPile.contents.remove(i);
             drawPile.contents.add(i, drawPile.contents.get(j));
             drawPile.contents.remove(j);
             drawPile.contents.add(j, temp);
         }
     }

    /**
     * Precision = 5 minus the largest difference among the terminator piles, min 0
     * (CR13.12.1). The terminator piles are destroyed, discarded, and displaced.
     */
    public int getPrecision() {
        int d = destroyedPile.contents.size();
        int x = discardPile.contents.size();
        int p = displacedPile.contents.size();
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
            new ArrayList<Condition>(),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(
                    EffectChangeType.Place,
                    List.of(TargetType.CardsInHand, TargetType.CardsYouUse, TargetType.PlacementCostIsMet),
                    1)
            ))
        ),
        Effect.ACTIVATABLE(
            "Place a face down decisive card from your decisive deck.",
            new ArrayList<Condition>(),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(
                    EffectChangeType.Place,
                    List.of(TargetType.CardsInDecisiveDeck, TargetType.CardsYouOwn, TargetType.DecisiveCards, TargetType.FaceDownCards, TargetType.PlacementCostIsMet),
                    1)
            ))
        ),
        Effect.ACTIVATABLE(
            "Place a face up card from your decisive deck.",
            new ArrayList<Condition>(),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(
                    EffectChangeType.Place,
                    List.of(TargetType.CardsInDecisiveDeck, TargetType.CardsYouOwn, TargetType.FaceUpCards, TargetType.PlacementCostIsMet),
                    1)
            ))
        ),
        Effect.ACTIVATABLE(
            "Unshroud a card you control that wasn't shrouded this turn.",
            new ArrayList<Condition>(),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(
                    EffectChangeType.Unshroud,
                    List.of(TargetType.CardsYouControl, TargetType.ShroudedCards, TargetType.CardsNotShroudedThisTurn),
                    1)
            ))
        ),
        Effect.ACTIVATABLE(
            "Destroy a production card you control on a production zone.",
            new ArrayList<Condition>(),
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
