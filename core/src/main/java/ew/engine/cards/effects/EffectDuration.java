package ew.engine.cards.effects;

import ew.engine.resolver.VariableGameNum;

import java.util.List;

public class EffectDuration {
    final VariableGameNum tickNum;
    final ConditionType type;
    // Condition that must be true during the duration of the effect - while false, the duration is also false, but doesn't end.
    // E.g. "Until the end of the phase, while you control this card -> ..."
    final List<Condition> durationCondition;

    public VariableGameNum getStrength() { return tickNum; }
    public ConditionType getTicksOn() { return type; }
    public List<Condition> getDurationCondition() { return durationCondition; }

    public static EffectDuration INSTANT() {
        return new EffectDuration(List.of(), ConditionType.NONE, VariableGameNum.ZERO());
    }

    public static EffectDuration FOREVER() {
        return new EffectDuration(List.of(), ConditionType.NONE, VariableGameNum.ONE());
    }

    public static EffectDuration EFFECT_END() {
        return new EffectDuration(List.of(), ConditionType.EffectEnds, VariableGameNum.ONE());
    }

    public static EffectDuration PHASE_END() {
        return new EffectDuration(List.of(), ConditionType.PhaseEnds, VariableGameNum.ONE());
    }

    public EffectDuration(ConditionType ticksOn, int tickNum) {
        this.type = ticksOn;
        this.tickNum = VariableGameNum.Num(tickNum);
        this.durationCondition = List.of();
    }

    public EffectDuration(ConditionType ticksOn, VariableGameNum tickNum) {
        this.type = ticksOn;
        this.tickNum = tickNum;
        this.durationCondition = List.of();
    }

    public EffectDuration(List<Condition> durationCondition, ConditionType ticksOn, int tickNum) {
        this.type = ticksOn;
        this.tickNum = VariableGameNum.Num(tickNum);
        this.durationCondition = durationCondition;
    }

    public EffectDuration(List<Condition> durationCondition, ConditionType ticksOn, VariableGameNum tickNum) {
        this.type = ticksOn;
        this.tickNum = tickNum;
        this.durationCondition = durationCondition;
    }
}
