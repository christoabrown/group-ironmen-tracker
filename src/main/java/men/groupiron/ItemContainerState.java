package men.groupiron;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;

import java.util.ArrayList;
import java.util.List;

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
