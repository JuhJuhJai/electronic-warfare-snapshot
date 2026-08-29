package ew.playerData;

import ew.server.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.random.RandomGenerator;

/** This is an information class that the server uses. */
public class Commander {
    final long playerID;
    String name;
    final CardCollection collection;
    final ArrayList<Deck> savedDecks;
    int datagrams = 0;
    int selectedDeck = 0;
    int wins = 0;
    int losses = 0;
    int draws = 0;

    /** Returns a commander which has the starter collection and starter decks. */
    public static Commander starter(long playerID, String name) {

    }

    /** Returns a commander which has a random collection of 60 cards and a deck of 40-60 cards from that collection. */
    public static Commander random(long playerID, RandomGenerator random) {

    }

    /** Returns a commander which has a collection every card in the deck and a deck of the given deck. */
    public static Commander givenDeck(long playerID, Deck deck) {

    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Commander)) return false;
        return ((Commander) obj).getPlayerID() == playerID;
    }

    public String toString() {
        return name + "(" + wins + " - " + losses + " - " + draws + ")";
    }

    public Commander(long playerID, String name) {
        this.playerID = playerID;
        this.name = name;
        this.collection = new CardCollection(playerID);
        this.savedDecks = new ArrayList<>();
    }

    public Commander(long playerID, String name, CardCollection collection, ArrayList<Deck> savedDecks) {
        this.playerID = playerID;
        this.name = name;
        this.collection = collection;
        this.savedDecks = savedDecks;
    }

    public long getPlayerID() { return playerID; }
    public CardCollection getCardCollection() { return collection; }
    public String getName() { return name; }
    public List<Deck> getSavedDecks() { return savedDecks; }
    public int getSelectedDeck() { return selectedDeck; }
    public int getDatagrams() { return datagrams; }

    public void setName(String newName) { this.name = newName; }
    public void setSelectedDeck(int newSelection) { this.selectedDeck = newSelection; }
    public void setDatagrams(int datagrams) { this.datagrams = datagrams; }
    public void incrementWins() { this.wins++; }
    public void incrementLoss() { this.losses++; }
    public void incrementDraws() { this.draws++; }
}
