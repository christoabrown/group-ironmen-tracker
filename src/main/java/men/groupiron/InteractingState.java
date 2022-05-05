package men.groupiron;

import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

public class InteractingState implements ConsumableState {
    private final transient String playerName;
    @Getter
    private final String name;
    @Getter
    private final int scale;
    @Getter
    private final int ratio;
    @Getter
    private final LocationState location;

    public InteractingState(String playerName, Actor actor, Client client) {
        this.playerName = playerName;
        this.scale = actor.getHealthScale();
        this.ratio = actor.getHealthRatio();
        this.name = actor.getName();

        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, actor.getLocalLocation());
        this.location = new LocationState(playerName, worldPoint);
    }

    @Override
    public Object get() {
        return this;
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof InteractingState)) return false;

        // NOTE: For interactions, we want to keep sending the data until the player stops interacting
        // even if nothing changed about what is being interacted with. The UI will handle not showing
        // the interaction once it goes stale from the player not interacting with anything.
        return false;
    }
}
