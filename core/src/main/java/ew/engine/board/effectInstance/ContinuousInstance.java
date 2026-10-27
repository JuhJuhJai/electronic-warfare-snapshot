package ew.engine.board.effectInstance;

import ew.engine.board.LivingObject;

import java.util.List;

/// This class is for continuous AND used for effects that have duration.
public class ContinuousInstance {
    final ImmutableEffectChangeInstance effect;
    /// May be null. This means the effect targets every card mentioned in it's effectChangeInstance.
    final List<LivingObject> target;
    final List<Integer> unwindInformation;

    public ImmutableEffectChangeInstance effect() { return effect; }
    public List<LivingObject> target() { return target; }
    public List<Integer> info() { return unwindInformation; }

    public ContinuousInstance(EffectChangeInstance effect, List<LivingObject> target, List<Integer> info) {
        this.effect = new ImmutableEffectChangeInstance(effect);
        this.target = target;
        this.unwindInformation = info;
    }
}
