package men.groupiron;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class CollectionLogManager {
    @Inject
    Client client;

    private final Map<String, DataState> collections = new HashMap<>();
    private String playerName;
    private Set<String> consumedNewItems = null;
    private Set<String> newItems = null;
    private final int collectionLogTabVarbit = 6905;
    static final Pattern COLLECTION_LOG_COUNT_PATTERN = Pattern.compile(".+:(.+)");

    public void updateCollection(ItemContainerState containerState) {
        Widget collectionLogHeader = client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_HEADER);
        if (collectionLogHeader == null || collectionLogHeader.isHidden()) return;
        Widget[] collectionLogHeaderChildren = collectionLogHeader.getChildren();
        if (collectionLogHeaderChildren == null || collectionLogHeaderChildren.length == 0) return;

        // Get the completion count information from the lines in the collection log header
        List<Integer> completionCounts = new ArrayList<>();
        for (int i = 2; i < collectionLogHeaderChildren.length; ++i) {
            String text = Text.removeTags(collectionLogHeaderChildren[i].getText());
            Matcher matcher = COLLECTION_LOG_COUNT_PATTERN.matcher(text);
            if (matcher.find()) {
                try {
                    Integer count = Integer.valueOf(matcher.group(1).trim());
                    completionCounts.add(count);
                } catch(Exception ignored) {}
            }
        }

        // Make sure the collection log page being shown is where these items came from
        if (areItemsOnPage(containerState)) {
            String pageName = Text.removeTags(collectionLogHeaderChildren[0].getText()).trim();
            // Sending the tab index just in case the page name is not unique across them
            int tabIdx = client.getVarbitValue(collectionLogTabVarbit);
            DataState pageDataState = collections.computeIfAbsent(pageName + tabIdx, k -> new DataState());

            pageDataState.update(new CollectionPageState(tabIdx, pageName, containerState, completionCounts));
        }
    }

    private boolean areItemsOnPage(ItemContainerState itemContainerState) {
        Widget collectionLogItems = client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_ITEMS);
        if (collectionLogItems == null || collectionLogItems.isHidden()) return false;
        Widget[] itemWidgets = collectionLogItems.getChildren();
        if (itemWidgets == null) return false;

        Set<Integer> containerItemIds = new HashSet<>(itemContainerState.getItemMap().keySet());
        Set<Integer> itemIdsOnPage = new HashSet<>();
        for (Widget itemWidget : itemWidgets) {
            itemIdsOnPage.add(itemWidget.getItemId());
        }

        return itemIdsOnPage.containsAll(containerItemIds);
    }

    public synchronized void updateNewItem(String item) {
        String playerName = client.getLocalPlayer().getName();
        if (playerName != null) {
            if (!playerName.equals(this.playerName) || newItems == null) {
                this.playerName = playerName;
                newItems = new HashSet<>();
            }
            newItems.add(item.trim());
        }
    }

    public synchronized void consumeNewItems(Map<String, Object> output) {
        if (newItems != null && output.get("name").equals(this.playerName)) {
            output.put("collection_log_new", newItems);
        }
        consumedNewItems = newItems;
        newItems = null;
    }

    public void consumeCollections(Map<String, Object> output) {
        if (collections.isEmpty()) return;
        List<Object> collectionLogOutput = new ArrayList<>();
        String whoIsUpdating = (String) output.get("name");

        for (DataState pageDataState : collections.values()) {
            Object result = pageDataState.consumeState(whoIsUpdating);
            if (result != null) {
                collectionLogOutput.add(result);
            }
        }

        // log.info("collectionLogOutput={}", collectionLogOutput);
        if (!collectionLogOutput.isEmpty()) {
            output.put("collection_log", collectionLogOutput);
        }
    }

    public void restoreCollections() {
        for (DataState pageDataState : collections.values()) {
            pageDataState.restoreState();
        }
    }

    public synchronized void restoreNewCollections() {
        if (consumedNewItems == null) return;
        for (String item : consumedNewItems) {
            updateNewItem(item);
        }
        consumedNewItems = null;
    }
}
