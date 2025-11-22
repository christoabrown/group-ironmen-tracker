package men.groupiron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
public class CollectionLogV2Manager {
    // item id -> quantity
    private final Map<Integer, Integer> clogItems = new HashMap<>();

    public synchronized void storeClogItem(int itemId, int quantity) {
        if (quantity <= 0) return;
        clogItems.put(itemId, quantity);
    }

    public synchronized void consumeClogItems(Map<String, Object> updates) {
        if (clogItems.isEmpty()) return;
        List<Integer> result = new ArrayList<>(clogItems.size() * 2);
        for (Map.Entry<Integer, Integer> item : clogItems.entrySet()) {
            result.add(item.getKey());
            result.add(item.getValue());
        }
        updates.put("collection_log_v2", result);
    }

    public synchronized void clearClogItems() {
        clogItems.clear();
    }

    @Subscribe
    public synchronized void onGameStateChanged(GameStateChanged ev) {
        if (ev.getGameState() != GameState.LOGGED_IN) {
            clogItems.clear();
        }
    }
}
