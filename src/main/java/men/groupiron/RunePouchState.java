package men.groupiron;

import net.runelite.api.Client;
import net.runelite.api.Varbits;

public class RunePouchState implements ConsumableState {
    private final ItemContainerItem rune1;
    private final ItemContainerItem rune2;
    private final ItemContainerItem rune3;
    private final transient String playerName;

    public RunePouchState(String playerName, Client client) {
        this.playerName = playerName;
        rune1 = new ItemContainerItem(client.getVarbitValue(Varbits.RUNE_POUCH_RUNE1), client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT1));
        rune2 = new ItemContainerItem(client.getVarbitValue(Varbits.RUNE_POUCH_RUNE2), client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT2));
        rune3 = new ItemContainerItem(client.getVarbitValue(Varbits.RUNE_POUCH_RUNE3), client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT3));
    }

    @Override
    public Object get() {
        return new ItemContainerItem[]{rune1, rune2, rune3};
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof RunePouchState)) return false;
        RunePouchState other = (RunePouchState) o;

        return rune1.equals(other.rune1) && rune2.equals(other.rune2) && rune3.equals(other.rune3);
    }
}
