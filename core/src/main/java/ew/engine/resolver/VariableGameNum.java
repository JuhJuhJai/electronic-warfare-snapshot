package ew.engine.resolver;

import ew.engine.board.*;
import ew.engine.cards.effects.EffectChangeType;
import ew.server.ControllerInstance;

import java.util.ArrayList;import java.util.List;

public class VariableGameNum {
    final int baseValue;
    VariableGameNum min;
    VariableGameNum max;
    final VariableNumType type;
    /** May multiply or add to the final result, depending on the VariableGameNumType. */
    VariableGameNum modifier;
    final boolean isSet;

    private VariableGameNum(int baseValue, VariableNumType type, VariableGameNum modifier, VariableGameNum min, VariableGameNum max, boolean isSet) {
        this.baseValue = baseValue;
        this.min = min;
        this.max = max;
        this.type = type;
        this.modifier = modifier;
        this.isSet = isSet;
    }

    public static VariableGameNum Copy(VariableGameNum copyNum) {
        return new VariableGameNum(copyNum.baseValue, copyNum.type, copyNum.modifier, copyNum.min, copyNum.max, copyNum.isSet);
    }

    public static VariableGameNum CopyDifferentType(VariableGameNum copyNum, VariableNumType newType) {
        return new VariableGameNum(copyNum.baseValue, newType, copyNum.modifier, copyNum.min, copyNum.max, copyNum.isSet);
    }

    /** Returns the copyNum's min if it has one, VariableGameNum.ONE otherwise. */
    public static VariableGameNum MinCopy(VariableGameNum copyNum) {
        if (copyNum.min != null) return copyNum.min;
        return VariableGameNum.ONE();
    }

    public static VariableGameNum ZERO() {
        return VariableGameNum.Num(0);
    }

    public static VariableGameNum ONE() {
        return VariableGameNum.Num(1);
    }

    /** Normal num. */
    public static VariableGameNum Num(int value) {
        return new VariableGameNum(value, VariableNumType.Num, null, null, null, true);
    }


    public static VariableGameNum Variable(int baseValue, VariableNumType type, VariableGameNum modifier, VariableGameNum min, VariableGameNum max) {
        return new VariableGameNum(baseValue, type, modifier, min, max, true);
    }

    public static VariableGameNum Variable(int baseValue, VariableNumType type, int modifier, VariableGameNum min, VariableGameNum max) {
        return VariableGameNum.Variable(baseValue, type, VariableGameNum.Num(modifier), min, max);
    }

    /** Sets the minimum to 0 and the maximum to 99. */
    public static VariableGameNum Variable(int baseValue, VariableNumType type, int modifier) {
        return VariableGameNum.Variable(baseValue, type, modifier, VariableGameNum.ZERO(), VariableGameNum.Num(99));
    }

    /** Sets the base value to 0, the minimum to 0 and the maximum to 99. */
    public static VariableGameNum Variable(VariableNumType type, VariableGameNum modifier) {
        return VariableGameNum.Variable(0, type, modifier, VariableGameNum.ZERO(), VariableGameNum.Num(99));
    }

    /** Sets the base value to 0, the minimum to 0 and the maximum to 99. */
    public static VariableGameNum Variable(VariableNumType type, int modifier) {
        return VariableGameNum.Variable(0, type, modifier, VariableGameNum.ZERO(), VariableGameNum.Num(99));
    }


    /** When X has been defined before in the same effect, use this instead of defining X again. */
    public static VariableGameNum PreviousX() {
        return new VariableGameNum(0, VariableNumType.XNum, null, null, null, false);
    }

    public static VariableGameNum PreviousXRoundedDown() {
        return new VariableGameNum(0, VariableNumType.XHalvedDown, null, null, null, false);
    }

    public static VariableGameNum PreviousXRoundedUp() {
        return new VariableGameNum(0, VariableNumType.XHalvedUp, null, null, null, false);
    }

    /** When ? has been defined before in the same effect, use this instead of defining X again. */
    public static VariableGameNum PreviousQ() {
        return new VariableGameNum(0, VariableNumType.QNum, null, null, null, false);
    }

    public static VariableGameNum PreviousQRoundedUp() {
        return new VariableGameNum(0, VariableNumType.QHalvedUp, null, null, null, false);
    }

