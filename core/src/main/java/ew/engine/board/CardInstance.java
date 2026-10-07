package ew.engine.board;

import ew.engine.cards.CardAttribute;
import ew.engine.cards.CardDefinition;
import ew.engine.cards.CardRarity;
import ew.engine.cards.CardType;
import ew.engine.cards.effects.Effect;
import ew.playerData.CardModifier;
import ew.playerData.CardStored;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Design note: a Card owns its own per-commander visibility (face-up / face-down /
 * revealed). The question "what does player X see of this card?" is answered BY the card,
 * which is why the board, hand, and piles simply hold {@code Card} references and never
 * store a separate visibility map. The per-player view of the whole game (see
 * {@code GameState.view(PlayerId)}) is built by asking each card for its viewer-visible form.
 */
public final class CardInstance implements LivingObject {
    final int cardID;
    final CardRarity rarity;
    final String cardArt;
    final String name;
    final CardType type;
    final CardAttribute attribute;
    final int health;
    final boolean isOriginallyToken;
    final boolean isDecisive;
    final Effect placeCost;
    final List<Effect> effects;

    final CardModifier modifier; // for displaying the card

    CardType typeInstance;
    int healthInstance;
    CardAttribute attributeInstance;
    String nameInstance;
    EffectInstance instancedPlaceCost;
    List<EffectInstance> instancedEffects = new ArrayList<EffectInstance>();
    int cardInstanceId;
    int cardPriority;
    final CommanderInstance owner;
    SideID controller = SideID.NEUTRAL;
    SideID user;
    ArrayList<CounterType> counters = new ArrayList<>();
    Position position;
    boolean commanderOneVis = false;
    boolean commanderTwoVis = false;
    boolean faceUp = false;
    boolean revealed = false;
    boolean shrouded = false;
    boolean shroudedThisTurn = false;
    boolean occupiesZone = false;
    boolean isNegated = false;
    boolean isDestroyed = false; // for when the card is destroyed on the field. False when a card is in the destroyed pile.
    boolean isRode = false;
    boolean isRider = false;
    boolean isExcavated = false;
    boolean isSearched = false;
    boolean isDecisiveInstance;
    boolean isTokenInstance;
    Position undisplacementPosition = null;
    EffectDurationInstance undisplacementTime = null;
    List<Integer> usedByEffectID;

    List<TargetInstance> target = new ArrayList<>();
    int Xvariable = 0;
    int Qvariable = 0;
    int declaredNum = 0;
    int declaredName = 0; // x = cardID associated with declared name in CardDefinition
    CardAttribute declaredAttribute = null;
    Pile declaredPile = null;
    Direction declaredDirection = null;
    List<Integer> declaredChoice =  new ArrayList<>(2); // for "Choose x: ..." effects.

    public CardInstance(CommanderInstance owner, CardStored card, AtomicInteger nextCardIDs, AtomicInteger nextEffectIDs) {
        CardDefinition reference = new CardDefinition(card.cardID());
        this.cardID = reference.cardID;
        this.rarity = reference.rarity;
        this.cardArt = reference.cardArt;
        this.name = reference.name;
        this.type = reference.type;
        this.attribute = reference.attribute;
        this.health = reference.health;
        this.isOriginallyToken = reference.isToken;
        this.isDecisive = reference.isDecisive;
        this.placeCost = reference.placeCost;
        this.effects = reference.effects;

        this.typeInstance = type;
        this.healthInstance = health;
        this.attributeInstance = attribute;
        this.nameInstance = name;
        this.isTokenInstance = isOriginallyToken;
        this.isDecisiveInstance = isDecisive;

        this.owner = owner;
        this.user = owner.getSide();
        this.position = owner.getSide() == SideID.ONE ? new Position(BoardLocation.DECK_ONE, CardRideState.NORMAL, owner.getDeck().contents.size()) : new Position(BoardLocation.DECK_TWO, CardRideState.NORMAL, owner.getDeck().contents.size());
        this.modifier = card.modifier();
        this.cardInstanceId = this.cardPriority = nextCardIDs.getAndIncrement();
        this.instancedPlaceCost = new EffectInstance(placeCost, this, nextEffectIDs);
        for (Effect effect : effects) instancedEffects.add(new EffectInstance(effect, this, nextEffectIDs));
    }

