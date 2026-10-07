package ew.engine.cards;

import ew.engine.cards.effects.*;
import ew.engine.resolver.VariableGameNum;
import ew.engine.resolver.VariableNumType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CardDefinition {
    public final int cardID;
    public final CardRarity rarity;
    public final String cardArt;
    public final String name;
    public final CardType type;
    public final CardAttribute attribute;
    public final int health;
    public final boolean isToken;
    public final boolean isDecisive;
    public final Effect placeCost;
    public final List<Effect> effects;

    public CardDefinition(int cardID) {
        this.cardID = cardID;
        switch(cardID) {
            case 0 -> {
                rarity = CardRarity.COMMON;
                cardArt = "Card Placeholder.jpg";
                name = "Empty Card";
                isToken = false;
                isDecisive = false;
                type = CardType.ATTACK;
                attribute = CardAttribute.Human;
                health = 1;

                // This placeholder card doesn't use any abbreviation.
                placeCost = Effect.PLACE_COST(
                    "1 Material.",
                    new ArrayList<EffectChange>(List.of(
                        EffectChange.conditional(List.of(), EffectChangeType.PayMaterial, TargetType.UsingCommander, VariableGameNum.Num(1))
                    ))
                );

                effects = new ArrayList<Effect>(List.of(
                    Effect.ACTIVATABLE(
                        "Twice per turn → Empty Effect.",
                        false,
                        new ArrayList<Condition>(List.of(
                            Condition.inPosition(TargetType.CardsOnTheField),
                            Condition.conditional(List.of(), ConditionType.MaxEffectPerTurn, TargetType.Self, VariableGameNum.Num(2))
                        )),
                        new ArrayList<EffectChange>(),
                        new ArrayList<EffectChange>()
                    )
                ));
            }

            case 1 -> {
                rarity = CardRarity.COMMON;
                cardArt = "Card Placeholder.jpg";
                name = "Pike Trooper";
                isToken = false;
                isDecisive = false;
                type = CardType.ATTACK;
                attribute = CardAttribute.Human;
                health = 2;

                placeCost = Effect.BASIC_MATERIAL(CardType.ATTACK, 3);

                effects = new ArrayList<Effect>(Arrays.asList(
                    Effect.UNSHROUD(
                        "Unshroud: Move this card forwards 1 zone.",
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.MoveForwards, TargetType.Self, 1)
                        ))
                    ),
                    Effect.ACTION(
                        "Thrust - 3 Damage, +1 if this card is on your opponent's side.",
                        new ArrayList<EffectChange>(List.of(
                            Target.pattern(
                                new Boolean[][] {
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, true, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, true, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false}
                                }
                            )
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(
                                EffectChangeType.Damage,
                                TargetType.Target,
                                VariableGameNum.Variable(3, VariableNumType.BonusOnOpponentsSide, 1))
                        ))
                    )
                ));
            }

            case 2 -> {
                rarity = CardRarity.COMMON;
                cardArt = "Card Placeholder.jpg";
                name = "Bombard Wagon";
                isToken = false;
                isDecisive = false;
                type = CardType.ATTACK;
                attribute = CardAttribute.Machine;
                health = 5;

                placeCost = Effect.BASIC_MATERIAL(CardType.ATTACK, 4);

                effects = new ArrayList<Effect>(List.of(
                    Effect.ACTION(
                        "Bombard - Cannot Target Production Zones. Multiattack 3. 1 Damage.",
                        new ArrayList<EffectChange>(List.of(
                            Target.pattern(EffectChangeType.MultiTarget,
                                TargetType.AttackZones,
                                3,
                                new Boolean[][] {
                                    {false, false, false, false, false, false, false},
                                    {false, false, true,  false, true,  false, false},
                                    {false, false, true,  false, true,  false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false}
                                }
                            )
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.Damage, TargetType.Target, 1)
                        ))
                    )
                ));
            }

            case 3 -> {
                rarity = CardRarity.COMMON;
                cardArt = "Card Placeholder.jpg";
                name = "Iron Smelter";
                isToken = false;
                isDecisive = false;
                type = CardType.PRODUCTION;
                attribute = CardAttribute.Rock;
                health = 4;

                placeCost = Effect.BASIC_MATERIAL(CardType.PRODUCTION, 4);

                effects = new ArrayList<Effect>(Arrays.asList(
                    Effect.PRODUCTION(1),
                    Effect.FIELD_TRIGGER(
                        "Once per turn, if a production card you own is discarded or destroyed → gain 2 material.",
                        new ArrayList<Condition>(List.of(
                            Condition.UsesPerTurn(1),
                            Condition.of(ConditionType.CardDestroyedOrDiscarded,
                                List.of(TargetType.CardsYouOwn, TargetType.ProductionCards), 1)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.GainMaterial, TargetType.ControllingCommander, 2)
                        ))
                    )
                ));
            }

            case 4 -> {
                rarity = CardRarity.COMMON;
                cardArt = "Card Placeholder.jpg";
                name = "Solar Collector";
                isToken = false;
                isDecisive = false;
                type = CardType.PRODUCTION;
                attribute = CardAttribute.Plant;
                health = 2;

                placeCost = Effect.BASIC_MATERIAL(CardType.PRODUCTION, 3);

                effects = new ArrayList<Effect>(Arrays.asList(
                    Effect.PRODUCTION(2),
                    Effect.FIELD_TRIGGER(
                        "If the build phase ends → add 1 Energy Counter to this card.",
                        new ArrayList<Condition>(List.of(
                            Condition.Timing(ConditionType.BuildPhaseEnds)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.AddEnergyCounters, TargetType.Self, 1)
                        ))
                    ),
                    Effect.ACTION(
                        "Collect - Remove x Energy Counters from cards you control and target a card → the target restores x (max 2).",
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.RemoveEnergyCounters, TargetType.CardsYouControl,
                                VariableGameNum.DefineX(1, 2)
                            ),
                            Target.of(List.of(TargetType.CardsOnTheField))
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.Restore, TargetType.Target, VariableGameNum.PreviousX())
                        ))
                    )
                ));
            }

            case 5 -> {
                rarity = CardRarity.COMMON;
                cardArt = "Card Placeholder.jpg";
                name = "Piranha";
                isToken = false;
                isDecisive = false;
                type = CardType.ATTACK;
                attribute = CardAttribute.Water;
                health = 1;

                placeCost = Effect.BASIC_MATERIAL(CardType.ATTACK, 2);

                effects = new ArrayList<Effect>(Arrays.asList(
                    Effect.UNSHROUD(
                        "Unshroud: Move this card 1 zone forwards, backwards, left, or right.",
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.MoveAnyDirection, TargetType.Self, 1)
                        ))
                    ),
                    Effect.FIELD_TRIGGER(
                        "If the combat phase begins, remove 1 Tide Counter from this card → move this card forwards or backwards 1 zone.",
                        new ArrayList<Condition>(List.of(
                            Condition.Timing(ConditionType.CombatPhaseBegins)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.RemoveTideCounters, TargetType.Self, 1)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.MoveForwardsOrBackwards, TargetType.Self, 1)
                        ))
                    ),
                    Effect.ACTION(
                        "Nip - Multiattack: 2 different zones. 1 Damage. This card gains 1 Tide Counter for each unique card this attack damages.",
                        new ArrayList<EffectChange>(List.of(
                            Target.pattern(EffectChangeType.UniqueTarget, 2,
                                new Boolean[][] {
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, true,  false, true,  false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, true,  false, true,  false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false}
                                }
                            )
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.Damage, TargetType.Target, 1),
                            EffectChange.of(EffectChangeType.AddTideCounters, TargetType.Self,
                                VariableGameNum.Variable(0, VariableNumType.PerUniqueDamagedTarget, 1))
                        ))
                    )
                ));
            }

            case 6 -> {
                rarity = CardRarity.RARE;
                cardArt = "Card Placeholder.jpg";
                name = "Backroom Dealer";
                isToken = false;
                isDecisive = false;
                type = CardType.PRODUCTION;
                attribute = CardAttribute.Human;
                health = 6;

                placeCost = Effect.BASIC_MATERIAL(CardType.PRODUCTION, 6);

                effects = new ArrayList<Effect>(Arrays.asList(
                    Effect.PRODUCTION(2),
                    Effect.FIELD_ACTIVATABLE(
                        "While you control a shrouded card in attack zone(s), pay 3 material → " +
                            "place a shrouded Conspiracy Token, then you can secretly swap the positions of " +
                            "two shrouded cards you own in attack zones.",
                        new ArrayList<Condition>(List.of(
                            Condition.of(ConditionType.MinThings, List.of(TargetType.FieldZones, TargetType.ShroudedCards, TargetType.CardsYouControl), 1)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.PayMaterial, TargetType.UsingCommander, 3)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.PlaceConspiracyToken, List.of(TargetType.ZonesOnly, TargetType.AttackZones), 1),
                            EffectChange.conditional(Condition.OPTIONAL(), EffectChangeType.SecretlySwap2Choices, List.of(TargetType.CardsYouControl, TargetType.ShroudedCards), 1)
                        ))
                    )
                ));
            }

            case 7 -> {
                rarity = CardRarity.TOKEN;
                cardArt = "Card Placeholder.jpg";
                name = "Conspiracy Token";
                isToken = true;
                isDecisive = false;
                type = CardType.TOKENATTACK;
                attribute = CardAttribute.Human;
                health = 1;

                placeCost = Effect.TOKEN_COST(); // which is nothing

                effects = new ArrayList<Effect>(List.of(
                    Effect.UNSHROUD(
                        "Unshroud: Destroy this card.",
                        EffectChange.of(EffectChangeType.Destroy, TargetType.Self)
                    )
                ));
            }

            case 8 -> {
                rarity = CardRarity.COMMON;
                cardArt = "Card Placeholder.jpg";
                name = "Gadget";
                isToken = false;
                isDecisive = false;
                type = CardType.ATTACK;
                attribute = CardAttribute.Machine;
                health = 3;

                placeCost = Effect.BASIC_MATERIAL(CardType.ATTACK, 2);

                effects = new ArrayList<Effect>(List.of(
                    Effect.FIELD_ACTIVATABLE(
                        "Once per turn → " +
                            "randomly choose 1: (1) Move a machine card you control backwards 1 zone. (2) Move a machine card" +
                            "you control forwards 1 zone. (3) Both players draw 1 card. (4) Draw 1 card from your opponent's deck." +
                            "(5) Return this card to the hand, and if you do, place a machine card from your hand, except this card" +
                            "paying its place cost minus 2.",
                        new ArrayList<Condition>(List.of(
                            Condition.UsesPerTurn(1)
                        )),
                        new ArrayList<EffectChange>(List.of(

                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.chooseRandom(5),
                            EffectChange.conditional(Condition.CHOICE1(), EffectChangeType.MoveBackwards, List.of(TargetType.CardsYouControl, TargetType.MachineCards), 1),
                            EffectChange.conditional(Condition.CHOICE2(), EffectChangeType.MoveForwards, List.of(TargetType.CardsYouControl, TargetType.MachineCards), 1),
                            EffectChange.conditional(Condition.CHOICE3(), EffectChangeType.Draw, TargetType.Commanders, 1),
                            EffectChange.conditional(Condition.CHOICE4(), EffectChangeType.DrawFromOpponentsDeck, TargetType.ControllingCommander, 1),
                            EffectChange.conditional(Condition.CHOICE5(), EffectChangeType.ReturnToHand, TargetType.Self, 1),
                            EffectChange.conditional(Condition.AndIfYouDo(), EffectChangeType.PlaceMinusStrengthMaterialMin0,
                                List.of(TargetType.CardsInHand, TargetType.MachineCards, TargetType.CardsExceptThisCard), 2)
                        ))
                    )
                ));
            }

            // I don't want to have to change the resolving system later for rituals, so I'm just implementing one now.
            case 9 -> {
                rarity = CardRarity.COMMON;
                cardArt = "Card Placeholder.jpg";
                name = "Plantsheep";
                isToken = false;
                isDecisive = false;
                type = CardType.ATTACK;
                attribute = CardAttribute.Ritual;
                health = 4;

                placeCost = Effect.PLACE_COST(
                    "By an effect that uses cards, " +
                        "displace cards until the end of the next Aftermath Phase whose material cost totals 6 Material, " +
                        "then pay 2 material for each card used.",
                    new ArrayList<Condition>(),
                    new ArrayList<EffectChange>(List.of(
                        EffectChange.duration(EffectChangeType.DisplaceMaterial, List.of(TargetType.CurrentlyUsable, TargetType.CardsNotDisplaced),
                            6, new EffectDuration(ConditionType.AftermathPhaseEnds, 2)),
                        EffectChange.of(EffectChangeType.PayMaterial, TargetType.UsingCommander, VariableGameNum.Variable(VariableNumType.PerCardThisCardUses, 2))
                    ))
                );

                effects = new ArrayList<Effect>(Arrays.asList(
                    Effect.PLACED(
                        "Placed: Target a card in this card's lane → the target restores 2.",
                        new ArrayList<Condition>(List.of(
                            Condition.of(ConditionType.MinThings, List.of(TargetType.ZonesInThisLane, TargetType.Cards), 1)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            Target.of(List.of(TargetType.ZonesInThisLane, TargetType.Cards))
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.Restore, TargetType.Target, 2)
                        ))
                    ),
                    Effect.FIELD_TRIGGER(
                        "Twice per turn, if a card you control is destroyed, target a card you control → " +
                            "the target restores 1.",
                        new ArrayList<Condition>(List.of(
                            Condition.of(ConditionType.CardDestroyed, TargetType.CardsYouControl, 1),
                            Condition.of(ConditionType.MaxEffectPerTurn, TargetType.Self, 2)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            Target.of(TargetType.CardsYouControl)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.Restore, TargetType.Target, 1)
                        ))
                    ),
                    Effect.ACTIVATABLE(
                        "While this card is in your hand, reveal this card → " +
                            "place a ritual card from your hand using card(s) from your hand.",
                        new ArrayList<Condition>(List.of(
                            Condition.inPosition(List.of(TargetType.CardsInHand))
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.duration(EffectChangeType.Reveal, TargetType.Self, 1, new EffectDuration(ConditionType.AftermathPhaseEnds, 1))
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.PlaceUsingHand, List.of(TargetType.CardsInHand, TargetType.CardsYouUse, TargetType.RitualCards), 1)
                        ))
                    ),
                    Effect.ACTION(
                        "Chew - If the target's health is higher than their original health → " +
                            "halve the target's health, rounded up. 1 damage.",
                        new ArrayList<EffectChange>(List.of(
                            Target.pattern(
                                new Boolean[][] {
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, true,  false, false, false},
                                    {false, false, true,  false, true,  false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false}
                                }
                            )
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.conditional(
                                Condition.of(
                                    ConditionType.MinHealth,
                                    TargetType.Target,
                                    VariableGameNum.Variable(VariableNumType.ThisTargetsOriginalHealth, 1)),
                                EffectChangeType.HalveHealthRoundedUp,
                                TargetType.Target,
                                1),
                            EffectChange.of(EffectChangeType.Damage, TargetType.Target, 1)
                        ))
                    )
                ));
            }

            default -> throw new IllegalArgumentException("Unexpected Card ID: " + cardID);
        }
        // cardArt = name + ".jpg";
    }

    /** Returns a token's cardID based on its referenced EffectChangeType */
    public static int getTokenID(EffectChangeType tokenPlacement) {
        return switch(tokenPlacement) {
            case PlaceConspiracyToken -> 7;
            default -> throw new IllegalArgumentException("Token type (" + tokenPlacement.toString().substring(5) + ") not yet implemented.");
        };
    }

    public static int maxID() {
        return 9;
    }
}
