package men.groupiron;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Skill;

public class ResourcesState implements ConsumableState {
    private static class CurrentMax {
        @Getter
        private final int current;
        @Getter
        private final int max;

        CurrentMax(int current, int max) {
            this.current = current;
            this.max = max;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof CurrentMax)) return false;

            CurrentMax other = (CurrentMax) o;
            return other.getCurrent() == current && other.getMax() == max;
        }
    }

    @Getter
    private final CurrentMax hitpoints;
    @Getter
    private final CurrentMax prayer;
    @Getter
    private final CurrentMax energy;
    @Getter
    int world;
    private transient final String playerName;

    ResourcesState(String playerName, Client client) {
        this.playerName = playerName;
        hitpoints = new CurrentMax(
                client.getBoostedSkillLevel(Skill.HITPOINTS),
                client.getRealSkillLevel(Skill.HITPOINTS)
        );
        prayer = new CurrentMax(
                client.getBoostedSkillLevel(Skill.PRAYER),
                client.getRealSkillLevel(Skill.PRAYER)
        );
        energy = new CurrentMax(
                client.getEnergy(),
                100
        );
        world = client.getWorld();
    }

    @Override
    public Object get() {
        return new int[] {
                hitpoints.current, hitpoints.max,
                prayer.current, prayer.max,
                energy.current, energy.max,
                world
        };
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ResourcesState)) return false;

        ResourcesState other = (ResourcesState) o;
        return other.world == world && other.hitpoints.equals(hitpoints) && other.prayer.equals(prayer) && other.energy.equals(energy);
    }
}
