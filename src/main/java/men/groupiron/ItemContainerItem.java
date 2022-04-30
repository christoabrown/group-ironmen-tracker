package men.groupiron;

import lombok.Getter;

public class ItemContainerItem {
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
