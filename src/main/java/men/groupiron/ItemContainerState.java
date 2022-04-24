package men.groupiron;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ItemContainerState implements ConsumableState {
    private static class ItemContainerItem {
        @Getter
        private final int id;
        @Getter
        private final int quantity;

        ItemContainerItem(int id, int quantity) {
            this.id = id;
            this.quantity = quantity;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof ItemContainerItem)) return false;
            ItemContainerItem other = (ItemContainerItem) o;
            return other.id == id && other.quantity == quantity;
        }
    }

    private final List<ItemContainerItem> items;
    private transient final String playerName;

    public ItemContainerState(String playerName, ItemContainer container, ItemManager itemManager) {
        this.playerName = playerName;
        items = new ArrayList<>();
        Item[] contents = container.getItems();
        for (final Item item : contents) {
            addItemIfValid(item, itemManager);
        }
    }

    // NOTE: This is for when we care about the order of the items in the container
    // like the player inventory and equipment.
    public ItemContainerState(String playerName, ItemContainer container, ItemManager itemManager, int containerSize) {
        this.playerName = playerName;
        items = new ArrayList<>();
        for (int i = 0; i < containerSize; i++) {
            Item item = container.getItem(i);
            if (item == null) {
                items.add(new ItemContainerItem(0, 0));
            } else {
                addItemIfValid(item, itemManager);
            }
        }
    }

    private void addItemIfValid(Item item, ItemManager itemManager) {
        final int id = item.getId();
        final int quantity = item.getQuantity();
        if (itemManager != null && id >= 0 && quantity > 0) {
            final int canonicalId = itemManager.canonicalize(id);
            items.add(new ItemContainerItem(canonicalId, quantity));
        }
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
