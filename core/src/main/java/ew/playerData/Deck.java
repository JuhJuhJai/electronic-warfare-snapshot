package ew.playerData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Deck {
    private final long owner;
    private String name;
    private final ConcurrentHashMap<CardStored, Integer> cards;

    public Deck(long owner, String name) {
        this.owner = owner;
        this.name = name;
        this.cards = new ConcurrentHashMap<>();
    }

    public Deck(long owner, String name, ConcurrentHashMap<CardStored, Integer> cards) {
        this.owner = owner;
        this.name = name;
        this.cards = cards;
    }

    public void addCard(CardStored card) {
        cards.put(card, cards.get(card) + 1);
    }
    public void removeCard(CardStored card) {
        cards.put(card, cards.get(card) - 1);
        if (cards.get(card) <= 0) cards.remove(card);
    }

    public String getName() { return name; }
    public int getSize() { return cards.size(); }
    public Map<CardStored, Integer> getCards() { return cards; }

    public void changeName(String name) { this.name = name; }

    /**
     * Given a card collection, return true if every card (not counting modifier)
     * in the deck is also in the collection
     */
    public boolean allCardsOwned(CardCollection collection) {
        for (Map.Entry<CardStored, Integer> entry : collection.getStorage().entrySet()) {
            if (!cards.containsKey(entry.getKey()) || cards.get(entry.getKey()) <= 0) return false;
        }
        return true;
    }
}
