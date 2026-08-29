package ew.engine.resolver;

import ew.engine.board.EffectInstance;
import ew.engine.board.LivingObject;

import java.util.List;

/** Class used to communicate choices to the commander. They can choose to use the effect on one of the options.  */
public class GameChoice {
    final EffectInstance effect;
    final List<LivingObject> initialOptions; // Holds possible target options for display. If null, the commander doesn't need to choose a target.

    public EffectInstance getEffect() { return effect; }
    public List<LivingObject> getInitialOptions() { return initialOptions; }

    public GameChoice(EffectInstance effect, List<LivingObject> initialOptions) {
        this.effect = effect;
        this.initialOptions = initialOptions;
    }

    public static GameChoice NOT_USED() {
        return new GameChoice(null, null);
    }

    public boolean equals(Object other) {
        if (!(other instanceof GameChoice o)) return false;
        return o.getEffect() == effect && o.getInitialOptions() == initialOptions;
    }
}