    public static VariableGameNum PreviousQRoundedDown() {
        return new VariableGameNum(0, VariableNumType.QHalvedDown, null, null, null, false);
    }

    public static VariableGameNum DefineX(VariableGameNum min, VariableGameNum max) {
        return new VariableGameNum(0, VariableNumType.XNum, null, min, max, true);
    }

    public static VariableGameNum DefineX(int min, int max) {
        return VariableGameNum.DefineX(VariableGameNum.Num(min), VariableGameNum.Num(max));
    }

    public static VariableGameNum DefineXEven(VariableGameNum min, VariableGameNum max) {
        return new VariableGameNum(0, VariableNumType.EvenXNum, null, min, max, true);
    }

    public static VariableGameNum DefineXEven(int min, int max) {
        return VariableGameNum.DefineXEven(VariableGameNum.Num(min), VariableGameNum.Num(max));
    }

    public static VariableGameNum DefineXOdd(VariableGameNum min, VariableGameNum max) {
        return new VariableGameNum(0, VariableNumType.OddXNum, null, min, max, true);
    }

    public static VariableGameNum DefineXOdd(int min, int max) {
        return VariableGameNum.DefineXOdd(VariableGameNum.Num(min), VariableGameNum.Num(max));
    }

    public static VariableGameNum DefineQ(int baseValue, VariableGameNum modifier, VariableGameNum min, VariableGameNum max) {
        return new VariableGameNum(baseValue, VariableNumType.QNum, modifier, min, max, true);
    }

    public static VariableGameNum DefineQ(int baseValue, VariableGameNum modifier, int min, int max) {
        return DefineQ(baseValue, modifier, VariableGameNum.Num(min), VariableGameNum.Num(max));
    }

    /** Abbreviation for defining Q that sets the minimum to 0 and the maximum to 99 */
    public static VariableGameNum DefineQ(int baseValue, VariableGameNum modifier) {
        return DefineQ(baseValue, modifier, 0, 99);
    }

    /** Abbreviation for defining Q that sets the base value to 0, the minimum to 0, and the maximum to 99 */
    public static VariableGameNum DefineQ(VariableGameNum modifier) {
        return DefineQ(0, modifier, 0, 99);
    }

    public VariableNumType getType() { return type; }

    public VariableGameNum getModifier() { return modifier; }

    public int getValue(GameState state,
                        ControllerInstance chooser,
                        EffectStipulationObject effectUsing,
                        List<EffectChangeInstance> activeContinuous) {
        return getValue(state, chooser, effectUsing, (List<LivingObject>) null, activeContinuous);
    }

    public int getValue(GameState state,
                        ControllerInstance chooser,
                        EffectStipulationObject effectUsing,
                        LivingObject target,
                        List<EffectChangeInstance> activeContinuous) {
        return getValue(state, chooser, effectUsing, List.of(target), activeContinuous);
    }

