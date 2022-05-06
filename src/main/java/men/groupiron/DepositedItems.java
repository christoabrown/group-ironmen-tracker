package men.groupiron;

import java.util.Map;

public class DepositedItems {
    private ItemContainerState items = null;
    private ItemContainerState consumedItems = null;

    public DepositedItems() {
    }

    public synchronized void update(ItemContainerState deposited) {
        if (deposited == null) return;
        if (items == null || !deposited.whoOwnsThis().equals(items.whoOwnsThis())) {
            items = deposited;
        } else {
            items = items.add(deposited);
        }
    }

    public synchronized void consumeState(Map<String, Object> output) {
        if (items != null) {
            final String whoOwnsThis = items.whoOwnsThis();
            final String whoIsUpdating = (String) output.get("name");
            if (whoOwnsThis != null && whoOwnsThis.equals(whoIsUpdating)) output.put("deposited", items.get());
        }
        consumedItems = items;
        items = null;
    }

    public synchronized void restoreState() {
        if (consumedItems == null) return;
        if (items != null) {
            items = items.add(consumedItems);
        } else {
            items = consumedItems;
        }
        consumedItems = null;
    }

    public synchronized void reset() {
        items = null;
        consumedItems = null;
    }
}