    /** Used for cards that are made during the duration of the game, usually tokens. Their modifier is always NORMAL. */
    private CardInstance(CommanderInstance owner, int cardDefinitionID, AtomicInteger nextCardIDs, AtomicInteger nextEffectIDs) {
        CardDefinition reference = new CardDefinition(cardDefinitionID);
        this.cardID = reference.cardID;
        this.rarity = reference.rarity;
        this.cardArt = reference.cardArt;
        this.name = reference.name;
        this.type = reference.type;
        this.attribute = reference.attribute;
        this.health = reference.health;
        this.isOriginallyToken = reference.isToken;
        this.isDecisive = reference.isDecisive;
        this.placeCost = reference.placeCost;
        this.effects = reference.effects;

        this.typeInstance = type;
        this.healthInstance = health;
        this.attributeInstance = attribute;
        this.nameInstance = name;
        this.isTokenInstance = isOriginallyToken;
        this.isDecisiveInstance = isDecisive;

        this.owner = owner;
        this.user = owner.getSide();
        this.position = owner.getSide() == SideID.ONE ?
            new Position(BoardLocation.DECK_ONE, CardRideState.NORMAL, owner.getDeck().contents.size())
            : new Position(BoardLocation.DECK_TWO, CardRideState.NORMAL, owner.getDeck().contents.size());
        this.modifier = CardModifier.NORMAL;
        this.cardInstanceId = this.cardPriority = nextCardIDs.getAndIncrement();
        this.instancedPlaceCost = new EffectInstance(placeCost, this, nextEffectIDs);
        for (Effect effect : effects) instancedEffects.add(new EffectInstance(effect, this, nextEffectIDs));
    }

    public static CardInstance TOKEN(CommanderInstance owner, int cardDefinitionID, AtomicInteger nextCardIDs, AtomicInteger nextEffectIDs) {
        return new CardInstance(owner, cardDefinitionID, nextCardIDs, nextEffectIDs);
    }

    public LivingObject getSelf() { return this; }
    // Getters and setters for resolution purposes.
    public CardType getCardType()       { return type; }
    public CardType getCardTypeInstance() { return typeInstance; }
    public String getOriginalName()     { return name; }
    public String getNameInstance()     { return nameInstance; }
    public List<Effect> getEffects()    { return effects; }
    public List<EffectInstance> getInstancedEffects() { return instancedEffects; }
    public Effect getPlaceCost()        { return placeCost; }
    public EffectInstance getInstancedPlaceCost() { return instancedPlaceCost; }
    public Position getPosition()       { return position; }
    public int getHealth()              { return health; }
    public int getHealthInstance()      { return healthInstance; }
    public CardAttribute getOriginalAttribute() { return attribute; }
    public CardAttribute getInstanceAttribute() { return attributeInstance; }
    public SideID getOwner()            { return owner.getOwner(); }
    public SideID getController()       { return controller; }
    public SideID getUser()             { return user; }
    public int getOriginalID()          { return cardID; }
    public int getInstanceID()          { return cardInstanceId; }
    public int getPriority()            { return cardPriority; }
    public int getX()                   { return Xvariable; }
    public int getQ()                   { return Qvariable; }
    public int getDeclaredNum()         { return declaredNum; }
    public int getDeclaredName()        { return declaredName; }
    public CardAttribute getDeclaredAttribute()   { return declaredAttribute; }
    public Pile getDeclaredPile()        { return declaredPile; }
    public Direction getDeclaredDirection() { return declaredDirection; }
    public List<Integer> getDeclaredChoices() { return declaredChoice; }
    public boolean isNegated()          { return isNegated; }
    public List<CounterType> getCounters()  { return counters; }
    public boolean hasCounter(CounterType counter) { return counters.contains(counter); }
    /** True if any counter in the list matches with a counterType of the obj */
    public boolean hasCounter(List<CounterType> counters) { return !counters.stream().filter(this.counters::contains).toList().isEmpty(); }    public List<TargetInstance> getTarget() { return target; }
    public boolean getCommanderOneVis() { return commanderOneVis; }
    public boolean getCommanderTwoVis() { return commanderTwoVis; }
    public boolean isFaceUp()           { return faceUp; }
    public boolean isRevealed()         { return revealed; }
    public boolean isShrouded()         { return shrouded; }
    public boolean wasShroudedThisTurn() { return shroudedThisTurn; }
    public Position getUndisplacementLocation() { return undisplacementPosition; }
    public EffectDurationInstance getUndisplacementTime() { return undisplacementTime; }
    public boolean isRode()             { return isRode; }
    public boolean isRider()            { return isRider; }
    public boolean isDestroyed()        { return isDestroyed; }
    public boolean isExcavated()        { return isExcavated; }
    public boolean isSearched()         { return isSearched; }
    public boolean isOriginallyToken()  { return isOriginallyToken; }
    public boolean isDecisive()         { return isDecisive; }
    public boolean isTokenInstance()    { return isTokenInstance; }
    public boolean isDecisiveInstance() { return isDecisiveInstance; }
    public boolean isOccupying()        { return occupiesZone; }
    public List<Integer> getUsedByEffectID() { return usedByEffectID; }

