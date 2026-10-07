package ew.engine.board;

import ew.engine.cards.effects.TargetType;
import ew.engine.resolver.VariableGameNum;

import java.util.List;

public interface EffectStipulationObject extends EffectObject, ImmutableEffectStipulationObject {
    void setTargetInstance(List<TargetType> newTargetReqs);
    void setStrengthInstance(VariableGameNum newStrength);
}
