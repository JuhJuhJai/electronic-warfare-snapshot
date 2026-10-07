package ew.engine.board;

import ew.engine.cards.effects.Condition;
import ew.engine.cards.effects.ConditionType;
import ew.engine.cards.effects.TargetType;
import ew.engine.resolver.VariableGameNum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ConditionInstance extends Condition implements EffectStipulationObject {
    /** Variables from extension:
     *     List<Condition> conditions;
     *     ConditionType type;
     *     ArrayList<TargetType> target; // Multiple targets indicates more stipulations checked cards must meet.
     *     VariableGameNum check;
     *
     *  Getters provided for each.
     */

    final LivingObject owner;
    final EffectInstance changeFrom;
    ConditionType typeInstance;
    ArrayList<TargetType> targetInstance;
    VariableGameNum checkInstance;
    List<ConditionInstance> instanceConditions;
    final EffectChangeInstance effectChangeFrom;

    LivingObject user;
    final int effectInstanceID; // effectID's can conflict with living objects. They aren't living objects.
    int timesUsed = 0;
    boolean isNegated = false;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ConditionInstance)) return false;
        return ((ConditionInstance) other).getEffectID() == effectInstanceID;
    }

    public boolean typeEquals(EffectObject other) {
        if (!(other instanceof ConditionInstance)) return false;
        return ((ConditionInstance) other).getTypeInstance() == typeInstance;
    }

    public List<ConditionInstance> getInstanceConditions() { return instanceConditions; }
    public EffectInstance getEffectFrom() { return changeFrom; }
    public ConditionType getTypeInstance() { return typeInstance; }
    public List<TargetType> getTargetInstance() { return targetInstance; }
    public VariableGameNum getStrengthInstance() { return checkInstance; }
    public LivingObject getOwner() { return owner; }
    public int getEffectID() { return effectInstanceID; }
    public int getTimesUsedThisTurn() { return timesUsed; }
    public boolean isNegated() { return isNegated; }
    public LivingObject getUser() { return user; }
    public EffectChangeInstance getEffectChangeFrom() { return effectChangeFrom; }
    public boolean isCost() {
        return this.getEffectFrom().getInstanceCost().stream()
            .map(EffectChangeInstance::getInstanceConditions)
            .flatMap(List::stream)
            .toList()
            .contains(this);
    }

    public void setInstanceConditions(List<ConditionInstance> conditions) { this.instanceConditions = (ArrayList<ConditionInstance>) conditions; }
    public void setTypeInstance(ConditionType conditionType) { this.typeInstance = conditionType; }
    public void setTargetInstance(List<TargetType> newTargetReqs) { this.targetInstance = (ArrayList<TargetType>) newTargetReqs; }
    public void setStrengthInstance(VariableGameNum newStrength) { this.checkInstance = newStrength; }
    public void tickTimesUsed() { ++timesUsed; }
    public void setTimesUsedThisTurn(int timesUsedThisTurn) { this.timesUsed = timesUsedThisTurn; }
    public void setNegation(boolean negated) { this.isNegated = negated; }
    public void setUser(LivingObject newUser) { this.user = newUser; }

    public ConditionInstance(Condition condition, LivingObject owner, AtomicInteger nextEffectIDs, EffectInstance changeFrom) {
        super(condition.getCheckConditions(), condition.getType(), condition.getTarget(), condition.getCheck());
        this.owner = owner;
        this.changeFrom = changeFrom;
        this.user = owner;
        this.effectInstanceID = nextEffectIDs.getAndIncrement();
        this.typeInstance = condition.getType();
        this.targetInstance = (ArrayList<TargetType>) condition.getTarget();
        this.checkInstance = condition.getCheck();
        this.instanceConditions = new ArrayList<ConditionInstance>();
        for (Condition checkCondition : condition.getCheckConditions()) {
            instanceConditions.add(new ConditionInstance(checkCondition, owner, nextEffectIDs, changeFrom));
        }
        this.effectChangeFrom = null;
    }

    public ConditionInstance(Condition condition, LivingObject owner, AtomicInteger nextEffectIDs, EffectChangeInstance changeFrom) {
        super(condition.getCheckConditions(), condition.getType(), condition.getTarget(), condition.getCheck());
        this.owner = owner;
        this.changeFrom = changeFrom.getEffectFrom();
        this.user = owner;
        this.effectInstanceID = nextEffectIDs.getAndIncrement();
        this.typeInstance = condition.getType();
        this.targetInstance = (ArrayList<TargetType>) condition.getTarget();
        this.checkInstance = condition.getCheck();
        this.instanceConditions = new ArrayList<ConditionInstance>();
        for (Condition checkCondition : condition.getCheckConditions()) {
            instanceConditions.add(new ConditionInstance(checkCondition, owner, nextEffectIDs, changeFrom));
        }
        this.effectChangeFrom = changeFrom;
    }

    public ConditionInstance(ConditionInstance condition) {
        super (condition.getCheckConditions(), condition.getType(), condition.getTarget(), condition.getCheck());
        this.owner = condition.getOwner();
        this.changeFrom = condition.getEffectFrom();
        this.user = condition.getUser();
        this.effectInstanceID = condition.getEffectID();
        this.typeInstance = condition.getType();
        this.targetInstance = (ArrayList<TargetType>) condition.getTarget();
        this.checkInstance = condition.getCheck();
        this.instanceConditions = condition.getInstanceConditions().stream().toList();
        this.effectChangeFrom = condition.getEffectChangeFrom();
    }
}
