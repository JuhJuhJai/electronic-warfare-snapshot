package ew.server;

import ew.engine.cards.effects.EffectChangeType;
import ew.engine.resolver.GameChoice;
import ew.engine.resolver.GameChoiceChange;
import ew.playerData.Commander;

import java.util.ArrayList;
import java.util.List;

/** Class that saves future actions the commander will make. It is Match's only method of communication to commanders. */
public class ControllerInstance {
    List<Integer> nextNumber;
    List<GameChoice> nextChoice;
    List<List<GameChoiceChange>> nextChangeChoice; // Some individual changes require multiple choices, which aren't related to previous choices.
    volatile List<Integer> choice;
    double time = 0;
    final Commander commander;
    final Room room;

    public void setTime(double time) { this.time = time; }
    public void setChoice(List<Integer> choice) { this.choice = choice; }

    public Commander getCommander() { return commander; }
    public Room getRoom() { return room; }
    public double getTime() { return time; }
    public List<Integer> getNextNumber() { return nextNumber; }
    public List<GameChoice> getNextChoice() { return nextChoice; }
    public List<List<GameChoiceChange>> getNextChange() { return nextChangeChoice; }

    public ControllerInstance(Room room, Commander controller) {
        this.commander = controller;
        this.room = room;
    }

    /** Returns 1 GameChoice from a list of GameChoice, which is chosen by the commander associated with this controller. */
    public GameChoice addChoice(List<GameChoice> availableChoices) {
        this.nextChoice = availableChoices;
        while (choice == null) {
            Thread.onSpinWait();
        }
        final GameChoice chosen = nextChoice.get(choice.get(0));
        nextChoice = null;
        choice = null;
        return chosen;
    }

    /** From a given List(List(GameChoiceChange)), where the first list is
     * the number of choices to be made, and the second list is the different
     * available choices, returns a List of every choice made.
     */
    public List<GameChoiceChange> addChangeChoice(List<List<GameChoiceChange>> availableChoices) {
        GameChoiceChange[] changes = new GameChoiceChange[availableChoices.size()];
        // Add choices that don't need to be selected.
        for (int i = 0; i < availableChoices.size(); i++) {
            if (availableChoices.get(i).size() == 1
                && availableChoices.get(i).get(0).getChange().getType() != EffectChangeType.UniqueTarget) changes[i] = availableChoices.get(i).get(0);
        }

        // Get choices that need to be selected
        List<List<GameChoiceChange>> neededChoices = new ArrayList<>();
        for (int i = 0; i < changes.length; i++) {
            if (changes[i] == null) neededChoices.add(availableChoices.get(i));
        }

        // Wait for server to set the choice from neededChoices.
        this.nextChangeChoice = neededChoices;
        while (choice == null) {
            Thread.onSpinWait();
        }

        // With the selected choices, add them back to changes
        int nextChange = 0;
        for (int i = 0; i < changes.length; i++) {
            if (changes[i] == null) {
                changes[i] = neededChoices.get(nextChange).get(choice.get(nextChange));
                ++nextChange;
            }
        }

        nextChangeChoice = null;
        choice = null;
        return List.of(changes);
    }

    /** Returns 1 number from a list of number, which is chosen by the commander associated with this controller. */
    public int addNumChoice(List<Integer> numbers) {
        this.nextNumber = numbers;
        while (choice == null) {
            Thread.onSpinWait();
        }
        final int chosen = nextNumber.get(choice.get(0));
        nextNumber = null;
        choice = null;
        return chosen;
    }
}
