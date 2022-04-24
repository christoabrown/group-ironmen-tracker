package men.groupiron;

import net.runelite.api.Client;
import net.runelite.api.Skill;

import java.util.HashMap;
import java.util.Map;

public class SkillState implements ConsumableState {
    private final Map<String, Integer> skillXpMap;
    private transient final String playerName;

    public SkillState(String playerName, Client client) {
        this.playerName = playerName;
        skillXpMap = new HashMap<>();
        for (Skill skill : Skill.values()) {
            skillXpMap.put(skill.getName(), client.getSkillExperience(skill));
        }
    }

    @Override
    public Object get() {
        return this.skillXpMap;
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SkillState)) return false;

        SkillState other = (SkillState) o;
        for (Skill skill : Skill.values()) {
            String skillName = skill.getName();
            if (!skillXpMap.get(skillName).equals(other.skillXpMap.get(skillName))) return false;
        }

        return true;
    }
}
