package ew.engine.resolver;

import ew.engine.board.EffectChangeInstance;
import ew.engine.board.LivingObject;

import java.util.List;

/** Class used to communicate the available options for the GameChoice they made */
public class GameChoiceChange {
    final EffectChangeInstance effect;
    final List<LivingObject> target; // May be null.
    final List<Integer> choiceInfo; // use changes based on the effect type of the EffectInstance. May be null.

    public EffectChangeInstance getChange() { return effect; }
    public List<LivingObject> getTarget() { return target; }
    public List<Integer> getChoiceInfo() { return choiceInfo; }

    public GameChoiceChange(EffectChangeInstance effect, List<LivingObject> targets, List<Integer> choiceInfo) {
        this.effect = effect;
        this.target = targets;
        this.choiceInfo = choiceInfo;
    }

    public GameChoiceChange(EffectChangeInstance effect, LivingObject target, List<Integer> choiceInfo) {
        this.effect = effect;
        this.target = List.of(target);
        this.choiceInfo = choiceInfo;
    }

    public static GameChoiceChange NOT_USED() {
        return new GameChoiceChange(null, (List<LivingObject>) null, null);
    }

    public static GameChoiceChange NO_INFO(EffectChangeInstance effect) { return new GameChoiceChange(effect, List.of(), List.of()); }

    public boolean equals(Object other) {
        if (!(other instanceof GameChoiceChange o)) return false;
        return o.getChange() == effect && o.getTarget() == target && o.getChoiceInfo() == choiceInfo;
    }
}
