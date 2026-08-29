package ew.engine.cards.effects;

import ew.engine.resolver.VariableGameNum;

import java.util.ArrayList;
import java.util.List;

/**
 * Conditions (along with the effect's type) are what determine when an effect can be used.
 * This is an information class.
 */
public class Condition {
    final List<Condition> conditions;
    final ConditionType type;
    final List<TargetType> targetType; // Multiple targets indicates more stipulations checked cards must meet
    final VariableGameNum check;

    public List<Condition> getCheckConditions() {
        return conditions;
    }
    public ConditionType getType() {
        return type;
    }
    public List<TargetType> getTarget() {
        return targetType;
    }
    public VariableGameNum getCheck() {
        return check;
    }

    protected Condition(List<Condition> checkConditions, ConditionType type, List<TargetType> targetType, VariableGameNum check) {
        this.conditions = checkConditions;
        this.type = type;
        this.targetType = targetType;
        this.check = check;
    }

    public static Condition inPosition(List<TargetType> usableState) {
        Condition condition = new Condition(List.of(), ConditionType.MinThings, new ArrayList<>(List.of(TargetType.Self)), VariableGameNum.Num(1));
        condition.getTarget().addAll(usableState);
        return condition;
    }

    public static Condition inPosition(TargetType usableState) {
        return inPosition(List.of(usableState));
    }

    public static Condition UsesPerTurn(int perTurn) {
        return new Condition(List.of(), ConditionType.MaxEffectPerTurn, List.of(TargetType.Self), VariableGameNum.Num(perTurn));
    }

    public static Condition Exclusive() {
        return new Condition(List.of(), ConditionType.Exclusive, List.of(TargetType.Self), VariableGameNum.ONE());
    }

    public static Condition Exclusive(int perTurn) {
        return new Condition(List.of(), ConditionType.Exclusive, List.of(TargetType.Self), VariableGameNum.Num(perTurn));
    }

    /** Use only for effectChange condition. */
    public static Condition andIfYouDo() {
        return new Condition(List.of(), ConditionType.IfLastChangeResolved, List.of(TargetType.Self), VariableGameNum.ONE());
    }

    public static Condition Timing(ConditionType timingPoint) {
        return new Condition(List.of(), timingPoint, List.of(TargetType.Gamestate), VariableGameNum.ZERO());
    }

    public static Condition of(ConditionType type, List<TargetType> targetType, VariableGameNum check) {
        return new Condition(List.of(), type, targetType, check);
    }

    public static Condition of(ConditionType type, List<TargetType> targetType, int check) {
        return of(type, targetType, VariableGameNum.Num(check));
    }

    public static Condition of(ConditionType type, TargetType targetType, VariableGameNum check) {
        return of(type, List.of(targetType), check);
    }

    public static Condition of(ConditionType type, TargetType targetType, int check) {
        return of(type, List.of(targetType), VariableGameNum.Num(check));
    }

    public static Condition conditional(List<Condition> checkConditions, ConditionType type, List<TargetType> targetType, VariableGameNum check) {
        return new Condition(checkConditions, type, targetType, check);
    }

    public static Condition conditional(List<Condition> checkConditions, ConditionType type, List<TargetType> targetType, int check) {
        return conditional(checkConditions, type, targetType, VariableGameNum.Num(check));
    }

    public static Condition conditional(List<Condition> checkConditions, ConditionType type, TargetType targetType, VariableGameNum check) {
        return conditional(checkConditions, type, List.of(targetType), check);
    }

    public static Condition conditional(List<Condition> checkConditions, ConditionType type, TargetType targetType, int check) {
        return conditional(checkConditions, type, List.of(targetType), VariableGameNum.Num(check));
    }

    public static Condition conditional(Condition checkCondition, ConditionType type, List<TargetType> targetType, VariableGameNum check) {
        return new Condition(List.of(checkCondition), type, targetType, check);
    }

    public static Condition conditional(Condition checkConditions, ConditionType type, List<TargetType> targetType, int check) {
        return conditional(checkConditions, type, targetType, VariableGameNum.Num(check));
    }

    public static Condition conditional(Condition checkCondition, ConditionType type, TargetType targetType, VariableGameNum check) {
        return conditional(checkCondition, type, List.of(targetType), check);
    }

    public static Condition conditional(Condition checkCondition, ConditionType type, TargetType targetType, int check) {
        return conditional(checkCondition, type, List.of(targetType), VariableGameNum.Num(check));
    }

    public static Condition OPTION1() {
        return of(ConditionType.Choice, TargetType.Self, 1);
    }

    public static Condition OPTION2() {
        return of(ConditionType.Choice, TargetType.Self, 2);
    }

    public static Condition OPTION3() {
        return of(ConditionType.Choice, TargetType.Self, 3);
    }

    public static Condition OPTION4() {
        return of(ConditionType.Choice, TargetType.Self, 4);
    }

    public static Condition OPTION5() {
        return of(ConditionType.Choice, TargetType.Self, 5);
    }

    public static Condition OPTION6() {
        return of(ConditionType.Choice, TargetType.Self, 6);
    }
}
