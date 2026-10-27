package ew.engine.board.effectInstance;

import ew.engine.board.LivingObject;

import java.util.List;

public interface ImmutableEffectObject {
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
    boolean isUsableBetweenEffectsInstance();
    boolean isUsableWhileShroudedInstance();
}
