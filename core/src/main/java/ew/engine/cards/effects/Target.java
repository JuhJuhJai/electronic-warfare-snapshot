package ew.engine.cards.effects;

import ew.engine.resolver.VariableGameNum;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Abbreviation for the effect change type that gives a card a target.
 * Stipulations represent limitations on target choices.
 * Accepts a target pattern which is converted into stipulations.
 */
public final class Target extends EffectChange {

    private Target(EffectChangeType targetType, List<Condition> targetCondition, List<TargetType> stipulations, VariableGameNum numOfTargets, EffectDuration targetDuration) {
        super (targetCondition, targetType, stipulations, numOfTargets, targetDuration);
    }

    /** TargetType must be EffectChangeType UniqueTarget, MultiTarget, RandomTarget, or RandomMultiTarget. */
    public static Target of(EffectChangeType targetType, List<TargetType> stipulations, VariableGameNum numOfTargets) {
        switch (targetType) {
            case UniqueTarget, MultiTarget, RandomTarget, RandomMultiTarget -> {}
            default -> throw new IllegalArgumentException("Invalid targetType for target constructor (" + targetType + ")");
        }
        return new Target(EffectChangeType.UniqueTarget, List.of(), stipulations, numOfTargets, EffectDuration.EFFECT_END());
    }

    public static Target of(EffectChangeType targetType, TargetType stipulations, VariableGameNum numOfTargets) {
        return Target.of(targetType, List.of(stipulations), numOfTargets);
    }

    public static Target of(EffectChangeType targetType, List<TargetType> stipulations, int numOfTargets) {
        return Target.of(targetType, stipulations, VariableGameNum.Num(numOfTargets));
    }

    public static Target of(EffectChangeType targetType, TargetType stipulations, int numOfTargets) {
        return Target.of(targetType, List.of(stipulations), VariableGameNum.Num(numOfTargets));
    }

    // Targeting explicitly one card is a MultiTarget as effects could theoretically raise the number of targets, which wouldn't hold them to targeting different things.
    public static Target of(List<TargetType> stipulations) {
        return Target.of(EffectChangeType.MultiTarget, stipulations, VariableGameNum.ONE());
    }

    public static Target of(TargetType stipulations) {
        return Target.of(List.of(stipulations));
    }

    /** If you want a random target with multiple targets, or conditions, use "of" or "conditional" instead. */
    public static Target random(List<TargetType> stipulations) {
        return Target.of(EffectChangeType.RandomMultiTarget, stipulations, VariableGameNum.ONE());
    }

    /** If you want a random target with multiple targets, or conditions, use "of" or "conditional" instead. */
    public static Target random(TargetType stipulations) {
        return Target.random(List.of(stipulations));
    }

    /** TargetType must be EffectChangeType UniqueTarget, MultiTarget, RandomTarget, or RandomMultiTarget. */
    public static Target conditional(EffectChangeType targetType, List<Condition> targetCondition, List<TargetType> stipulations, VariableGameNum numOfTargets) {
        switch (targetType) {
            case UniqueTarget, MultiTarget, RandomTarget, RandomMultiTarget -> {}
            default -> throw new IllegalArgumentException("Invalid targetType for target constructor (" + targetType + ")");
        }
        return new Target(targetType, targetCondition, stipulations, numOfTargets, EffectDuration.EFFECT_END());
    }

    public static Target conditional(EffectChangeType targetType, Condition targetCondition, List<TargetType> stipulations, VariableGameNum numOfTargets) {
        return Target.conditional(targetType, List.of(targetCondition), stipulations, numOfTargets);
    }

    public static Target conditional(EffectChangeType targetType, Condition targetCondition, List<TargetType> stipulations, int numOfTargets) {
        return Target.conditional(targetType, List.of(targetCondition), stipulations, VariableGameNum.Num(numOfTargets));
    }

    public static Target conditional(EffectChangeType targetType, Condition targetCondition, TargetType stipulations, VariableGameNum numOfTargets) {
        return Target.conditional(targetType, List.of(targetCondition), List.of(stipulations), numOfTargets);
    }

    public static Target conditional(EffectChangeType targetType, Condition targetCondition, TargetType stipulations, int numOfTargets) {
        return Target.conditional(targetType, List.of(targetCondition), List.of(stipulations), VariableGameNum.Num(numOfTargets));
    }

    public static Target conditional(EffectChangeType targetType, List<Condition> targetCondition, List<TargetType> stipulations) {
        return Target.conditional(targetType, targetCondition, stipulations, VariableGameNum.ONE());
    }

