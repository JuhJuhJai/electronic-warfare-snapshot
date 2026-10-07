package ew.engine.board;

import ew.engine.cards.effects.Condition;

import java.util.ArrayList;
import java.util.List;

public interface EffectObject extends ImmutableEffectObject {
    void setInstanceConditions(List<ConditionInstance> conditions);
    void tickTimesUsed();
    void setTimesUsedThisTurn(int timesUsedThisTurn);
    void setNegation(boolean negated);
    void setUser(LivingObject user);
}
