package men.groupiron;

import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.ScriptID;
import net.runelite.client.game.ItemManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PotionStorageState implements ConsumableState {
    private final List<ItemContainerItem> items;
    private final transient String playerName;

    private PotionStorageState(String playerName, List<ItemContainerItem> items) {
        this.playerName = playerName;
        this.items = items;
    }

    @Nullable
    public static PotionStorageState fromClient(String playerName, Client client, ItemManager itemManager) {
        List<ItemContainerItem> items = new ArrayList<>();

        for (EnumComposition e : new EnumComposition[]{
            client.getEnum(EnumID.POTIONSTORE_POTIONS),
            client.getEnum(EnumID.POTIONSTORE_UNFINISHED_POTIONS)
        }) {
            if (e == null) continue;

            for (int potionEnumId : e.getIntVals()) {
                EnumComposition potionEnum = client.getEnum(potionEnumId);
                if (potionEnum == null) continue;

                client.runScript(ScriptID.POTIONSTORE_DOSES, potionEnumId);
                int doses = client.getIntStack()[0];

                if (doses > 0) {
                    // Canonicalize the 1-dose variant's item ID so the potion type has a
                    // consistent identifier regardless of how many doses are currently stored.
                    int canonicalOneDoseItemId = itemManager.canonicalize(potionEnum.getIntValue(1));
                    int quantity = doses;
                    if (canonicalOneDoseItemId > 0 && quantity > 0) {
                        items.add(new ItemContainerItem(canonicalOneDoseItemId, quantity));
                    }
                }
            }
        }

        items.sort(Comparator.comparingInt(ItemContainerItem::getId));
        return new PotionStorageState(playerName, items);
    }

    @Override
    public Object get() {
        List<Integer> result = new ArrayList<>(items.size() * 2);
        for (ItemContainerItem item : items) {
            result.add(item.getId());
            result.add(item.getQuantity());
        }
        return result;
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof PotionStorageState)) return false;
        PotionStorageState other = (PotionStorageState) o;
        if (other.items.size() != items.size()) return false;

        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).equals(other.items.get(i))) return false;
        }

        return true;
    }
}
