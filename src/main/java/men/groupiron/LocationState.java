package men.groupiron;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

public class LocationState implements ConsumableState {
    @Getter
    private final int x;
    @Getter
    private final int y;
    @Getter
    private final int plane;
    @Getter
    private final boolean isOnBoat;
    private transient final String playerName;

    LocationState(String playerName, WorldPoint worldPoint, boolean isOnBoat) {
        this.playerName = playerName;
        x = worldPoint.getX();
        y = worldPoint.getY();
        plane = worldPoint.getPlane();
        this.isOnBoat = isOnBoat;
    }

    @Override
    public Object get() {
        return new int[] { x, y, plane, isOnBoat ? 1 : 0 };
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LocationState)) return false;

        LocationState other = (LocationState) o;
        return (x == other.x) && (y == other.y) && (plane == other.plane) && (isOnBoat == other.isOnBoat);
    }

    @Override
    public String toString() {
        return String.format("{ x: %d, y: %d, plane: %d, isOnBoat: %b }", x, y, plane, isOnBoat);
    }
}
