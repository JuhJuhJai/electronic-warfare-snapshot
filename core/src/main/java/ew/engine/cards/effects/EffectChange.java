package ew.engine.cards.effects;

import ew.engine.resolver.VariableGameNum;

import java.util.List;
import java.util.Objects;

public class EffectChange {
    final EffectChangeType type;
    final List<TargetType> stipulations;
    final VariableGameNum strength;
    final List<Condition> changeCondition;
    final EffectDuration duration;

    public List<Condition> getChangeConditions() { return changeCondition; }
    public EffectChangeType getType() { return type; }
    public List<TargetType> getTargetRequirements() { return stipulations; }
    public VariableGameNum getStrength() { return strength; }
    public EffectDuration getDuration() { return duration; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EffectChange other)) return false;
        return this.type == other.type
            && this.stipulations == other.stipulations
            && this.strength == other.strength
            && this.duration == other.duration
            && this.changeCondition == other.changeCondition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, stipulations, stipulations, duration, changeCondition);
    }

    protected EffectChange(List<Condition> changeCondition, EffectChangeType type, List<TargetType> stipulations, VariableGameNum strength, EffectDuration duration) {
        this.changeCondition = changeCondition;
        this.type = type;
        this.stipulations = stipulations;
        this.strength = strength;
        this.duration = duration;
    }

    public static EffectChange system(EffectChangeType type) {
        return new EffectChange(List.of(), type, List.of(TargetType.Gamestate), VariableGameNum.ZERO(), EffectDuration.INSTANT());
    }

    public static EffectChange choose(int choices) {
        return new EffectChange(List.of(), EffectChangeType.Choose, List.of(), VariableGameNum.Num(choices), EffectDuration.INSTANT());
    }

    public static EffectChange chooseRandom(int choices) {
        return new EffectChange(List.of(), EffectChangeType.RandomChoose, List.of(), VariableGameNum.Num(choices), EffectDuration.INSTANT());
    }

    public static EffectChange of(EffectChangeType type, List<TargetType> targets, VariableGameNum strength) {
        return new EffectChange(List.of(), type, targets, strength, EffectDuration.INSTANT());
    }

    public static EffectChange of(EffectChangeType type, List<TargetType> targets, int strength) {
        return of(type, targets, VariableGameNum.Num(strength));
    }

    public static EffectChange of(EffectChangeType type, TargetType target, VariableGameNum strength) {
        return of(type, List.of(target), strength);
    }

    public static EffectChange of(EffectChangeType type, TargetType target, int strength) {
        return of(type, List.of(target), VariableGameNum.Num(strength));
    }

    /** Abbreviation that sets strength to 1. Still normally specify a strength of 1 unless the type is clear. (e.g. destroy) */
    public static EffectChange of(EffectChangeType type, List<TargetType> targets) {
        return of(type, targets, VariableGameNum.Num(1));
    }

    /** Abbreviation that sets strength to 1. Still normally specify a strength of 1 unless the type is clear. (e.g. destroy) */
    public static EffectChange of(EffectChangeType type, TargetType target) {
        return of(type, List.of(target), 1);
    }


    public static EffectChange duration(EffectChangeType type, List<TargetType> targets, VariableGameNum strength, EffectDuration duration) {
        return new EffectChange(List.of(), type, targets, strength, duration);
    }

    public static EffectChange duration(EffectChangeType type, List<TargetType> targets, int strength, EffectDuration duration) {
        return duration(type, targets, VariableGameNum.Num(strength), duration);
    }

    public static EffectChange duration(EffectChangeType type, TargetType target, VariableGameNum strength, EffectDuration duration) {
        return duration(type, List.of(target), strength, duration);
    }

    public static EffectChange duration(EffectChangeType type, TargetType target, int strength, EffectDuration duration) {
        return duration(type, List.of(target), VariableGameNum.Num(strength), duration);
    }


    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, List<TargetType> targets, VariableGameNum strength) {
        return new EffectChange(conditions, type, targets, strength, EffectDuration.INSTANT());
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, List<TargetType> targets, int strength) {
        return conditional(conditions, type, targets, VariableGameNum.Num(strength));
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, TargetType target, VariableGameNum strength) {
        return conditional(conditions, type, List.of(target), strength);
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, TargetType target, int strength) {
        return conditional(conditions, type, List.of(target), VariableGameNum.Num(strength));
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, List<TargetType> targets, VariableGameNum strength, EffectDuration duration) {
        return new EffectChange(conditions, type, targets, strength, duration);
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, List<TargetType> targets, int strength, EffectDuration duration) {
        return conditional(conditions, type, targets, VariableGameNum.Num(strength), duration);
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, TargetType target, VariableGameNum strength, EffectDuration duration) {
        return conditional(conditions, type, List.of(target), strength, duration);
    }

    public static EffectChange conditional(List<Condition> conditions, EffectChangeType type, TargetType target, int strength, EffectDuration duration) {
        return conditional(conditions, type, List.of(target), VariableGameNum.Num(strength), duration);
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, List<TargetType> targets, VariableGameNum strength) {
        return new EffectChange(List.of(condition), type, targets, strength, EffectDuration.INSTANT());
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, List<TargetType> targets, int strength) {
        return conditional(condition, type, targets, VariableGameNum.Num(strength));
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, TargetType target, VariableGameNum strength) {
        return conditional(condition, type, List.of(target), strength);
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, TargetType target, int strength) {
        return conditional(condition, type, List.of(target), VariableGameNum.Num(strength));
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, List<TargetType> targets, VariableGameNum strength, EffectDuration duration) {
        return new EffectChange(List.of(condition), type, targets, strength, duration);
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, List<TargetType> targets, int strength, EffectDuration duration) {
        return conditional(condition, type, targets, VariableGameNum.Num(strength), duration);
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, TargetType target, VariableGameNum strength, EffectDuration duration) {
        return conditional(condition, type, List.of(target), strength, duration);
    }

    public static EffectChange conditional(Condition condition, EffectChangeType type, TargetType target, int strength, EffectDuration duration) {
        return conditional(condition, type, List.of(target), VariableGameNum.Num(strength), duration);
    }

}
