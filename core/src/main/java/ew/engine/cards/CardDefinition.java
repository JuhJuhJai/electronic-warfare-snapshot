package ew.engine.cards;

import ew.engine.cards.effects.*;
import ew.engine.resolver.VariableGameNum;
import ew.engine.resolver.VariableNumType;

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

                // There's a lot of abbreviation you can use that this placeholder card doesn't attempt to use.
                placeCost = Effect.PLACE_COST(
                    "1 Material.",
                    new ArrayList<EffectChange>(List.of(
                        EffectChange.conditional(List.of(), EffectChangeType.PayMaterial, TargetType.UsingCommander, VariableGameNum.Num(1))
                    )),
                    new ArrayList<EffectChange>(List.of(
                        EffectChange.of(EffectChangeType.selfPlacementEffect, TargetType.OnlyAttackZonesOnYourSide, VariableGameNum.ONE())
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
                    Effect.ACTION("""
                            Thrust
                            3 Damage, +1 if this card is on your opponent's side.
                            """,
                        new ArrayList<EffectChange>(List.of(
                            Target.pattern(
                                new Boolean[][] {
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, true ,false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, true ,false, false, false},
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
                                    {false, false, true , false, true , false, false},
                                    {false, false, true , false, true , false, false},
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
                    Effect.ACTION("""
                        Collect
                        Remove x Energy Counters from cards you control and target a card → the target restores x (max 2).
                        """,
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.RemoveEnergyCounters, TargetType.CardsYouControl,
                                VariableGameNum.DefineX( // Min is 0
                                    VariableGameNum.Variable(0, VariableNumType.ThisCardsEnergyCounters, 1,
                                        // Max is this card's num of Energy Counters
                                        VariableGameNum.ZERO(), VariableGameNum.Num(2))) // max 2
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
                        """
                            Nip
                            Multiattack: 2 different zones.
                            1 Damage.
                            This card gains 1 Tide Counter for each unique card this attack damages.
                            """,
                        new ArrayList<EffectChange>(List.of(
                            Target.pattern(EffectChangeType.UniqueTarget, 2,
                                new Boolean[][] {
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, true , false, true , false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, true , false, true , false, false},
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
                type = CardType.TOKEN_ATTACK;
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
                            EffectChange.ChooseRandom(1, 5),
                            EffectChange.conditional(Condition.CHOICE1(), EffectChangeType.MoveBackwards, List.of(TargetType.CardsYouControl, TargetType.MachineCards), 1),
                            EffectChange.conditional(Condition.CHOICE2(), EffectChangeType.MoveForwards, List.of(TargetType.CardsYouControl, TargetType.MachineCards), 1),
                            EffectChange.conditional(Condition.CHOICE3(), EffectChangeType.Draw, TargetType.Commanders, 1),
                            EffectChange.conditional(Condition.CHOICE4(), EffectChangeType.DrawFromOpponentsDeck, TargetType.ControllingCommander, 1),
                            EffectChange.conditional(Condition.CHOICE5(), EffectChangeType.AddToHand, TargetType.Self, 1),
                            EffectChange.conditional(Condition.AndIfYouDo(), EffectChangeType.PlaceMinusStrengthMaterialMin0,
                                List.of(TargetType.CardsInHand, TargetType.MachineCards, TargetType.CardsExceptThisCard), 2)
                        ))
                    )
                ));
            }

            // To test a place cost that isn't just material, and also a discard of two things, and also 2 choices.
            case 9 -> {
                rarity = CardRarity.RARE;
                cardArt = "Card Placeholder.jpg";
                name = "Raw All-Consumer";
                isToken = false;
                isDecisive = false;
                type = CardType.ATTACK;
                attribute = CardAttribute.Flame;
                health = 5;

                placeCost = Effect.PLACE_COST(CardType.ATTACK,
                    "6 Material and discard 2 cards.",
                    new ArrayList<EffectChange>(List.of(
                        EffectChange.of(EffectChangeType.PayMaterial, TargetType.UsingCommander, VariableGameNum.Num(1)),
                        EffectChange.of(EffectChangeType.Discard, TargetType.UsingCommander, 2)
                    ))
                );

                effects = new ArrayList<Effect>(List.of(
                    Effect.PLACED(
                        "Target a card in this card’s lane → the target ignites 3",
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.MultiTarget, List.of(TargetType.ZonesInThisLane, TargetType.Cards), 1)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.AddFlameCounters, TargetType.Target, 3)
                        ))
                    ),
                    Effect.FIELD_TRIGGER(
                        "If this card is destroyed while it has Flame Counter(s), discard 1 card → " +
                            "Choose 2 different options: (1) Place this card, only paying its material cost. " +
                            "(2) Add 1 discarded Flame card you own to your hand. (3) Draw 1 card.",
                        new ArrayList<Condition>(List.of(
                            Condition.of(ConditionType.CardDestroyed, TargetType.Self),
                            Condition.of(ConditionType.MinFlameCounters, TargetType.Self, 1)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.Discard, TargetType.UsingCommander, 1)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.Choose(2, 3),
                            EffectChange.conditional(Condition.CHOICE1(), EffectChangeType.PlaceWithOnlyMaterialCost, TargetType.Self, 1),
                            EffectChange.conditional(Condition.CHOICE2(), EffectChangeType.AddToHand,
                                List.of(TargetType.CardsInDiscardPile, TargetType.CardsYouUse, TargetType.FlameCards), 1),
                            EffectChange.conditional(Condition.CHOICE3(), EffectChangeType.Draw, TargetType.UsingCommander, 1)
                        ))
                    ),
                    Effect.ACTION("""
                            Infernize
                            2 damage.
                            If the target is destroyed, this card ignites 3.
                            """,
                        new ArrayList<EffectChange>(List.of(
                            Target.pattern(
                                new Boolean[][] {
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, true , true , true , false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false}
                                }
                            )
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.DamageTarget(2),
                            EffectChange.conditional(Condition.TargetDestroyed(), EffectChangeType.AddFlameCounters,
                                TargetType.Self, 3)
                        ))
                    )
                ));
            }

            // I don't want to have to change the resolving system later for rituals, so I'm just implementing one now.
            case 10 -> {
                rarity = CardRarity.COMMON;
                cardArt = "Card Placeholder.jpg";
                name = "Plantsheep";
                isToken = false;
                isDecisive = false;
                type = CardType.ATTACK;
                attribute = CardAttribute.Ritual;
                health = 4;

                placeCost = Effect.PLACE_COST(CardType.ATTACK,
                    "By an effect that uses cards, " +
                        "displace cards until the end of the next Aftermath Phase whose material cost totals 6 Material, " +
                        "then pay 2 material for each card used.",
                    new ArrayList<EffectChange>(List.of(
                        EffectChange.duration(EffectChangeType.DisplaceMaterial, List.of(TargetType.CurrentlyUsable, TargetType.CardsNotDisplaced), 6,
                            new EffectDuration(Condition.of(ConditionType.AftermathPhaseEnds, TargetType.Gamestate, 1), 2)),
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
                            EffectChange.duration(EffectChangeType.Reveal, TargetType.Self, 1,
                                new EffectDuration(Condition.of(ConditionType.AftermathPhaseEnds, TargetType.Gamestate, 1), 1))
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.PlaceUsingHand, List.of(TargetType.CardsInHand, TargetType.CardsYouUse, TargetType.RitualCards), 1)
                        ))
                    ),
                    Effect.ACTION("""
                            Chew
                            If the target's health is higher than their original health halve the target's health, rounded up.
                            1 damage.
                            """,
                        new ArrayList<EffectChange>(List.of(
                            Target.pattern(
                                new Boolean[][] {
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, true , false, false, false},
                                    {false, false, true , false, true , false, false},
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

            // Rideable card test 1
            case 11 -> {
                rarity = CardRarity.COMMON;
                cardArt = "Card Placeholder.jpg";
                name = "Raptor";
                isToken = false;
                isDecisive = false;
                type = CardType.ATTACK;
                attribute = CardAttribute.Beast;
                health = 2;

                placeCost = Effect.BASIC_MATERIAL(CardType.ATTACK, 2);

                effects = new ArrayList<Effect>(List.of(
                    Effect.RIDEABLE(2),
                    Effect.ACTION("""
                            Chew Charge
                            If a card you don't control is in front of this card → move this card forwards 1 zone.
                            1 damage.
                            """,
                        new ArrayList<EffectChange>(List.of(
                            Target.pattern(
                                new Boolean[][] {
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, true , false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false}
                                }
                            )
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.conditional(Condition.CardInFrontOfThis(),
                                EffectChangeType.MoveForwards, TargetType.Self, 1),
                            EffectChange.of(EffectChangeType.Damage, TargetType.Target, 2)
                        ))
                    )
                ));
            }

            // Rideable card test 2
            case 12 -> {
                rarity = CardRarity.COMMON;
                cardArt = "Card Placeholder.jpg";
                name = "Trade Elephant";
                isToken = false;
                isDecisive = false;
                type = CardType.ATTACK;
                attribute = CardAttribute.Beast;
                health = 7;

                placeCost = Effect.PLACE_COST("6 Material (Must be placed backline)",
                    List.of( EffectChange.of(EffectChangeType.PayMaterial, TargetType.UsingCommander, 6)),
                    List.of( EffectChange.of(EffectChangeType.selfPlacementEffect, Target.YourBackline()))
                );

                effects = new ArrayList<Effect>(List.of(
                    Effect.RIDEABLE("Rideable: must be beast, discard 1 card.",
                        List.of(TargetType.BeastCards),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.Discard, List.of(TargetType.CardsInHand, TargetType.CardsYouUse), 1)
                        ))
                    ),
                    Effect.FIELD_TRIGGER("If the combat phase begins and this card has a rider → " +
                        "move this card left or right, and if it does, this card gains 1 Travel Counter.",
                        new ArrayList<Condition>(List.of(
                            Condition.Timing(ConditionType.CombatPhaseBegins),
                            Condition.of(ConditionType.MinThings, Target.thisRodeCard(), 1)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.MoveLeftOrRight, TargetType.Self, 1),
                            EffectChange.conditional(Condition.AndIfYouDo(), EffectChangeType.AddTravelCounters, TargetType.Self, 1)
                        ))
                    ),
                    Effect.FIELD_ACTIVATABLE("Once per turn, remove 4 Travel Counters from this card → draw 1 card.",
                        new ArrayList<Condition>(List.of(
                            Condition.UsesPerTurn(1)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.RemoveTravelCounters, TargetType.Self, 4)
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.Draw, TargetType.UsingCommander, 1)
                        ))
                    ),
                    Effect.ACTION("""
                            Trample
                            2 damage.
                            If this attack destroys a card → this card gains 1 Travel Counter.
                            """,
                        new ArrayList<EffectChange>(List.of(
                            Target.pattern(
                                new Boolean[][] {
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, true , false, true , false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false},
                                    {false, false, false, false, false, false, false}
                                }
                            )
                        )),
                        new ArrayList<EffectChange>(List.of(
                            EffectChange.of(EffectChangeType.Damage, TargetType.Target, 2),
                            EffectChange.conditional(Condition.TargetDestroyed(), EffectChangeType.AddTravelCounters, TargetType.Self, 1)
                        ))
                    )
                ));
            }

            default -> throw new IllegalArgumentException("Unexpected Card ID: " + cardID);
        }
        // cardArt = name + ".png";
    }

    /** Returns a token's cardID based on its referenced EffectChangeType */
    public static int getTokenID(EffectChangeType tokenPlacement) {
        return switch(tokenPlacement) {
            case PlaceConspiracyToken -> 7;
            default -> throw new IllegalArgumentException("Token type (" + tokenPlacement.toString().substring(5) + ") not yet implemented.");
        };
    }

    public static String integerToName(int name) {
        try {
            return new CardDefinition(name).name;
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Invalid cardID (" + name + ") for integerToName");
        }
    }

    /// Really inefficient. Should REALLY be in a database.
    public static int nameToInteger(String name) {
        CardDefinition check;
        for (int i = 0; i < maxID(); i++) {
            check = new CardDefinition(i);
            if (check.name.equals(name)) return i;
        }
        throw new IllegalArgumentException("Invalid cardID (" + name + ") for integerToName");
    }

    public static int maxID() {
        return 12;
    }
}
