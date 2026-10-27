package ew.engine.cards.effects;

import ew.engine.resolver.VariableGameNum;

import java.util.List;

public class EffectDuration {
    /// The number of a ticks before a duration ends.
    final VariableGameNum tickNum;
    /// Condition that determines when the duration ticks.
    final List<Condition> ticksOn;
    /// Condition that must be true during the duration of the effect - while false, the duration is also false, but doesn't end.
    /// E.g. "Until the end of the phase, while you control this card -> ..."
    final List<Condition> durationCondition;

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(tickNum.toString()).append(" ticks remaining, ticks on: ");
        for (int i = 0 ; i < ticksOn.size(); i++) {
            builder.append(ticksOn.get(i).toString());
            if (i < ticksOn.size() - 1) builder.append(" && ");
        }
        if (!durationCondition.isEmpty()) {
            builder.append(", ends early if ");
            for (int i = 0; i < durationCondition.size(); i++) {
                builder.append(durationCondition.get(i).toString());
                if (i < durationCondition.size() - 1) builder.append(" && ");
            }
        }
        return builder.toString();
    }

    public VariableGameNum getStrength() { return tickNum; }
    public List<Condition> getTicksOn() { return ticksOn; }
    public List<Condition> getDurationCondition() { return durationCondition; }

    public static EffectDuration INSTANT() {
        // Always ticks, tick condition is always true. Also, at zero strength, duration ends anyways.
        return new EffectDuration(List.of(), Condition.TRUE(), VariableGameNum.ZERO());
    }

    public static EffectDuration FOREVER() {
        // Never ticks, tick condition is always false
        return new EffectDuration(List.of(), Condition.FALSE(), VariableGameNum.ONE());
    }

    public static EffectDuration EFFECT_END() {
        return new EffectDuration(List.of(), Condition.of(ConditionType.EffectEnds, TargetType.Gamestate, 1), VariableGameNum.ONE());
    }

    public static EffectDuration PHASE_END() {
        return new EffectDuration(List.of(), Condition.of(ConditionType.PhaseEnds, TargetType.Gamestate, 1), VariableGameNum.ONE());
    }

    public static EffectDuration END_AFTERMATH() {
        return new EffectDuration(List.of(), Condition.of(ConditionType.AftermathPhaseEnds, TargetType.Gamestate, 1), VariableGameNum.ONE());
    }

    public EffectDuration(Condition ticksOn, int tickNum) {
        this.ticksOn = List.of(ticksOn);
        this.tickNum = VariableGameNum.Num(tickNum);
        this.durationCondition = List.of();
    }

    public EffectDuration(Condition ticksOn, VariableGameNum tickNum) {
        this.ticksOn = List.of(ticksOn);
        this.tickNum = tickNum;
        this.durationCondition = List.of();
    }

    public EffectDuration(List<Condition> durationCondition, Condition ticksOn, int tickNum) {
        this.ticksOn = List.of(ticksOn);
        this.tickNum = VariableGameNum.Num(tickNum);
        this.durationCondition = durationCondition;
    }

    public EffectDuration(List<Condition> durationCondition, Condition ticksOn, VariableGameNum tickNum) {
        this.ticksOn = List.of(ticksOn);
        this.tickNum = tickNum;
        this.durationCondition = durationCondition;
    }

    public EffectDuration(List<Condition> ticksOn, int tickNum) {
        this.ticksOn = ticksOn;
        this.tickNum = VariableGameNum.Num(tickNum);
        this.durationCondition = List.of();
    }

    public EffectDuration(List<Condition> ticksOn, VariableGameNum tickNum) {
        this.ticksOn = ticksOn;
        this.tickNum = tickNum;
        this.durationCondition = List.of();
    }

    public EffectDuration(List<Condition> durationCondition, List<Condition> ticksOn, int tickNum) {
        this.ticksOn = ticksOn;
        this.tickNum = VariableGameNum.Num(tickNum);
        this.durationCondition = durationCondition;
    }

    public EffectDuration(List<Condition> durationCondition, List<Condition> ticksOn, VariableGameNum tickNum) {
        this.ticksOn = ticksOn;
        this.tickNum = tickNum;
        this.durationCondition = durationCondition;
    }

}
