package ew.server;

import ew.engine.resolver.GameChoice;
import ew.engine.resolver.GameChoiceChange;
import ew.playerData.Commander;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/** Class that saves future actions the commander will make. It is Match's only method of communication to commanders. */
public class ControllerInstance {
    private final SynchronousQueue<List<Integer>> responses = new SynchronousQueue<>();

    volatile List<Integer> nextNumber;
    volatile List<GameChoice> nextChoice;
    volatile List<List<GameChoiceChange>> nextChangeChoice; // Some individual changes require multiple choices, which aren't related to previous choices.
    WaitType waitType = WaitType.None;
    int effectTime = 0; // Set by the match for the length the controller can take on one specific effect
    int phaseTime = 0; // Set by the match for the length the controller can take during one entire phase
    final Commander commander;
    final Room room;

    public void setMaxEffectTime(int effectTime) { this.effectTime = effectTime; }
    public void setMaxPhaseTime(int phaseTime)   { this.phaseTime = phaseTime; }

    /** Server delivers the player's answer here. */
    public void setChoice(List<Integer> choice) {
        try {
            responses.put(choice); // hands off to the parked engine thread
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted delivering choice", e);
        }
    }

    public Commander getCommander() { return commander; }
    public Room getRoom() { return room; }
    public int getEffectTime() { return effectTime; }
    public int getPhaseTime() { return phaseTime; }
    public WaitType getWaitType() { return waitType; }

    /**
     * The player is expected to respond in a List of Integer whose size is 1
     * with the index of which number they intend.
     */
    public List<Integer> getNextNumber() { return nextNumber; }
    /**
     * The player is expected to respond in a List of Integer whose size is 1
     * with the index of which choice they intend to use.
     */
    public List<GameChoice> getNextChoice() { return nextChoice; }
    /** For getNextChange, the player is expected to respond in a List of Integer that correspond to:
        List.of(
            # // Index of GameChoiceChange to use
            #... // Index of every choice for that gameChoiceChange
        )
    **/
    public List<List<GameChoiceChange>> getNextChange() { return nextChangeChoice; }


    public ControllerInstance(Room room, Commander controller) {
        this.commander = controller;
        this.room = room;
    }

    public GameChoice addChoice(List<GameChoice> availableChoices) {
        this.nextChoice = availableChoices;
        this.waitType = WaitType.Choice;
        final List<Integer> response = awaitResponse();

        this.nextChoice = null;
        this.waitType = WaitType.None;
        return availableChoices.get(response.get(0));
    }

    public int addNumChoice(List<Integer> numbers) {
        this.nextNumber = numbers;
        this.waitType = WaitType.Number;
        final List<Integer> response = awaitResponse();

        this.nextNumber = null;
        this.waitType = WaitType.None;
        return numbers.get(response.get(0));
    }

    /** From a given List(List(GameChoiceChange)), where the first list is
     * the number of choices to be made, and the second list is the different
     * available choices, returns a List of every choice made.
     */
    public List<GameChoiceChange> addChangeChoice(List<List<GameChoiceChange>> availableChoices) {
        this.waitType = WaitType.ChangeChoice;
        GameChoiceChange[] changes = new GameChoiceChange[availableChoices.size()];
        // Add choices that don't need to be selected.
        for (int i = 0; i < availableChoices.size(); i++) {
            if (availableChoices.get(i).size() == 1
                && availableChoices.get(i).get(0).getChange().getType().getSimultNum() != 2) // Target
                changes[i] = availableChoices.get(i).get(0);
        }

        // Get choices that need to be selected
        List<List<GameChoiceChange>> neededChoices = new ArrayList<>();
        for (int i = 0; i < changes.length; i++) {
            if (changes[i] == null) neededChoices.add(availableChoices.get(i));
        }

        this.nextChangeChoice = neededChoices;
        final List<Integer> response = awaitResponse();

        // With the selected choices, add them back to changes
        int nextChange = 0;
        for (int i = 0; i < changes.length; i++) {
            if (changes[i] == null) {
                changes[i] = nextChangeChoice.get(nextChange).get(response.get(nextChange));
                ++nextChange;
            }
        }

        this.nextChangeChoice = null;
        this.waitType = WaitType.None;
        return List.of(changes);
    }

    /** Waits until server delivers a response. */
    private List<Integer> awaitResponse() {
        try {
            List<Integer> r = responses.poll(effectTime * 1000L, TimeUnit.MILLISECONDS);
            return (r != null) ? r : handleTimeout();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted awaiting player choice", e);
        }
    }

    /** If a player times out, they choose "Pass", if available, and if not, then they choose a random choice. */
    private List<Integer> handleTimeout() {
        switch (this.waitType) {
            case Number -> {
                return List.of(Math.toIntExact((Math.round(Math.random() * (nextNumber.size()-1)))));
            }
            case Choice -> {
                for (int i = 0; i < nextChoice.size(); i++) {
                    if (Objects.equals(nextChoice.get(i), GameChoice.PASS())) return List.of(i);
                }
                return List.of(
                    0  // The first index of every choice is NOT_USED, which is considered passing during the build phase
                );
            }
            // Changes cannot be passed.
            case ChangeChoice -> {
                return List.of(
                    0, // The first index of every choiceChange is FAILED_USE
                    0  // FAILED_USE has one option.
                );
            }
            default -> throw new IllegalArgumentException("How did we get here?");
        }
    }
}
