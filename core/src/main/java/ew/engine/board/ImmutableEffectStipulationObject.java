package ew.engine.board;

import ew.engine.cards.effects.TargetType;
import ew.engine.resolver.VariableGameNum;

import java.util.List;

public interface ImmutableEffectStipulationObject extends ImmutableEffectObject {
    List<TargetType> getTargetInstance();
    VariableGameNum getStrengthInstance();
    EffectChangeInstance getEffectChangeFrom();
    /** Returns whether the object is in the "Cost" of its effectFrom. */
    boolean isCost();
}
