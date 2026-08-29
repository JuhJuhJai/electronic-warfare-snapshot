package ew.engine.cards.effects;

public enum EffectType {
    PlaceCost, ByEffectPlaceCost, UsingCardsPlaceCost,
    Unshroud,
    Placed,
    ConstantContinuous, // A continuous that ALWAYS applies (e.g. Your attacks gain 1 damage.) ("faster" than optional continuous.)
    OptionalContinuous, // A continuous that CAN be applied (e.g. When a card attacks, you *can* deal 1 damage to this card to have that attack gain 1 damage.)
    Trigger,
    Activatable,
    Action
}
