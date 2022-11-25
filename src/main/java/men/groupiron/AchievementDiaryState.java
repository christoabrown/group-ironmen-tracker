package men.groupiron;

import net.runelite.api.Client;

public class AchievementDiaryState implements ConsumableState {
    private final transient String playerName;
    private static final int[] diaryVarbits = new int[]{
            /* Karamja Easy */
            3566, 3567, 3568, 3569, 3570, 3571, 3572, 3573, 3574, 3575,
            /* Karamja Medium */
            3579, 3580, 3581, 3582, 3583, 3584, 3596, 3586, 3587, 3588, 3589, 3590, 3591, 3592, 3593, 3594, 3595, 3597, 3585,
            /* Karamja Hard */
            3600, 3601, 3602, 3603, 3604, 3605, 3606, 3607, 3608, 3609
    };

    private static final int[] diaryVarps = new int[]{
            /* Ardougne */
            1196, 1197,
            /* Desert */
            1198, 1199,
            /* Falador */
            1186, 1187,
            /* Fremennik */
            1184, 1185,
            /* Kandarin */
            1178, 1179,
            /* Karamja Elite */
            1200,
            /* Kourend & Kebos */
            2085, 2086,
            /* Lumbridge & Draynor */
            1194, 1195,
            /* Morytania */
            1180, 1181,
            /* Varrock */
            1176, 1177,
            /* Western Provinces */
            1182, 1183,
            /* Wilderness */
            1192, 1193
    };

    private final int[] diaryVarValues = new int[diaryVarbits.length + diaryVarps.length];

    public AchievementDiaryState(String playerName, Client client) {
        this.playerName = playerName;

        for (int i = 0; i < diaryVarps.length; ++i) {
            diaryVarValues[i] = client.getVarpValue(diaryVarps[i]);
        }
        for (int i = 0; i < diaryVarbits.length; ++i) {
            diaryVarValues[i + diaryVarps.length] = client.getVarbitValue(diaryVarbits[i]);
        }
    }

    @Override
    public Object get() {
        return diaryVarValues;
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof AchievementDiaryState)) return false;

        AchievementDiaryState other = (AchievementDiaryState) o;
        for (int i = 0; i < diaryVarValues.length; ++i) {
            if (diaryVarValues[i] != other.diaryVarValues[i]) return false;
        }

        return true;
    }
}
