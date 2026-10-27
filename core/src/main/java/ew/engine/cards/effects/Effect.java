package ew.engine.cards.effects;

import ew.engine.cards.CardType;
import ew.engine.resolver.VariableGameNum;
import ew.engine.resolver.VariableNumType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Effect {
    final List<Condition> conditions;
    final List<EffectChange> cost;
    final List<EffectChange> effect;
    public final EffectType type;
    final String effectText;
    final boolean usableWhileShrouded;
    final boolean usableBetweenEffects;

    public List<Condition> getConditions() { return conditions; }
    public List<EffectChange> getCostChanges() { return cost; }
    public List<EffectChange> getEffectChanges() { return effect; }
    public EffectType getType() { return type; }
    public String getEffectText() { return effectText; }
    public boolean isUsableWhileShrouded() { return usableWhileShrouded; }
    public boolean isUsableBetweenEffects() { return usableBetweenEffects; }

    protected Effect(String effectText,
                   boolean usableWhileShrouded,
                   boolean usableBetweenEffects,
                   EffectType type,
                   List<Condition> conditions,
                   List<EffectChange> cost,
                   List<EffectChange> effect) {
        this.effectText = effectText;
        this.usableWhileShrouded = usableWhileShrouded;
        this.usableBetweenEffects = usableBetweenEffects;
        this.type = type;
        this.conditions = conditions;
        this.cost = cost;
        this.effect = effect;
    }

    // There's a world where there's placeCost with multiple effects,
    // and where usableWhileShrouded is true (meaning it actually does occur on a shrouded placement)
    /** The selfPlacementEffect must be added manually. */
    public static Effect PLACE_COST(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        if (effect.stream().noneMatch(e -> e.getType() == EffectChangeType.selfPlacementEffect))
            throw new IllegalArgumentException("Place cost (" + effectText + ") doesn't have the selfPlacementEffect as an effect.");
        return new Effect(effectText, usableWhileShrouded, true, EffectType.PlaceCost, conditions, cost, effect);
    }

    /** The selfPlacementEffect must be added manually. */
    public static Effect PLACE_COST(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost) {
        return PLACE_COST(effectText, usableWhileShrouded, conditions, cost, List.of());
    }

    /** The selfPlacementEffect must be added manually. */
    public static Effect PLACE_COST(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return PLACE_COST(effectText, false, conditions, cost, effect);
    }

    /** The selfPlacementEffect must be added manually. */
    public static Effect PLACE_COST(String effectText, List<EffectChange> cost, List<EffectChange> effect) {
        return PLACE_COST(effectText, false, List.of(), cost, effect);
    }

    /** Automatically adds the selfPlacementEffect, based on the given CardType. */
    public static Effect PLACE_COST(CardType type, String effectText, List<Condition> conditions, List<EffectChange> cost) {
        return PLACE_COST(effectText, conditions, cost, List.of(
            switch(type.getType()) {
                case 1 -> EffectChange.of(EffectChangeType.selfPlacementEffect, List.of(TargetType.OnlyAttackZonesOnYourSide), 1);
                case 2 -> EffectChange.of(EffectChangeType.selfPlacementEffect, List.of(TargetType.OnlyProductionZonesOnYourSide), 1);
                default -> throw new IllegalArgumentException(type + " is not valid CardType for PLACE_COST.");
            }
        ));
    }

    /** Automatically adds the selfPlacementEffect, based on the given CardType. */
    public static Effect PLACE_COST(CardType type, String effectText, List<EffectChange> cost) {
        return PLACE_COST(type, effectText, List.of(), cost);
    }

    /** Abbreviation for PLACE_COST that generates effect text. */
    public static Effect BASIC_MATERIAL(CardType type, int placeMaterialCost) {
        if (!(type == CardType.ATTACK || type == CardType.PRODUCTION)) throw new IllegalArgumentException("Invalid card type (" + type + ") for Basic Material Cost.");
        return PLACE_COST(type, placeMaterialCost + " Material",
            List.of(
                EffectChange.of(EffectChangeType.PayMaterial, TargetType.UsingCommander, placeMaterialCost)
            )
        );
    }

    /** A token has no cost, usually. The cost, still, if any, is paid in the effect that makes the token. */
    public static Effect TOKEN_COST() {
        return PLACE_COST("", List.of(), List.of());
    }

    /**
     * Abbreviation for PLACE_COST designed for cards that must be placed by an effect which uses cards
     * and follow the multiple of 3 rule for placement cost. (Displace cards until end of next
     * aftermath whose material cost totals [x] material, then pay [x / 3] material per card used).
     */
    public static Effect RITUAL_COST(CardType type, int materialCostTotal) {
        if (!(type == CardType.ATTACK || type == CardType.PRODUCTION)) throw new IllegalArgumentException("Invalid card type (" + type + ") for Ritual Place Cost.");
        final int paymentPerCard = (materialCostTotal + 1) / 3; // +1 for proper rounding on rituals without an exact multiple of 3 for total cost
        return PLACE_COST(type, "By an effect that uses cards, displace cards until the end of the next Aftermath Phase " +
            "whose material cost totals " + materialCostTotal + " material, then pay " + paymentPerCard + " for each card used.",
            new ArrayList<EffectChange>(List.of(
                EffectChange.duration(EffectChangeType.DisplaceMaterial, List.of(TargetType.CurrentlyUsable, TargetType.CardsNotDisplaced),
                    materialCostTotal, new EffectDuration(Condition.of(ConditionType.AftermathPhaseEnds, TargetType.Gamestate, 1), 2)),
                EffectChange.of(EffectChangeType.PayMaterial, TargetType.UsingCommander,
                    VariableGameNum.Variable(VariableNumType.PerCardThisCardUses, paymentPerCard))
            ))
        );
    }

    // Unshroud's condition to be used when the card is unshrouded isn't controlled by the card, it always happens when the card unshrouds. Handled by the resolver.
    public static Effect UNSHROUD(String effectText, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, false, false, EffectType.Unshroud, List.of(), cost, effect);
    }

    public static Effect UNSHROUD(String effectText, List<EffectChange> effect) {
        return UNSHROUD(effectText, List.of(), effect);
    }

    public static Effect UNSHROUD(String effectText, List<EffectChange> cost, EffectChange effect) {
        return UNSHROUD(effectText, cost, List.of(effect));
    }

    public static Effect UNSHROUD(String effectText, EffectChange effect) {
        return UNSHROUD(effectText, List.of(), List.of(effect));
    }


    // Placed ALWAYS must remain on the field before it resolves.
    public static Effect PLACED(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, false, false, EffectType.Placed, Stream.of(List.of(Condition.inPosition(TargetType.CardsOnTheField)), conditions).flatMap(List::stream).toList(), cost, effect);
    }

    public static Effect PLACED(String effectText, List<EffectChange> cost, List<EffectChange> effect) {
        return PLACED(effectText, List.of(), cost, effect);
    }

    public static Effect PLACED(String effectText, List<EffectChange> effect) {
        return PLACED(effectText, List.of(), List.of(), effect);
    }


    /// A Strategic trigger may have its condition met between effects and resolve then. Normal triggers may not.
    public static Effect TRIGGER_STRATEGIC(String effectText, boolean usableWhileShrouded, boolean usableBetweenEffects, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, usableWhileShrouded, usableBetweenEffects, EffectType.Trigger, conditions, cost, effect);
    }

    public static Effect TRIGGER(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return TRIGGER_STRATEGIC(effectText, usableWhileShrouded, false, conditions, cost, effect);
    }

    public static Effect TRIGGER(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return TRIGGER(effectText, false, conditions, cost, effect);
    }

    public static Effect FIELD_TRIGGER(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return TRIGGER(effectText, usableWhileShrouded, Stream.concat(Stream.of(Condition.inPosition(TargetType.CardsOnTheField)), conditions.stream()).toList(), cost, effect);
    }

    public static Effect FIELD_TRIGGER(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return FIELD_TRIGGER(effectText, false, conditions, cost, effect);
    }

    public static Effect FIELD_TRIGGER(String effectText, List<Condition> conditions, List<EffectChange> effect) {
        return FIELD_TRIGGER(effectText, false, conditions, List.of(), effect);
    }


    public static Effect ACTIVATABLE(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, usableWhileShrouded, false, EffectType.Activatable, conditions, cost, effect);
    }

    public static Effect ACTIVATABLE(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return ACTIVATABLE(effectText, false, conditions, cost, effect);
    }

    public static Effect ACTIVATABLE(String effectText, List<Condition> conditions, List<EffectChange> effect) {
        return ACTIVATABLE(effectText, conditions, List.of(), effect);
    }

    public static Effect FIELD_ACTIVATABLE(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return ACTIVATABLE(effectText, usableWhileShrouded, Stream.concat(Stream.of(Condition.inPosition(TargetType.CardsOnTheField)), conditions.stream()).toList(), cost, effect);
    }

    public static Effect FIELD_ACTIVATABLE(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return FIELD_ACTIVATABLE(effectText, false, conditions, cost, effect);
    }

    public static Effect FIELD_ACTIVATABLE(String effectText, List<EffectChange> cost, List<EffectChange> effect) {
        return FIELD_ACTIVATABLE(effectText, List.of(), cost, effect);
    }


    public static Effect ACTION(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, usableWhileShrouded, false, EffectType.Action, conditions, cost, effect);
    }

    public static Effect ACTION(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return Effect.ACTION(effectText, false, conditions, cost, effect);
    }

    // Actions don't always need conditions as the resolver already checks for the base condition of it being the combat phase and their lane being selected, while the card is on the field.
    public static Effect ACTION(String effectText, List<EffectChange> cost, List<EffectChange> effect) {
        return Effect.ACTION(effectText, List.of(), cost, effect);
    }

    public static Effect ACTION(String effectText, List<EffectChange> effect) {
        return Effect.ACTION(effectText, List.of(), List.of(), effect);
    }

    /** Action abbreviation that assumes no stipulations on the pattern and generates effect text. */
    public static Effect ACTION(String actionName, int actionDamage, Boolean[][] pattern) {
        return Effect.ACTION(actionName + " - " + actionDamage + " Damage.",
            List.of(),
            List.of(Target.pattern(pattern)),
            List.of(EffectChange.of(EffectChangeType.Damage, TargetType.Target, actionDamage)));
    }


    public static Effect OPTIONAL_CONTINUOUS(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, usableWhileShrouded, true, EffectType.OptionalContinuous, conditions, cost, effect);
    }

    /** You probably shouldn't being using this to define a continuous, unless that continuous has multiple effects, and the vast majority don't. */
    public static Effect OPTIONAL_CONTINUOUS(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return Effect.OPTIONAL_CONTINUOUS(effectText, false, conditions, cost, effect);
    }

    public static Effect OPTIONAL_CONTINUOUS(String effectText, boolean usableWhileShrouded, EffectChange effect) {
        return Effect.OPTIONAL_CONTINUOUS(effectText, usableWhileShrouded, List.of(Condition.inPosition(TargetType.CardsOnTheField)), List.of(), List.of(effect));
    }

    public static Effect OPTIONAL_CONTINUOUS(String effectText, EffectChange effect) {
        return Effect.OPTIONAL_CONTINUOUS(effectText, false, effect);
    }

    public static Effect CONSTANT_CONTINUOUS(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, usableWhileShrouded, true, EffectType.ConstantContinuous, conditions, cost, effect);
    }

    /** You probably shouldn't being using this to define a continuous, unless that continuous has multiple effects, and the vast majority don't. */
    public static Effect CONSTANT_CONTINUOUS(String effectText, List<Condition> conditions, List<EffectChange> effect) {
        return Effect.CONSTANT_CONTINUOUS(effectText, false, conditions, List.of(), effect);
    }

    public static Effect CONSTANT_CONTINUOUS(String effectText, List<Condition> conditions, EffectChange effect) {
        return Effect.CONSTANT_CONTINUOUS(effectText, conditions, List.of(effect));
    }

    /** You probably shouldn't be using this to define a continuous, unless that continuous has multiple effects, and the vast majority don't. */
    public static Effect CONSTANT_CONTINUOUS(String effectText, Condition condition, List<EffectChange> effect) {
        return Effect.CONSTANT_CONTINUOUS(effectText, List.of(condition), effect);
    }

    public static Effect CONSTANT_CONTINUOUS(String effectText, Condition condition, EffectChange effect) {
        return Effect.CONSTANT_CONTINUOUS(effectText, List.of(condition), List.of(effect));
    }

    public static Effect FIELD_CONSTANT_CONTINUOUS(String effectText, EffectChange effect) {
        return Effect.CONSTANT_CONTINUOUS(effectText, Condition.inPosition(TargetType.FieldZones), List.of(effect));
    }

    public static Effect RIDEABLE(String effectText, List<Condition> conditions, List<EffectChange> cost, List<TargetType> validRideable) {
        if (!effectText.toLowerCase().contains("rideable:")) // Rideable comes after any conditions on the rideable effect
            throw new IllegalArgumentException("Rideable effect (" + effectText + ") doesn't contain \"rideable:\"");
        return Effect.CONSTANT_CONTINUOUS(effectText, false, conditions, cost,
            List.of(EffectChange.of(EffectChangeType.Rideable, validRideable, VariableGameNum.ONE()))); // strength doesn't matter
    }

    /**
     * Includes the condition of the card being on the field.
     * You must include the condition of the rideable card being adjacent to the card, or being in the same pile.
     */
    public static Effect RIDEABLE(String effectText, List<TargetType> validRideable, List<EffectChange> cost) {
        return Effect.RIDEABLE(effectText,
            List.of(Condition.inPosition(TargetType.CardsOnTheField)), cost, validRideable);
    }

    /** Includes the condition of the card being on the field, and the rider being adjacent to the card. */
    public static Effect RIDEABLE(String effectText, List<EffectChange> cost) {
        return Effect.RIDEABLE(effectText, List.of(TargetType.ZonesAdjacentToThis, TargetType.Cards), cost);
    }

    /** Basic rideable cost of just material. Generates effect text. */
    public static Effect RIDEABLE(int rideableMaterialCost) {
        return Effect.RIDEABLE("Rideable: pay " + rideableMaterialCost + " material.",
            List.of(EffectChange.of(EffectChangeType.PayMaterial, TargetType.UsingCommander, rideableMaterialCost)));
    }

    /** Constant Continuous production abbreviation. */
    public static Effect PRODUCTION(int production) {
        return Effect.CONSTANT_CONTINUOUS("Production: " + production,
            new ArrayList<Condition>(List.of(
                Condition.inPosition(TargetType.FieldZones)
            )),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(EffectChangeType.Production, new ArrayList<TargetType>(List.of(TargetType.ControllingCommander)), production)
            )));
    }
}
