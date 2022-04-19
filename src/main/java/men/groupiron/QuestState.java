package men.groupiron;

import net.runelite.api.Client;
import net.runelite.api.Quest;

import java.util.HashMap;
import java.util.Map;

public class QuestState implements ConsumableState {
    private final Map<String, net.runelite.api.QuestState> questStateMap;

    public QuestState(Client client) {
        this.questStateMap = new HashMap<>();
        for (Quest quest : Quest.values()) {
            questStateMap.put(String.valueOf(quest.getId()), quest.getState(client));
        }
    }

    @Override
    public Object get() {
        return questStateMap;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof QuestState)) return false;

        QuestState other = (QuestState) o;
        for (Quest quest : Quest.values()) {
            String questId = String.valueOf(quest.getId());
            if (questStateMap.get(questId) != other.questStateMap.get(questId)) {
                return false;
            }
        }

        return true;
    }
}