    public static Target conditional(EffectChangeType targetType, Condition targetCondition, List<TargetType> stipulations) {
        return Target.conditional(targetType, List.of(targetCondition), stipulations);
    }

    public static Target conditional(EffectChangeType targetType, List<Condition> targetCondition, TargetType stipulations) {
        return Target.conditional(targetType, targetCondition, List.of(stipulations));
    }

    public static Target conditional(EffectChangeType targetType, Condition targetCondition, TargetType stipulations) {
        return Target.conditional(targetType, List.of(targetCondition), List.of(stipulations));
    }

    /** TargetType must be EffectChangeType UniqueTarget, MultiTarget, RandomTarget, or RandomMultiTarget. */
    public static Target pattern(EffectChangeType targetType, List<Condition> condition, List<TargetType> stipulations, Boolean[][] pattern, VariableGameNum numOfTargets) {
        switch (targetType) {
            case UniqueTarget, MultiTarget, RandomTarget, RandomMultiTarget -> {}
            default -> throw new IllegalArgumentException("Invalid targetType for target constructor (" + targetType + ")");
        }
        return new Target(targetType,
            condition,
            Stream.of(patternToStipulation(pattern), stipulations).flatMap(List::stream).toList(),
            numOfTargets,
            EffectDuration.EFFECT_END());
    }

    public static Target pattern(EffectChangeType targetType, Condition condition, List<TargetType> stipulations, VariableGameNum numOfTargets, Boolean[][] pattern) {
        return Target.pattern(targetType, List.of(condition), stipulations, pattern, numOfTargets);
    }

    public static Target pattern(EffectChangeType targetType, List<TargetType> stipulations, VariableGameNum numOfTargets, Boolean[][] pattern) {
        return Target.pattern(targetType, List.of(), stipulations, pattern, numOfTargets);
    }

    public static Target pattern(EffectChangeType targetType, TargetType stipulations,  VariableGameNum numOfTargets, Boolean[][] pattern) {
        return Target.pattern(targetType, List.of(stipulations), numOfTargets, pattern);
    }

    public static Target pattern(EffectChangeType targetType, List<TargetType> stipulations, int numOfTargets, Boolean[][] pattern) {
        return Target.pattern(targetType, List.of(), stipulations, pattern, VariableGameNum.Num(numOfTargets));
    }

    public static Target pattern(EffectChangeType targetType, TargetType stipulations, int numOfTargets, Boolean[][] pattern) {
        return Target.pattern(targetType, List.of(stipulations), VariableGameNum.Num(numOfTargets), pattern);
    }

    public static Target pattern(EffectChangeType targetType, VariableGameNum numOfTargets, Boolean[][] pattern) {
        return Target.pattern(targetType, List.of(), List.of(), pattern, numOfTargets);
    }

    public static Target pattern(EffectChangeType targetType, int numOfTargets, Boolean[][] pattern) {
        return Target.pattern(targetType, List.of(), VariableGameNum.Num(numOfTargets), pattern);
    }

    public static Target pattern(List<TargetType> stipulations, Boolean[][] pattern) {
        return Target.pattern(EffectChangeType.MultiTarget, stipulations, VariableGameNum.Num(1), pattern);
    }

    public static Target pattern(TargetType stipulations, Boolean[][] pattern) {
        return Target.pattern(List.of(stipulations), pattern);
    }

    public static Target pattern(Boolean[][] pattern) {
        return Target.pattern(List.of(), pattern);
    }

