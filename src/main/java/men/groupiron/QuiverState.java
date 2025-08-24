package men.groupiron;

import net.runelite.api.Client;
import net.runelite.api.VarPlayer;
import net.runelite.client.game.ItemManager;

public class QuiverState implements ConsumableState {
    private final ItemContainerItem ammo;
    private final String playerName;

    public QuiverState(String playerName, Client client, ItemManager itemManager) {
        this.playerName = playerName;

        int id = client.getVarpValue(VarPlayer.DIZANAS_QUIVER_ITEM_ID);
        int qty = client.getVarpValue(VarPlayer.DIZANAS_QUIVER_ITEM_COUNT);
        if (id <= 0 || qty <= 0) {
            this.ammo = new ItemContainerItem(0, 0);
        } else {
            int canonId = itemManager.canonicalize(id);
            this.ammo = new ItemContainerItem(canonId, qty);
        }
    }

    @Override
    public Object get() {
        return new int[] { ammo.getId(), ammo.getQuantity() };
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof QuiverState)) {
            return false;
        }

        QuiverState other = (QuiverState) o;
        
        return this.ammo.equals(other.ammo);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + ammo.getId();
        result = 31 * result + ammo.getQuantity();

        return result;
    }
}
