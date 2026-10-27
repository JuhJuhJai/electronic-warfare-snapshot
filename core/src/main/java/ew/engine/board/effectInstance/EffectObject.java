package ew.engine.board.effectInstance;

import ew.engine.board.LivingObject;

import java.util.List;

public interface EffectObject extends ImmutableEffectObject {
    void setInstanceConditions(List<ConditionInstance> conditions);
    void incrementTimesUsed();
    void setTimesUsedThisTurn(int timesUsedThisTurn);
    void setNegation(boolean negated);
    void setUser(LivingObject user);
}
