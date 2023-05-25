package men.groupiron;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.ItemComposition;
import net.runelite.api.StructComposition;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.StringUtils;

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
    @Inject
    ItemManager itemManager;

    private final Map<String, DataState> collections = new HashMap<>();
    private String playerName;
    private Set<String> consumedNewItems = null;
    private Set<String> newItems = null;
    private static final int collectionLogTabVarbit = 6905;
    private static final int collectionLogPageVarbit = 6906;
    static final Pattern COLLECTION_LOG_COUNT_PATTERN = Pattern.compile(".+:(.+)");
    static Map<String, Set<Integer>> pageItems;
    static Map<Integer, Map<Integer, String>> pageNameLookup;
    private static final List<Integer> COLLECTION_LOG_TAB_STRUCT_IDS = ImmutableList.of(
            471, // Bosses
            472, // Raids
            473, // Clues
            474, // Minigames
            475  // Other
    );
    private static final int COLLECTION_LOG_PAGE_NAME_PARAM_ID = 689;
    private static final int COLLECTION_LOG_TAB_ENUM_PARAM_ID = 683;
    private static final int COLLECTION_LOG_PAGE_ITEMS_ENUM_PARAM_ID = 690;

    public void initCollectionLog()
    {
        // NOTE: varbit 6905 gives us the selected collection log tab index and 6906 is the selected page index.
        // In here we build a lookup map which will give us the page name with the tab index and the page index.
        // This should be better than pulling the page name from the widget since that value can be changed by
        // other runelite plugins.
        // We also create a lookup of the item ids to the page name which should be better than using the item id
        // in the collection log window as these will match the ids in the container state change.
        pageItems = new HashMap<>();
        pageNameLookup = new HashMap<>();
        int tabIdx = 0;
        for (Integer structId : COLLECTION_LOG_TAB_STRUCT_IDS) {
            StructComposition tabStruct = client.getStructComposition(structId);
            int tabEnumId = tabStruct.getIntValue(COLLECTION_LOG_TAB_ENUM_PARAM_ID);
            EnumComposition tabEnum = client.getEnum(tabEnumId);
            Map<Integer, String> pageIdToName = pageNameLookup.computeIfAbsent(tabIdx, k -> new HashMap<>());

            int pageIdx = 0;
            for (Integer pageStructId : tabEnum.getIntVals()) {
                StructComposition pageStruct = client.getStructComposition(pageStructId);
                String pageName = pageStruct.getStringValue(COLLECTION_LOG_PAGE_NAME_PARAM_ID);
                int pageItemsEnumId = pageStruct.getIntValue(COLLECTION_LOG_PAGE_ITEMS_ENUM_PARAM_ID);
                EnumComposition pageItemsEnum = client.getEnum(pageItemsEnumId);

                pageIdToName.put(pageIdx, pageName);
                Set<Integer> items = pageItems.computeIfAbsent(pageName, k -> new HashSet<>());

                for (Integer pageItemId : pageItemsEnum.getIntVals()) {
                    ItemComposition itemComposition = itemManager.getItemComposition(pageItemId);
                    items.add(itemComposition.getId());
                }

                ++pageIdx;
            }

            ++tabIdx;
        }
    }

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

        int tabIdx = client.getVarbitValue(collectionLogTabVarbit);
        int pageIdx = client.getVarbitValue(collectionLogPageVarbit);
        String pageName = getPageName(tabIdx, pageIdx);
        // Make sure the collection log page being shown is where these items came from
        if (!StringUtils.isBlank(pageName) && areItemsOnPage(pageName, containerState)) {
            // Sending the tab index just in case the page name is not unique across them
            DataState pageDataState = collections.computeIfAbsent(pageName + tabIdx, k -> new DataState());
            pageDataState.update(new CollectionPageState(tabIdx, pageName, containerState, completionCounts));
        }
    }

    private String getPageName(int tabIdx, int pageIdx) {
        Map<Integer, String> x = pageNameLookup.get(tabIdx);
        if (x != null) return x.get(pageIdx);
        return null;
    }

    private boolean areItemsOnPage(String pageName, ItemContainerState itemContainerState) {
        if (StringUtils.isBlank(pageName)) return false;
        Set<Integer> itemIdsOnPage = pageItems.computeIfAbsent(pageName, k -> new HashSet<>());

        Set<Integer> containerItemIds = new HashSet<>(itemContainerState.getItemMap().keySet());
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
