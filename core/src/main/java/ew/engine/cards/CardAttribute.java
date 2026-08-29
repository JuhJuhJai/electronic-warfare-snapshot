package ew.engine.cards;

public enum CardAttribute {
    None,
    Human,
    Machine,
    Rock,
    Plant,
    Wind,
    Water,
    Flame,
    Beast,
    Divine,
    Ghoul,
    Ether,
    Ritual;

    public boolean isCopyable() { return this.ordinal() > 0; }

    public int toDeclared() { return this.ordinal(); }
}