    /**
     * Determines the value of a number.
     * May also set the declared values of the livingObject user.
     */
    public int getValue(GameState state,
                        ControllerInstance chooser,
                        EffectStipulationObject effectUsing,
                        List<LivingObject> target, // May be null
                        List<EffectChangeInstance> activeContinuous) {
        final LivingObject user = effectUsing.getUser();

        switch (this.type) {
            // normal number. Edit with activeContinuous based on effectUsing. Every continuous that affects ANY number goes here.
            case Num -> {
                return baseValue;
            }

            // x. Also sets the user's x.
            case XNum -> {
                if (isSet) {
                    final int choice = baseValue + chooser.addNumChoice(getRange(
                        min.getValue(state, chooser, effectUsing, target, activeContinuous),
                        max.getValue(state, chooser, effectUsing, target, activeContinuous),
                        null)
                    );
                    user.setX(choice);
                    return choice;
                }
                else return user.getX();
            }

            case EvenXNum -> {
                final int choice = baseValue + chooser.addNumChoice(getRange(
                    min.getValue(state, chooser, effectUsing, target, activeContinuous),
                    max.getValue(state, chooser, effectUsing, target, activeContinuous),
                    true)
                );
                user.setX(choice);
                return choice;
            }

            case OddXNum -> {
                final int choice = baseValue + chooser.addNumChoice(getRange(
                    min.getValue(state, chooser, effectUsing, target, activeContinuous),
                    max.getValue(state, chooser, effectUsing, target, activeContinuous),
                    false)
                );
                user.setX(choice);
                return choice;
            }

            case XHalvedUp -> {
                final int x = user.getX();
                return x % 2 == 0? x / 2 : x / 2 + 1;
            }

            case XHalvedDown -> {
                return user.getX() / 2;
            }

            // ?. Also sets the user's ?.
            case QNum -> {
                if (isSet) {
                    final int gameValue = baseValue + modifier.getValue(state, chooser, effectUsing, target, activeContinuous);
                    user.setQ(gameValue);
                    return gameValue;
                }
                else return user.getQ();
            }

            case QHalvedUp -> {
                final int q = user.getQ();
                return q % 2 == 0? q / 2 : q / 2 + 1;
            }

            case QHalvedDown -> {
                return user.getQ() / 2;
            }

            // *Declaring* is an effect change type that must be used beforehand.
            case DeclaredNum -> {
                return user.getDeclaredNum();
            }

            case DeclaredName -> {
                return user.getDeclaredName();
            }

            case DeclaredAttribute -> {
                return user.getDeclaredAttribute().toDeclared();
            }

            case DeclaredPile -> {
                return user.getDeclaredPile().position.getBoardLocation().toDeclared();
            }

            case DeclaredDirection -> {
                return user.getDeclaredDirection().toDeclared();
            }

            // Specific case
            case BonusOnOpponentsSide -> {
                for (ZoneInstance zoneInstance : state.field.getSide(user.getController().opponentOf())) {
                    for (CardInstance card : zoneInstance.getCards()) {
                        if (user.getInstanceID() == card.getInstanceID()) return baseValue + modifier.getValue(state, chooser, effectUsing, target, activeContinuous);
                    }
                }
                return baseValue;
            }

            case PerUniqueDamagedTarget -> { // basically just for Piranha
                return baseValue + modifier.getValue(state, chooser, effectUsing, target, activeContinuous) *
                    Math.toIntExact(state.getJustHappened().stream()
                    .flatMap(List::stream)
                    .filter(t -> t.getChange().getType() == EffectChangeType.Damage && t.getChange().getUser().equals(user) && t.getEventInfo().get(1) > 0) // 1 is how much actual damage impacted health
                    .map(GameEvent::getTarget)
                    .distinct()
                    .count());
            }

            case Precision -> {
                return baseValue + state.getCommander(user.getUser()).getPrecision()
                    * modifier.getValue(state, chooser, effectUsing, target, activeContinuous);
            }

            case PerCardThisCardUses -> {
                // When displacing for material, a card's Q value is set to the number of cards used.
                return baseValue + modifier.getValue(state, chooser, effectUsing, target, activeContinuous) * user.getQ();
            }

            case MinCardThisCardUses -> {
                // When checking for material available, a card's Q value is set to the minimum number of cards required.
                return baseValue + modifier.getValue(state, chooser, effectUsing, target, activeContinuous) * user.getQ();
            }

            case ThisTargetsOriginalHealth -> {
                return baseValue + modifier.getValue(state, chooser, effectUsing, target, activeContinuous) + target.stream().mapToInt(LivingObject::getHealth).sum();
            }

            default -> throw new IllegalArgumentException("Unexpected VariableGameNum Type: " + type);
        }
    }

    /**
     * Returns a list of Integer between the range, inclusive, if evens is null.
     * If evens is true, the retuned list only contains even numbers, otherwise it contains only odd numbers.
     */
    private static List<Integer> getRange(int min, int max, Boolean evens) {
        List<Integer> nums = new ArrayList<>();
        if (evens == null) {
            for (int i = min; i <= max; i++) {
                nums.add(i);
            }
        } else if (evens) {
            if (min % 2 == 0) {
                for (int i = min; i <= max; i += 2) {
                    nums.add(i);
                }
            }
            else for (int i = min + 1; i <= max; i += 2) {
                nums.add(i);
            }
        }
        else {
            if (min % 2 == 1) {
                for (int i = min; i <= max; i += 2) {
                    nums.add(i);
                }
            }
            else for (int i = min + 1; i <= max; i += 2) {
                nums.add(i);
            }
        }
        return nums;
    }
}
