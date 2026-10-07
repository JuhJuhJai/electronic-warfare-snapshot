package ew.engine.board;

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
}
