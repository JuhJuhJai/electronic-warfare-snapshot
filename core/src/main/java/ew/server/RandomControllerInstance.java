package ew.server;

import ew.engine.resolver.GameChoice;
import ew.engine.resolver.GameChoiceChange;
import ew.playerData.Commander;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public class RandomControllerInstance extends ControllerInstance {
    final RandomGenerator random;

    public RandomControllerInstance(Room room, Commander controller, RandomGenerator random) {
        super(room, controller);
        this.random = random;
    }

    /** Adds available choices to this NextChoice list, then waits for the Server to check its nextChoice and set its choice integer. May only choose 1 choice. */
    public GameChoice addChoice(List<GameChoice> availableChoices) {
        return availableChoices.get(random.nextInt(0, availableChoices.size() - 1));
    }

    /** Adds available choiceChange to this NextChoice list, then waits for the Server to check its nextChangeChoice and set its choice integer. */
    public List<GameChoiceChange> addChangeChoice(List<List<GameChoiceChange>> availableChoices) {
        List<GameChoiceChange> choices = new ArrayList<>();
        for (List<GameChoiceChange> availableChoice : availableChoices) {
            choices.add(availableChoice.get(random.nextInt(0, availableChoice.size() - 1)));
        }
        return choices;
    }

    /** Adds available Integer to this nextNumber list, then waits for the Server to check its nextNumber and set its choice integer. May only choose 1 number. */
    public int addNumChoice(List<Integer> numbers) {
        return numbers.get(random.nextInt(0, numbers.size() -1));
    }
}