    public void setTypeInstance(CardType newType) { this.typeInstance = newType; }
    public void setName(String newName) { this.nameInstance = newName; }
    public void setHealthInstance(int healthInstance) { this.healthInstance = healthInstance; }
    public void setAttributeInstance(CardAttribute attribute) { this.attributeInstance = attribute; }
    public void setPriority(int priority) { cardPriority = priority; }
    public void setPosition(Position newPosition) { position = newPosition; }
    public void setController(SideID newController) { controller = newController; }
    public void setUser(SideID newUser) { user = newUser; }
    public void setX(int x)             { Xvariable = x; }
    public void setQ(int q)             { Qvariable = q; }
    public void setDeclaredNum(int d)   { declaredNum = d; }
    public void setDeclaredName(int d)  { declaredName = d; }
    public void setDeclaredAttribute(CardAttribute d)   { declaredAttribute = d; }
    public void setDeclaredPile(Pile d)  { declaredPile = d; }
    public void setDeclaredDirection(Direction d)   { declaredDirection = d; }
    public void setDeclaredChoices(List<Integer> choice) { declaredChoice = choice; }
    public void addEffect(EffectInstance effect) { this.instancedEffects.add(effect); }
    public void removeEffect(int effectID) { for (int i = 0; i < instancedEffects.size(); i++) if (instancedEffects.get(i).getEffectID() == effectID) { instancedEffects.remove(i); return; }
        throw new IllegalArgumentException(cardID + " effect " + effectID + " not found to remove."); }
    public void resetEffects(AtomicInteger nextEffectIDs) {
        this.instancedPlaceCost = new EffectInstance(placeCost, owner, nextEffectIDs);
        instancedEffects = new ArrayList<>();
        for (Effect effect : effects) instancedEffects.add(new EffectInstance(effect, owner, nextEffectIDs));
    }
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
        if (amount != 0) throw new RuntimeException(cardInstanceId + " doesn't have enough " + counter + " counters to remove.");
    }
    public void setCommanderOneVis(boolean vis)     { this.commanderOneVis = vis; }
    public void setCommanderTwoVis(boolean vis)     { this.commanderTwoVis = vis; }
    public void setFaceUp(boolean faceUp)           { this.faceUp = faceUp; }
    public void setRevealed(boolean revealed)       { this.revealed = revealed; }
    public void setShrouded(boolean shrouded)       { this.shrouded = shrouded; }
    public void setShroudedThisTurn(boolean shroudedThisTurn) { this.shroudedThisTurn = shroudedThisTurn; }
    public void setUndisplacementPosition(Position pos) { this.undisplacementPosition = pos; }
    public void setUndisplacementTime(EffectDurationInstance undisplacementTime) { this.undisplacementTime = undisplacementTime; }
    public void setNegated(boolean negated)         { this.isNegated = negated; }
    public void setZoneOccupation(boolean occupies) { this.occupiesZone = occupies; }
    public void setExcavated(boolean excavated)     { this.isExcavated = excavated; }
    public void setSearched(boolean searched)       { this.isSearched = searched; }
    public void setRideState(boolean riding, boolean rode) { this.isRider = riding; this.isRode = rode; }
    public void setDestroyedState(boolean destroyed) { this.isDestroyed = destroyed; }
    public void addTarget(TargetInstance target)     { this.target.add(target); }
    public void addTarget(List<TargetInstance> target) { this.target.addAll(target); }
    public void clearTargets()                       { this.target = new ArrayList<>(); }
    public void setDecisiveState(boolean isDecisive) { this.isDecisiveInstance = isDecisive; }
    public void setTokenState(boolean isToken)       { this.isTokenInstance = isToken; }
    public void setUsedByEffectID(List<Integer> usingEffectID) { this.usedByEffectID = usingEffectID; }

    public boolean equals(Object o) {
        if (!(o instanceof CardInstance other)) return false;
        return other.getInstanceID() == cardInstanceId;
    }
    public int hashCode() {
        return Objects.hash(cardID, rarity, cardArt, name, cardInstanceId);
    }
    public String toString() { return nameInstance + " cardID " + cardInstanceId + " at " + position; }
}
