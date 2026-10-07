package ew.engine.board;

import ew.engine.cards.effects.Condition;
import ew.engine.cards.effects.EffectChange;
import ew.engine.cards.effects.EffectChangeType;
import ew.engine.cards.effects.TargetType;
import ew.engine.resolver.VariableGameNum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class EffectChangeInstance extends EffectChange implements EffectStipulationObject {
    EffectChangeType typeInstance;
    List<TargetType> targetRequirementsInstance;
    VariableGameNum strengthInstance;
    EffectDurationInstance durationInstance;
    List<ConditionInstance> instanceConditions;

    final LivingObject owner;
    final EffectInstance changeFrom;
    LivingObject user;
    final int effectInstanceID; // effectID's can conflict with living objects. They aren't living objects.
    int timesUsed = 0;
    boolean isNegated = false;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EffectChangeInstance)) return false;
        return ((EffectChangeInstance) other).getEffectID() == effectInstanceID;
    }

    public boolean typeEquals(EffectObject other) {
        if (!(other instanceof EffectChangeInstance)) return false;
        return ((EffectChangeInstance) other).getTypeInstance() == typeInstance;
    }

    public boolean equalsType(EffectChangeInstance other) {
        return typeInstance == other.typeInstance;
    }

    public EffectInstance getEffectFrom() { return changeFrom; }
    public List<ConditionInstance> getInstanceConditions() { return instanceConditions; }
    public EffectChangeType getTypeInstance() { return typeInstance; }
    public List<TargetType> getTargetInstance() { return targetRequirementsInstance; }
    public VariableGameNum getStrengthInstance() { return strengthInstance; }
    public EffectDurationInstance getDurationInstance() { return durationInstance; }
    public LivingObject getOwner() { return owner; }
    public LivingObject getUser() { return user; }
    public int getEffectID() { return effectInstanceID; }
    public int getTimesUsedThisTurn() { return timesUsed; }
    public boolean isNegated() { return isNegated; }
    public EffectChangeInstance getEffectChangeFrom() { return this; }
    public boolean isCost() { return this.getEffectFrom().getInstanceCost().contains(this); }

    public void setInstanceConditions(List<ConditionInstance> conditions) { this.instanceConditions = conditions; }
    public void setTypeInstance(EffectChangeType effectChangeType) { this.typeInstance = effectChangeType; }
    public void setTargetInstance(List<TargetType> newTargetReqs) { this.targetRequirementsInstance = newTargetReqs; }
    public void setStrengthInstance(VariableGameNum newStrength) { this.strengthInstance = newStrength; }
    public void setDurationInstance(EffectDurationInstance newDuration) { this.durationInstance = newDuration; }
    public void tickTimesUsed() { ++timesUsed; }
    public void setTimesUsedThisTurn(int timesUsedThisTurn) { this.timesUsed = timesUsedThisTurn; }
    public void setNegation(boolean negated) { this.isNegated = negated; }
    public void setUser(LivingObject newUser) { this.user = newUser; }

    public EffectChangeInstance returnAndSetTypeInstance(EffectChangeType newType) { this.typeInstance = newType; return this; }
    public EffectChangeInstance returnAndSetTypeAndStrengthInstance(EffectChangeType newType, VariableGameNum newStrength) { this.typeInstance = newType; this.strengthInstance = newStrength; return this; }

    public EffectChangeInstance(EffectChange effectChange, LivingObject owner, AtomicInteger nextEffectIDs, EffectInstance changeFrom) {
        super(effectChange.getChangeConditions(), effectChange.getType(), effectChange.getTargetRequirements(), effectChange.getStrength(), effectChange.getDuration());
        this.owner = owner;
        this.changeFrom = changeFrom;
        this.user = owner;
        this.effectInstanceID = nextEffectIDs.getAndIncrement();
        this.typeInstance = effectChange.getType();
        this.targetRequirementsInstance = effectChange.getTargetRequirements();
        this.strengthInstance = effectChange.getStrength();
        this.instanceConditions = new ArrayList<ConditionInstance>();
        for (Condition condition : effectChange.getChangeConditions()) {
            instanceConditions.add(new ConditionInstance(condition, owner, nextEffectIDs, changeFrom));
        }
        this.durationInstance = new EffectDurationInstance(effectChange.getDuration(), owner, nextEffectIDs, this);
    }
}
