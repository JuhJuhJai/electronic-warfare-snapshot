package ew.engine.board;

import ew.engine.cards.effects.EffectChange;
import ew.engine.cards.effects.EffectChangeType;
import ew.engine.cards.effects.TargetType;
import ew.engine.resolver.VariableGameNum;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructed with an EffectChangeInstance, performs a deep copy, minus the EffectFrom.
 * Has all getter functions from EffectChangeInstance
 */
public class ImmutableEffectChangeInstance extends EffectChange implements ImmutableEffectStipulationObject{
    final EffectChangeType typeInstance;
    final List<TargetType> targetRequirementsInstance;
    final VariableGameNum strengthInstance;
    final EffectDurationInstance durationInstance;
    final List<ConditionInstance> instanceConditions;
    final EffectChangeInstance referenceChange;

    final LivingObject owner;
    final EffectInstance changeFrom;
    final LivingObject user;
    final int effectInstanceID; // effectID's can conflict with living objects. They aren't living objects.
    final int timesUsed;
    final boolean isNegated;

    public boolean typeEquals(EffectObject other) {
        if (!(other instanceof EffectChangeInstance)) return false;
        return ((EffectChangeInstance) other).getTypeInstance() == typeInstance;
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
    public EffectChangeInstance getEffectChangeFrom() { return referenceChange; }
    public boolean isCost() { return this.getEffectFrom().getInstanceCost().stream().anyMatch(c -> c.getEffectID() == this.effectInstanceID); }

    public ImmutableEffectChangeInstance(EffectChangeInstance reference) {
        super(reference.getChangeConditions(), reference.getType(), reference.getTargetRequirements(), reference.getStrength(), reference.getDuration());
        this.referenceChange = reference;
        this.owner = reference.getOwner();
        this.changeFrom = reference.getEffectFrom();
        this.instanceConditions = new ArrayList<>();
        for (ConditionInstance condition : reference.getInstanceConditions()) {
            instanceConditions.add(new ConditionInstance(condition));
        }
        this.user = reference.getUser();
        this.effectInstanceID = reference.getEffectID();
        this.typeInstance = reference.getTypeInstance();
        this.targetRequirementsInstance = reference.getTargetInstance();
        this.strengthInstance = reference.getStrengthInstance();
        this.durationInstance = new EffectDurationInstance(reference.getDurationInstance());
        this.timesUsed = reference.getTimesUsedThisTurn();
        this.isNegated = reference.isNegated();
    }
}
