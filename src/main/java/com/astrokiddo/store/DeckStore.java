
package com.astrokiddo.store;

import com.astrokiddo.model.LessonDeck;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeckStore {
    private final Map<String, LessonDeck> store = new ConcurrentHashMap<>();

    public void save(LessonDeck d) {
        store.put(d.getId(), d);
    }

    public Optional<LessonDeck> get(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
