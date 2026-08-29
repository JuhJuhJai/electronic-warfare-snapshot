package ew.engine.board;

import ew.engine.cards.effects.Condition;

import java.util.ArrayList;
import java.util.List;

public interface EffectObject {
    boolean equals(Object other);
    boolean typeEquals(EffectObject other);
    int hashCode();
    String toString();

    EffectInstance getEffectFrom();
    LivingObject getOwner();
    LivingObject getUser();
    List<ConditionInstance> getInstanceConditions();
    int getEffectID();
    int getTimesUsedThisTurn();
    boolean isNegated();

    void setInstanceConditions(List<ConditionInstance> conditions);
    void tickTimesUsed();
    void setTimesUsedThisTurn(int timesUsedThisTurn);
    void setNegation(boolean negated);
    void setUser(LivingObject user);
}
