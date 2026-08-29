package ew.engine.resolver;

public enum VariableNumType {
    Num,

    XNum, // x or the previously chosen x
    EvenXNum, // set x, but the user must choose an even number
    OddXNum, // set x, but the user must choose an odd number
    XHalvedUp, // previous x halved rounded up
    XHalvedDown, // previous x halved rounded down

    QNum, // ?
    QHalvedUp, // previous ? halved rounded up
    QHalvedDown, // previous ? halved rounded down

    // Below are declared values which are saved in the card until being received by its respective type.
    DeclaredNum,
    DeclaredName,
    DeclaredAttribute,
    DeclaredPile,
    DeclaredDirection,

    // Below are types for derived values that may change automatically with the gamestate.
    BonusOnOpponentsSide,
    PerUniqueDamagedTarget, // for Piranha
    PerCardThisCardUses,
    MinCardThisCardUses,
    Precision,

    // Below MUST resolve during resolution, as it checks something that can only happen before an EffectChange (others can do both)
    ThisTargetsOriginalHealth,
}
