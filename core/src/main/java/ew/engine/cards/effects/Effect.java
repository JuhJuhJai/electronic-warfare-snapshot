package ew.engine.cards.effects;

import ew.engine.cards.CardType;
import ew.engine.resolver.VariableGameNum;
import ew.engine.resolver.VariableNumType;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Stream;

public class Effect {
    final List<Condition> conditions;
    final List<EffectChange> cost;
    final List<EffectChange> effect;
    public final EffectType type;
    final String effectText;
    final boolean usableWhileShrouded;

    public List<Condition> getConditions() { return conditions; }
    public List<EffectChange> getCostChanges() { return cost; }
    public List<EffectChange> getEffectChanges() { return effect; }
    public EffectType getType() { return type; }
    public String getEffectText() { return effectText; }
    public boolean isUsableWhileShrouded() { return usableWhileShrouded; }

    public Effect(String effectText,
                  boolean usableWhileShrouded,
                  EffectType type,
                  List<Condition> conditions,
                  List<EffectChange> cost,
                  List<EffectChange> effect) {
        this.effectText = effectText;
        this.usableWhileShrouded = usableWhileShrouded;
        this.type = type;
        this.conditions = conditions;
        this.cost = cost;
        this.effect = effect;
    }

    // There's a world where there's placeCost with effects.
    public static Effect PLACE_COST(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, usableWhileShrouded, EffectType.PlaceCost, conditions, cost, effect);
    }

    public static Effect PLACE_COST(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return PLACE_COST(effectText, false, conditions, cost, effect);
    }

    public static Effect PLACE_COST(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost) {
        return PLACE_COST(effectText, usableWhileShrouded, conditions, cost, List.of());
    }

    public static Effect PLACE_COST(String effectText, List<Condition> conditions, List<EffectChange> cost) {
        return PLACE_COST(effectText, conditions, cost, List.of());
    }

    public static Effect PLACE_COST(String effectText, List<EffectChange> cost) {
        return PLACE_COST(effectText, List.of(), cost, List.of());
    }

    /** Abbreviation for PLACE_COST that generates effect text and the proper placement condition based on the card's type. */
    public static Effect BASIC_MATERIAL(CardType type, int placeMaterialCost) {
        if (!(type == CardType.ATTACK || type == CardType.PRODUCTION)) throw new IllegalArgumentException("Invalid card type (" + type + ") for Basic Material Cost.");
        return PLACE_COST(placeMaterialCost + " Material",
            Arrays.asList(
                Condition.of(ConditionType.MinMaterial, new ArrayList<TargetType>(List.of(TargetType.UsingCommander)), placeMaterialCost),
                Condition.of(type == CardType.ATTACK ?
                        ConditionType.AvailableAttackPlacementSpace :
                        ConditionType.AvailableProductionPlacementSpace,
                    List.of(TargetType.UsingCommander), 1)
            ),
            List.of(
                EffectChange.of(EffectChangeType.PayMaterial, new ArrayList<TargetType>(List.of(TargetType.UsingCommander)), placeMaterialCost)
            )
        );
    }

    /** Abbreviation for BASIC_MATERIAL that takes a token and only requires available space. */
    public static Effect TOKEN_MATERIAL(CardType tokenType) {
        if (!(tokenType == CardType.TOKENATTACK || tokenType == CardType.TOKENPRODUCTION)) throw new IllegalArgumentException("Invalid card type (" + tokenType + ") for Token Material Cost.");
        return PLACE_COST("",
            List.of(
                Condition.of(tokenType == CardType.TOKENATTACK ?
                        ConditionType.AvailableAttackPlacementSpace :
                        ConditionType.AvailableProductionPlacementSpace,
                    List.of(TargetType.UsingCommander), 1)
            ),
            List.of()
        );
    }

    /**
     * Abbreviation for PlaceCost designed for cards that must be placed by an effect which uses cards
     * and follow the multiple of 3 rule for placement cost. (Displace cards until end of next
     * aftermath whose material cost totals [x] material, then pay [x / 3] material per card used).
     */
    public static Effect RITUAL_COST(CardType type, int materialCostTotal) {
        if (!(type == CardType.ATTACK || type == CardType.PRODUCTION)) throw new IllegalArgumentException("Invalid card type (" + type + ") for Ritual Place Cost.");
        final int paymentPerCard = (materialCostTotal + 1) / 3; // +1 for proper rounding on rituals without an exact multiple of 3 for total cost
        return PLACE_COST("By an effect that uses cards, displace cards until the end of the next Aftermath Phase " +
            "whose material cost totals " + materialCostTotal + " material, then pay " + paymentPerCard + " for each card used.",
            new ArrayList<Condition>(List.of(
                Condition.of(ConditionType.MaterialAvailable, List.of(TargetType.CurrentlyUsable, TargetType.CardsNotDisplaced), materialCostTotal),
                Condition.of(ConditionType.MinMaterial, TargetType.UsingCommander, VariableGameNum.Variable(VariableNumType.MinCardThisCardUses, paymentPerCard)),
                Condition.of(ConditionType.AvailableAttackPlacementSpace, TargetType.UsingCommander, 1)
            )),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(EffectChangeType.DisplaceMaterial, List.of(TargetType.CurrentlyUsable, TargetType.CardsNotDisplaced), materialCostTotal),
                EffectChange.of(EffectChangeType.PayMaterial, TargetType.UsingCommander, VariableGameNum.Variable(VariableNumType.PerCardThisCardUses, paymentPerCard))
            ))
        );
    }

    // Unshroud's condition to be used when the card is unshrouded isn't controlled by the card, it always happens when the card unshrouds. Handled by the resolver.
    public static Effect UNSHROUD(String effectText, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, false, EffectType.Unshroud, List.of(), cost, effect);
    }

    public static Effect UNSHROUD(String effectText, List<EffectChange> effect) {
        return UNSHROUD(effectText, List.of(), effect);
    }

    public static Effect UNSHROUD(String effectText, List<EffectChange> cost, EffectChange effect) {
        return new Effect(effectText, false, EffectType.Unshroud, List.of(), cost, List.of(effect));
    }

    public static Effect UNSHROUD(String effectText, EffectChange effect) {
        return UNSHROUD(effectText, List.of(), List.of(effect));
    }


    // Placed ALWAYS must remain on the field before it resolves.
    public static Effect PLACED(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, false, EffectType.Placed, Stream.of(List.of(Condition.inPosition(TargetType.CardsOnTheField)), conditions).flatMap(List::stream).toList(), cost, effect);
    }

    public static Effect PLACED(String effectText, List<EffectChange> cost, List<EffectChange> effect) {
        return PLACED(effectText, List.of(), cost, effect);
    }

    public static Effect PLACED(String effectText, List<EffectChange> effect) {
        return PLACED(effectText, List.of(), List.of(), effect);
    }


    public static Effect TRIGGER(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, usableWhileShrouded, EffectType.Trigger, conditions, cost, effect);
    }

    public static Effect TRIGGER(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return TRIGGER(effectText, false, conditions, cost, effect);
    }

    public static Effect FIELD_TRIGGER(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return TRIGGER(effectText, usableWhileShrouded, Stream.of(List.of(Condition.inPosition(TargetType.CardsOnTheField)), conditions).flatMap(List::stream).toList(), cost, effect);
    }

    public static Effect FIELD_TRIGGER(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return FIELD_TRIGGER(effectText, false, conditions, cost, effect);
    }

    public static Effect FIELD_TRIGGER(String effectText, List<Condition> conditions, List<EffectChange> effect) {
        return FIELD_TRIGGER(effectText, false, conditions, List.of(), effect);
    }


    public static Effect ACTIVATABLE(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, usableWhileShrouded, EffectType.Activatable, conditions, cost, effect);
    }

    public static Effect ACTIVATABLE(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return ACTIVATABLE(effectText, false, conditions, cost, effect);
    }

    public static Effect ACTIVATABLE(String effectText, List<Condition> conditions, List<EffectChange> effect) {
        return ACTIVATABLE(effectText, false, conditions, List.of(), effect);
    }

    public static Effect FIELD_ACTIVATABLE(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return ACTIVATABLE(effectText, usableWhileShrouded, Stream.of(List.of(Condition.inPosition(TargetType.CardsOnTheField)), conditions).flatMap(List::stream).toList(), cost, effect);
    }

    public static Effect FIELD_ACTIVATABLE(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return FIELD_ACTIVATABLE(effectText, false, conditions, cost, effect);
    }

    public static Effect FIELD_ACTIVATABLE(String effectText, List<EffectChange> cost, List<EffectChange> effect) {
        return FIELD_ACTIVATABLE(effectText, List.of(), cost, effect);
    }

    public static Effect FIELD_ACTIVATABLE(String effectText, List<EffectChange> effect) {
        return FIELD_ACTIVATABLE(effectText, List.of(), List.of(), effect);
    }


    public static Effect ACTION(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return new Effect(effectText, usableWhileShrouded, EffectType.Action, conditions, cost, effect);
    }

    public static Effect ACTION(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return Effect.ACTION(effectText, false, conditions, cost, effect);
    }

    // Actions don't need conditions as the resolver already checks for the base condition of it being the combat phase and their lane being selected.
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
        return new Effect(effectText, usableWhileShrouded, EffectType.OptionalContinuous, conditions, cost, effect);
    }

    public static Effect OPTIONAL_CONTINUOUS(String effectText, List<Condition> conditions, List<EffectChange> cost, List<EffectChange> effect) {
        return Effect.OPTIONAL_CONTINUOUS(effectText, false, conditions, cost, effect);
    }

    public static Effect OPTIONAL_CONTINUOUS(String effectText, boolean usableWhileShrouded, EffectChange effect) {
        return Effect.OPTIONAL_CONTINUOUS(effectText, usableWhileShrouded, List.of(Condition.inPosition(TargetType.CardsOnTheField)), List.of(), List.of(effect));
    }

    public static Effect OPTIONAL_CONTINUOUS(String effectText, EffectChange effect) {
        return Effect.OPTIONAL_CONTINUOUS(effectText, List.of(Condition.inPosition(TargetType.CardsOnTheField)), List.of(), List.of(effect));
    }


    public static Effect CONSTANT_CONTINUOUS(String effectText, boolean usableWhileShrouded, List<Condition> conditions, List<EffectChange> effect) {
        return new Effect(effectText, usableWhileShrouded, EffectType.ConstantContinuous, conditions, List.of(), effect);
    }

    public static Effect CONSTANT_CONTINUOUS(String effectText, List<Condition> conditions, List<EffectChange> effect) {
        return Effect.CONSTANT_CONTINUOUS(effectText, false, conditions, effect);
    }

    public static Effect CONSTANT_CONTINUOUS(String effectText, List<Condition> conditions, EffectChange effect) {
        return Effect.CONSTANT_CONTINUOUS(effectText, conditions, List.of(effect));
    }

    public static Effect CONSTANT_CONTINUOUS(String effectText, Condition condition, List<EffectChange> effect) {
        return Effect.CONSTANT_CONTINUOUS(effectText, List.of(condition), effect);
    }

    public static Effect CONSTANT_CONTINUOUS(String effectText, Condition condition, EffectChange effect) {
        return Effect.CONSTANT_CONTINUOUS(effectText, List.of(condition), List.of(effect));
    }

    public static Effect FIELD_CONSTANT_CONTINUOUS(String effectText, EffectChange effect) {
        return Effect.CONSTANT_CONTINUOUS(effectText, Condition.inPosition(TargetType.CardsOnTheField), List.of(effect));
    }

    /** Constant Continuous production abbreviation. */
    public static Effect PRODUCTION(int production) {
        return Effect.CONSTANT_CONTINUOUS("Production: " + production,
            new ArrayList<Condition>(List.of(
                Condition.of(ConditionType.MinThings, new ArrayList<TargetType>(List.of(TargetType.Self, TargetType.FieldZones)), 1)
            )),
            new ArrayList<EffectChange>(List.of(
                EffectChange.of(EffectChangeType.Production, new ArrayList<TargetType>(List.of(TargetType.ControllingCommander)), production)
            )));
    }
}
