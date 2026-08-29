package ew.playerData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CardCollection {
    private final ConcurrentHashMap<CardStored, Integer> storage;
    private final long ownerID;

    public CardCollection(long ownerID) {
        this.ownerID = ownerID;
        storage = new ConcurrentHashMap<>();
    }

    public CardCollection(long ownerID, Map<CardStored, Integer> startingCollection) {
        this.ownerID = ownerID;
        this.storage = (ConcurrentHashMap<CardStored, Integer>) startingCollection;
    }

    public ConcurrentHashMap<CardStored, Integer> getStorage() { return storage; }

    public void addCard(CardStored card) { storage.put(card, 1); }
    public void addCard(CardStored card, int amount) { storage.put(card, amount); }
    public void addCards(Map<CardStored, Integer> cards) { storage.putAll(cards); }

    public void removeCard(CardStored card, int amount) {
        if (storage.get(card) - amount < 0) throw new RuntimeException("Unable to remove " + amount + " of " + card + " from " + ownerID + " CardCollection.");
        storage.replace(card, storage.get(card) - amount);
    }
}
