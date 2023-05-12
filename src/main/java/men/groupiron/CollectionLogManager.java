package men.groupiron;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Singleton
public class CollectionLogManager {
    @Inject
    Client client;

    private final Map<Integer, Map<Integer, DataState>> collections = new HashMap<>();
    private String playerName;
    private Set<String> newItems = new HashSet<>();
    private final int collectionLogTabVarbit = 6905;
    private final int collectionLogPageVarbit = 6906;

    public void updateCollection(ItemContainerState containerState) {
        if (!containerState.isEmpty()) {
            int tab = client.getVarbitValue(collectionLogTabVarbit);
            int page = client.getVarbitValue(collectionLogPageVarbit);
            Map<Integer, DataState> collectionTab = collections.computeIfAbsent(tab, k -> new HashMap<>());
            DataState pageDataState = collectionTab.computeIfAbsent(page, k -> new DataState(String.valueOf(page), false));
            pageDataState.update(containerState);
        }
        // log.info("collections={}", collections);
    }

    public synchronized void updateNewItem(String item) {
        String playerName = client.getLocalPlayer().getName();
        if (playerName != null) {
            if (!playerName.equals(this.playerName)) {
                newItems = new HashSet<>();
            }
            newItems.add(item.trim());
        }
    }

    public synchronized void consumeNewItems(Map<String, Object> output) {
        if (output.get("name").equals(this.playerName)) {
            output.put("collection_log_new", newItems);
        }
        newItems = new HashSet<>();
    }

    public void consumeCollections(Map<String, Object> output) {
        Map<String, Object> collectionLogOutput = new HashMap<>();

        for (Integer tab : collections.keySet()) {
            Map<String, Object> collectionTabOutput = new HashMap<>();
            Map<Integer, DataState> collectionTab = collections.get(tab);
            for (Integer page : collectionTab.keySet()) {
                DataState pageDataState = collectionTab.get(page);
                pageDataState.consumeState((String) output.get("name"), collectionTabOutput);
            }

            // log.info("collectionTabOutput={}", collectionTabOutput);
            if (!collectionTabOutput.isEmpty()) {
                collectionLogOutput.put(String.valueOf(tab), collectionTabOutput);
            }
        }

        // log.info("collectionLogOutput={}", collectionLogOutput);
        if (!collectionLogOutput.isEmpty()) {
            output.put("collection_log", collectionLogOutput);
        }
    }
}
