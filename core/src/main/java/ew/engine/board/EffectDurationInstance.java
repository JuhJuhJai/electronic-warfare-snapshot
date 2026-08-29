package ew.engine.board;

import ew.engine.cards.effects.*;
import ew.engine.resolver.VariableGameNum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Class that saves the duration of effectChanges that have a duration. */
public final class EffectDurationInstance extends EffectDuration implements EffectStipulationObject {
    List<ConditionInstance> instanceConditions;
    ConditionType ticksInstance;
    List<TargetType> targetRequirementsInstance;
    VariableGameNum strengthInstance;

    final LivingObject owner;
    final EffectInstance changeFrom;
    LivingObject user;
    final int effectInstanceID;
    int timesUsed = 0;
    boolean isNegated = false;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EffectDurationInstance)) return false;
        return ((EffectDurationInstance) other).getEffectID() == effectInstanceID;
    }
    public boolean typeEquals(EffectObject other) {
        if (!(other instanceof EffectDurationInstance)) return false;
        return ((EffectDurationInstance) other).getTicksOnInstance() == ticksInstance;
    }

    public List<ConditionInstance> getInstanceConditions() { return instanceConditions; }
    public EffectInstance getEffectFrom() { return changeFrom; }
    public ConditionType getTicksOnInstance() { return ticksInstance; }
    public List<TargetType> getTargetInstance() { return targetRequirementsInstance; }
    public VariableGameNum getStrengthInstance() { return strengthInstance; }
    public LivingObject getOwner() { return owner; }
    public LivingObject getUser() { return user; }
    public int getEffectID() { return effectInstanceID; }
    public int getTimesUsedThisTurn() { return timesUsed; }
    public boolean isNegated() { return isNegated; }

    public void setInstanceConditions(List<ConditionInstance> conditions) { this.instanceConditions = conditions; }
    public void tickTimesUsed() { ++timesUsed; }
    public void setTimesUsedThisTurn(int timesUsedThisTurn) { this.timesUsed = timesUsedThisTurn; }
    public void setNegation(boolean negated) { this.isNegated = negated; }
    public void setUser(LivingObject newUser) { this.user = newUser; }
    public void setTargetInstance(List<TargetType> newTargetReqs) { this.targetRequirementsInstance = newTargetReqs; }
    public void setStrengthInstance(VariableGameNum newStrength) { this.strengthInstance = newStrength; }

    public EffectDurationInstance(EffectDuration duration, LivingObject owner, AtomicInteger nextEffectIDs, EffectInstance changeFrom) {
        super(duration.getDurationCondition(), duration.getTicksOn(), duration.getStrength());
        this.instanceConditions = new ArrayList<ConditionInstance>();
        for (Condition condition : duration.getDurationCondition()) {
            instanceConditions.add(new ConditionInstance(condition, owner, nextEffectIDs, changeFrom));
        }
        this.changeFrom = changeFrom;
        this.strengthInstance = duration.getStrength();
        this.ticksInstance = duration.getTicksOn();
        this.owner = owner;
        this.user = owner;
        this.effectInstanceID = nextEffectIDs.getAndIncrement();
    }

    public EffectDurationInstance(EffectDurationInstance copyInstance) {
        super(copyInstance.getDurationCondition(), copyInstance.getTicksOnInstance(), copyInstance.getStrengthInstance());
        this.targetRequirementsInstance = copyInstance.getTargetInstance();
        this.strengthInstance = copyInstance.getStrengthInstance();
        this.changeFrom = copyInstance.getEffectFrom();
        this.ticksInstance = copyInstance.getTicksOnInstance();
        this.owner = copyInstance.getOwner();
        this.user = copyInstance.getUser();
        this.instanceConditions = copyInstance.getInstanceConditions();
        this.effectInstanceID = copyInstance.getEffectID();
        this.timesUsed = copyInstance.getTimesUsedThisTurn();
        this.isNegated = copyInstance.isNegated();
    }
}