    /**
     * <p>Generates a list of target stipulations from a 7x7 boolean array that corresponds to the following list of zones</p>
     *
     *     Zone3f3l, Zone3f2l, Zone3f1l, Zone3f, Zone3f1r, Zone3f2r, Zone3f3r,
     *     Zone2f3l, Zone2f2l, Zone2f1l, Zone2f, Zone2f1r, Zone2f2r, Zone2f3r,
     *     Zone1f3l, Zone1f2l, Zone1f1l, Zone1f, Zone1f1r, Zone1f2r, Zone1f3r,
     *     Zone3l,   Zone2l,   Zone1l,  ZoneSelf, Zone1r,  Zone2r,   Zone3r,
     *     Zone1b3l, Zone1b2l, Zone1b1l, Zone1b, Zone1b1r, Zone1b2r, Zone1b3r,
     *     Zone2b3l, Zone2b2l, Zone2b1l, Zone2b, Zone2b1r, Zone2b2r, Zone2b3r,
     *     Zone3b3l, Zone3b2l, Zone3b1l, Zone3b, Zone3b1r, Zone3b2r, Zone3b3r
     */
    private static List<TargetType> patternToStipulation(Boolean[][] pattern) throws IllegalArgumentException {
        if (pattern.length != 7) throw new IllegalArgumentException("Pattern must be 7x7.");
        List<TargetType> stipulations = new ArrayList<>();
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                if (pattern[x][y]) switch(x) {
                    case 0 -> {
                        switch(y) {
                            case 0 -> stipulations.add(TargetType.Zone3f3l);
                            case 1 -> stipulations.add(TargetType.Zone3f2l);
                            case 2 -> stipulations.add(TargetType.Zone3f1l);
                            case 3 -> stipulations.add(TargetType.Zone3f);
                            case 4 -> stipulations.add(TargetType.Zone3f1r);
                            case 5 -> stipulations.add(TargetType.Zone3f2r);
                            case 6 -> stipulations.add(TargetType.Zone3f3r);
                        }
                    }
                    case 1 -> {
                        switch(y) {
                            case 0 -> stipulations.add(TargetType.Zone2f3l);
                            case 1 -> stipulations.add(TargetType.Zone2f2l);
                            case 2 -> stipulations.add(TargetType.Zone2f1l);
                            case 3 -> stipulations.add(TargetType.Zone2f);
                            case 4 -> stipulations.add(TargetType.Zone2f1r);
                            case 5 -> stipulations.add(TargetType.Zone2f2r);
                            case 6 -> stipulations.add(TargetType.Zone2f3r);
                        }
                    }
                    case 2 -> {
                        switch(y) {
                            case 0 -> stipulations.add(TargetType.Zone1f3l);
                            case 1 -> stipulations.add(TargetType.Zone1f2l);
                            case 2 -> stipulations.add(TargetType.Zone1f1l);
                            case 3 -> stipulations.add(TargetType.Zone1f);
                            case 4 -> stipulations.add(TargetType.Zone1f1r);
                            case 5 -> stipulations.add(TargetType.Zone1f2r);
                            case 6 -> stipulations.add(TargetType.Zone1f3r);
                        }
                    }
                    case 3 -> {
                        switch(y) {
                            case 0 -> stipulations.add(TargetType.Zone3l);
                            case 1 -> stipulations.add(TargetType.Zone2l);
                            case 2 -> stipulations.add(TargetType.Zone1l);
                            case 3 -> stipulations.add(TargetType.ZoneSelf);
                            case 4 -> stipulations.add(TargetType.Zone1r);
                            case 5 -> stipulations.add(TargetType.Zone2r);
                            case 6 -> stipulations.add(TargetType.Zone3r);
                        }
                    }
                    case 4 -> {
                        switch(y) {
                            case 0 -> stipulations.add(TargetType.Zone1b3l);
                            case 1 -> stipulations.add(TargetType.Zone1b2l);
                            case 2 -> stipulations.add(TargetType.Zone1b1l);
                            case 3 -> stipulations.add(TargetType.Zone1b);
                            case 4 -> stipulations.add(TargetType.Zone1b1r);
                            case 5 -> stipulations.add(TargetType.Zone1b2r);
                            case 6 -> stipulations.add(TargetType.Zone1b3r);
                        }
                    }
                    case 5 -> {
                        switch(y) {
                            case 0 -> stipulations.add(TargetType.Zone2b3l);
                            case 1 -> stipulations.add(TargetType.Zone2b2l);
                            case 2 -> stipulations.add(TargetType.Zone2b1l);
                            case 3 -> stipulations.add(TargetType.Zone2b);
                            case 4 -> stipulations.add(TargetType.Zone2b1r);
                            case 5 -> stipulations.add(TargetType.Zone2b2r);
                            case 6 -> stipulations.add(TargetType.Zone2b3r);
                        }
                    }
                    case 6 -> {
                        switch(y) {
                            case 0 -> stipulations.add(TargetType.Zone3b3l);
                            case 1 -> stipulations.add(TargetType.Zone3b2l);
                            case 2 -> stipulations.add(TargetType.Zone3b1l);
                            case 3 -> stipulations.add(TargetType.Zone3b);
                            case 4 -> stipulations.add(TargetType.Zone3b1r);
                            case 5 -> stipulations.add(TargetType.Zone3b2r);
                            case 6 -> stipulations.add(TargetType.Zone3b3r);
                        }
                    }
                }
            }
        }
        return stipulations;
    }
}
