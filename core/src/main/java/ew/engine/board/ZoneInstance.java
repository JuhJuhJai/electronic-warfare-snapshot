package ew.engine.board;

import ew.engine.cards.CardAttribute;
import ew.engine.cards.CardType;
import ew.engine.cards.effects.Effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A single cell of the field. Its {@link #position}, {@link #type}, and {@link #side} are
 * fixed for the life of the board; only the cards on it and any gained effects change.
 * Because it can gain effects, it is a Living Object.
 */
public final class ZoneInstance implements LivingObject {

    private final Position position;
    private final ZoneType type;
    /** Owning side for placement. null for the neutral middle (OPEN_FIELD / EMPTY_SPACE). */
    private final SideID side;

    /**
     * Cards physically present on this zone. Normally 0 or 1. Two cards share a zone when
     * one rides the other (rider + rode), and a destroyed-but-not-yet-removed card may
     * linger here alongside a freshly placed card (CR12.3.3, CR13.6.5).
     */
    private final List<CardInstance> cards = new ArrayList<>();

    /**
     * Effects a zone has gained via an effect (CR3.5.3); retained until removed (CR3.5.5).
     * Zones never hold counters.
     */
    private List<EffectInstance> instancedEffects = new ArrayList<>();

    final int instanceID;
    final int priority;
    SideID controller;
    boolean isNegated = false;
    boolean commanderOneVis = false;
    boolean commanderTwoVis = false;
    boolean isRevealed = false;

    List<TargetInstance> target = new ArrayList<>();
    List<Integer> usedByEffectID;
    int Xvariable = 0;
    int Qvariable = 0;
    int declaredNum = 0;
    int declaredName = 0; // x = name of the cardID associated with x in CardDefinition
    CardAttribute declaredAttribute = null;
    Pile declaredPile = null;
    Direction declaredDirection = null;
    List<Integer> declaredChoice = new ArrayList<>(1); // for "Choose x: ..." effects.

    public String toString() { return type.toString() + " at " + position; }

    public ZoneInstance(Position position, ZoneType type, SideID side, int instanceID) {
        this.position = position;
        this.type = type;
        this.side = side;
        this.instanceID = instanceID;
        this.priority = instanceID;
    }

    public LivingObject getSelf() { return this; }
    public ZoneType getType() { return type; }
    public CardType getCardType() { return CardType.ZONE; }
    public CardType getCardTypeInstance() { return CardType.ZONE; }
    public SideID getSide() { return side; }
    public List<CardInstance> getCards() { return cards; }
    public boolean isEmpty() { return cards.isEmpty(); }
    /** Returns True if the zone is occupied, only checking cards on the zone for its "isOccupying" state. */
    public boolean isOccupied() {
        for (CardInstance card : cards) {
            if (card.occupiesZone) return true;
        }
        return false;
    }
    public void addCard(CardInstance card) { cards.add(card); }
    public void removeCard(int cardID) { for (int i = 0; i < cards.size(); i++) if (cards.get(i).getInstanceID() == cardID) { cards.remove(i); break; }
        throw new IllegalStateException("Card ID to remove not found."); }

    public String getOriginalName() { return this.type.toString(); }
    public String getNameInstance() { return this.type.toString(); }
    public int getHealthInstance() { return 0; }
    public int getHealth() { return 0; }
    public SideID getOwner() { return SideID.NEUTRAL; }
    public CardAttribute getOriginalAttribute() { return CardAttribute.None; }
    public CardAttribute getInstanceAttribute() { return CardAttribute.None; }
    public List<Effect> getEffects() { return new ArrayList<Effect>(0);  }
    public List<EffectInstance> getInstancedEffects() { return instancedEffects; }
    public int getPriority() { return priority; }
    public int getX()                   { return Xvariable; }
    public int getQ()                   { return Qvariable; }
    public int getDeclaredNum()         { return declaredNum; }
    public int getDeclaredName()        { return declaredName; }
    public CardAttribute getDeclaredAttribute()   { return declaredAttribute; }
    public Pile getDeclaredPile()        { return declaredPile; }
    public Direction getDeclaredDirection()   { return declaredDirection; }
    public List<Integer> getDeclaredChoices()      { return declaredChoice; }
    public List<CounterType> getCounters() { return new ArrayList<CounterType>(); }
    public boolean hasCounter(CounterType counter) { return false; }
    public boolean hasCounter(List<CounterType> counters) { return false; }
    /** If a commander controls every card on a zone, they control the zone. If a zone is empty, it is neutral. */
    public SideID getController() { if (cards.isEmpty()) return SideID.NEUTRAL; SideID x = SideID.NEUTRAL;
        for (CardInstance card : cards) if (card.getController() != x && x == SideID.NEUTRAL) x = card.getController(); else return SideID.NEUTRAL; return x; }
    public SideID getUser()             { return getController(); }
    public int getOriginalID()          { return instanceID * -1; }
    public int getInstanceID()          { return instanceID; }
    public List<TargetInstance> getTarget() { return target; }
    public Position getPosition()      { return position; }
    public boolean getCommanderOneVis() { return commanderOneVis; }
    public boolean getCommanderTwoVis() { return commanderTwoVis; }
    public boolean isFaceUp()           { return true; }
    public boolean isRevealed()         { return isRevealed; }
    public boolean isShrouded()         { return false; }
    public boolean wasShroudedThisTurn() { return false; }
    public boolean isNegated()          { return isNegated; }
    public Position getUndisplacementLocation() { return null; }
    public EffectDurationInstance getUndisplacementTime() { return null; }
    public boolean isRode()             { return false; }
    public boolean isRider()            { return false; }
    public boolean isDestroyed()        { return false; }
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

    public void setTypeInstance(CardType newType) { throw new ClassCastException("Attempted to set the CardType of " + this); }
    public void setName(String newName) { throw new ClassCastException("Attempted to set the name of " + this); }
    public void setHealthInstance(int healthInstance) { throw new ClassCastException("Attempted to set the health of " + this); }
    public void setAttributeInstance(CardAttribute attribute) { throw new ClassCastException("Attempted to set the attribute of " + this); }
    public void addCounter(CounterType counter, int amount) { throw new ClassCastException("Attempted to add counter to " + this); }
    public void removeCounter(CounterType counter, int amount) { throw new ClassCastException("Attempted to remove counter from " + this); }
    public void setPriority(int priority) { throw new ClassCastException("Attempted to set the priority of " + this); }
    public void setPosition(Position newPosition) { throw new ClassCastException("Attempted to set the position of " + this); }
    public void setController(SideID newController) { this.controller = newController; } // remember to set this using the equation above in MATCH, after every gamestate change
    public void setUser(SideID newUser)     { this.controller = newUser; }
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
        throw new IllegalArgumentException(this + " effect " + effectID + " not found to remove."); }
    public void resetEffects(AtomicInteger nextEffectIDs) { instancedEffects = new ArrayList<>(); }
    public void setCommanderOneVis(boolean vis) { this.commanderOneVis = vis; }
    public void setCommanderTwoVis(boolean vis) { this.commanderTwoVis = vis; }
    public void setFaceUp(boolean faceUp)       { throw new ClassCastException("Attempted to set the faceUpState of " + this); }
    public void setRevealed(boolean revealed)   { this.isRevealed = revealed; }
    public void setShrouded(boolean shrouded)   { throw new ClassCastException("Attempted to set the shrouded state of " + this); }
    public void setShroudedThisTurn(boolean shroudedThisTurn) { throw new ClassCastException("Attempted to set the shroudedThisTurn of " + this); }
    public void setUndisplacementPosition(Position pos) { throw new ClassCastException("Attempted to set the undisplacement position of " + this); }
    public void setUndisplacementTime(EffectDurationInstance e) { throw new ClassCastException("Attempted to set the undisplacement time of " + this); }
    public void setNegated(boolean negated)         { this.isNegated = negated; }
    public void setZoneOccupation(boolean occupies) { throw new ClassCastException("Attempted to set the zone occupation of " + this); }
    public void setExcavated(boolean excavated)     { throw new ClassCastException("Attempted to set the excavated state of " + this); }
    public void setSearched(boolean searched)       { throw new ClassCastException("Attempted to set the searched state of " + this); }
    public void setRideState(boolean riding, boolean ridden) { throw new ClassCastException("Attempted to set the riding state of " + this); }
    public void setDestroyedState(boolean destroyed) { throw new ClassCastException("Attempted to set the destroyed state of " + this); }
    public void addTarget(TargetInstance target)    { this.target.add(target); }
    public void addTarget(List<TargetInstance> target) { this.target.addAll(target); }
    public void clearTargets()                      { this.target = new ArrayList<TargetInstance>(); }
    public void setDecisiveState(boolean isDecisive) { throw new ClassCastException("Attempted to set the decisive state of " + this); }
    public void setTokenState(boolean isToken) { throw new ClassCastException("Attempted to set the token state of " + this); }
    public void setUsedByEffectID(List<Integer> usingEffectID) { this.usedByEffectID = usingEffectID; }
    public boolean equals(Object o) {
        if (!(o instanceof ZoneInstance other)) return false;
        return other.getInstanceID() == instanceID;
    }
    public int hashCode() {
        return Objects.hash(position, type);
    }
}
