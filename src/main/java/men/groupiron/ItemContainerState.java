package men.groupiron;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ItemContainerState implements ConsumableState {
    private final List<ItemContainerItem> items;
    private transient final String playerName;

    public ItemContainerState(String playerName, ItemContainer container, ItemManager itemManager) {
        this.playerName = playerName;
        items = new ArrayList<>();
        Item[] contents = container.getItems();
        for (final Item item : contents) {
            if (isItemValid(item, itemManager)) {
                items.add(new ItemContainerItem(itemManager.canonicalize(item.getId()), item.getQuantity()));
            }
        }
    }

    // NOTE: This is for when we care about the order of the items in the container
    // like the player inventory and equipment.
    public ItemContainerState(String playerName, ItemContainer container, ItemManager itemManager, int containerSize) {
        this.playerName = playerName;
        items = new ArrayList<>();
        for (int i = 0; i < containerSize; i++) {
            Item item = container.getItem(i);
            if (!isItemValid(item, itemManager)) {
                items.add(new ItemContainerItem(0, 0));
            } else {
                items.add(new ItemContainerItem(itemManager.canonicalize(item.getId()), item.getQuantity()));
            }
        }
    }

    public ItemContainerState(String playerName, List<ItemContainerItem> items) {
        this.playerName = playerName;
        this.items = items;
    }

    @Nullable
    public ItemContainerState add(ItemContainerState itemsToAdd) {
        if (itemsToAdd == null || !itemsToAdd.whoOwnsThis().equals(whoOwnsThis())) return null;
        Map<Integer, ItemContainerItem> thisItems = getItemMap();
        Map<Integer, ItemContainerItem> otherItems = itemsToAdd.getItemMap();
        List<ItemContainerItem> result = new ArrayList<>();

        for (Integer itemId : thisItems.keySet()) {
            ItemContainerItem item = thisItems.get(itemId);
            if (otherItems.containsKey(itemId)) {
                item.addQuantity(otherItems.get(itemId).getQuantity());
            }
            result.add(item);
        }

        for (Integer itemId : otherItems.keySet()) {
            if (!thisItems.containsKey(itemId)) {
                result.add(otherItems.get(itemId));
            }
        }

        return new ItemContainerState(whoOwnsThis(), result);
    }

    @Nullable
    public ItemContainerState whatGotRemoved(ItemContainerState other) {
        if (other == null || !other.whoOwnsThis().equals(whoOwnsThis())) return null;
        Map<Integer, ItemContainerItem> thisItems = getItemMap();
        Map<Integer, ItemContainerItem> otherItems = other.getItemMap();
        List<ItemContainerItem> result = new ArrayList<>();

        for (Integer itemId : otherItems.keySet()) {
            ItemContainerItem otherItem = otherItems.get(itemId);
            if (otherItem.getId() == 0) continue;

            if (thisItems.containsKey(itemId)) {
                ItemContainerItem thisItem = thisItems.get(itemId);
                int quantityDifference = otherItem.getQuantity() - thisItem.getQuantity();
                if (quantityDifference > 0) {
                    result.add(new ItemContainerItem(itemId, quantityDifference));
                }
            } else {
                result.add(new ItemContainerItem(itemId, otherItem.getQuantity()));
            }
        }

        return new ItemContainerState(playerName, result);
    }

    public Map<Integer, ItemContainerItem> getItemMap() {
        Map<Integer, ItemContainerItem> itemMap = new HashMap<>();
        for (ItemContainerItem itemContainerItem : items) {
            Integer id = itemContainerItem.getId();
            if (itemMap.containsKey(id)) {
                itemMap.get(id).addQuantity(itemContainerItem.getQuantity());
            } else {
                itemMap.put(id, new ItemContainerItem(id, itemContainerItem.getQuantity()));
            }
        }
        return itemMap;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    private boolean isItemValid(Item item, ItemManager itemManager) {
        if (item == null) return false;
        final int id = item.getId();
        final int quantity = item.getQuantity();
        if (itemManager != null) {
            final boolean isPlaceholder = itemManager.getItemComposition(id).getPlaceholderTemplateId() != -1;

            return id >= 0 && quantity >= 0 && !isPlaceholder;
        }
        return false;
    }

    @Override
    public Object get() {
        return items;
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ItemContainerState)) return false;
        ItemContainerState other = (ItemContainerState) o;
        if (other.items.size() != items.size()) return false;

        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).equals(other.items.get(i))) return false;
        }

        return true;
    }
}
