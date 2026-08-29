package ew.engine.board;

import ew.engine.cards.effects.Condition;
import ew.engine.cards.effects.Effect;
import ew.engine.cards.effects.EffectChange;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EffectInstance extends Effect implements EffectObject {
    /** <p>Variables from extension:
     *     ArrayList<Condition> conditions;
     *     ArrayList<EffectChange> cost;
     *     ArrayList<EffectChange> effect;
     *     EffectType type;
     *     String effectText;
     *     boolean usableWhileShrouded;
     *
     *  <p>.equals provided as well.
     */

    ArrayList<ConditionInstance> instanceConditions;
    ArrayList<EffectChangeInstance> instanceCost;
    ArrayList<EffectChangeInstance> instanceEffectChanges;

    final LivingObject owner;
    LivingObject user;
    final int effectInstanceID;
    int timesUsedThisTurn = 0;
    boolean isNegated = false;

    /** An effectInstance's equals checks for the same InstanceID. */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EffectInstance)) return false;
        return ((EffectInstance) other).getEffectID() == effectInstanceID;
    }

    public boolean typeEquals(EffectObject other) {
        if (!(other instanceof EffectInstance)) return false;
        return ((EffectInstance) other).getType() == type;
    }

    @Override
    public String toString() {
        return getEffectID() + ": " + getEffectText();
    }

    public List<ConditionInstance> getInstanceConditions() { return instanceConditions; }
    public List<EffectChangeInstance> getInstanceCost() { return instanceCost; }
    public List<EffectChangeInstance> getInstanceEffectChanges() { return instanceEffectChanges; }
    public EffectInstance getEffectFrom() { return this; }
    public LivingObject getOwner() { return owner; }
    public LivingObject getUser() { return user; }
    public int getEffectID() { return effectInstanceID; }
    public int getTimesUsedThisTurn() { return timesUsedThisTurn; }
    public boolean isNegated() { return isNegated; }

    public void setInstanceConditions(List<ConditionInstance> conditions) { this.instanceConditions = (ArrayList<ConditionInstance>) conditions; }
    public void setInstanceCost(List<EffectChangeInstance> costs) { this.instanceCost = (ArrayList<EffectChangeInstance>) costs; }
    public void setInstanceEffects(List<EffectChangeInstance> effectChanges) { this.instanceEffectChanges = (ArrayList<EffectChangeInstance>) effectChanges; }
    public void tickTimesUsed() { ++timesUsedThisTurn; }
    public void setTimesUsedThisTurn(int timesUsedThisTurn) { this.timesUsedThisTurn = timesUsedThisTurn; }
    public void setNegation(boolean negated) { this.isNegated = negated; }
    public void setUser(LivingObject newUser) { this.user = newUser; }

    public boolean equals(EffectInstance other) {
        return this.getCostChanges().equals(other.getCostChanges()) && this.getEffectChanges().equals(other.getEffectChanges());
    }

    public EffectInstance(Effect effect, LivingObject owner, AtomicInteger nextEffectIDs) {
        super (effect.getEffectText(), effect.isUsableWhileShrouded(), effect.getType(), effect.getConditions(), effect.getCostChanges(), effect.getEffectChanges());
        this.owner = owner;
        this.user = owner;
        this.effectInstanceID = nextEffectIDs.getAndIncrement();
        for (Condition condition : effect.getConditions()) { instanceConditions.add(
            new ConditionInstance(condition, owner, nextEffectIDs, this)); }
        for (EffectChange effectChange : effect.getCostChanges()) { instanceCost.add(
            new EffectChangeInstance(effectChange, owner, nextEffectIDs, this)); }
        for (EffectChange effectChange : effect.getEffectChanges()) { instanceEffectChanges.add(
            new EffectChangeInstance(effectChange, owner, nextEffectIDs, this)); }
    }
}
