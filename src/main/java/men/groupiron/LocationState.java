package men.groupiron;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public class LocationState implements ConsumableState {
    @Getter
    private final int x;
    @Getter
    private final int y;
    @Getter
    private final int plane;

    LocationState(Client client) {
        Player player = client.getLocalPlayer();
        assert player != null;
        LocalPoint localPoint = player.getLocalLocation();
        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
        x = worldPoint.getX();
        y = worldPoint.getY();
        plane = worldPoint.getPlane();
    }

    @Override
    public Object get() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LocationState)) return false;

        LocationState other = (LocationState) o;
        return (x == other.x) && (y == other.y) && (plane == other.plane);
    }
}
