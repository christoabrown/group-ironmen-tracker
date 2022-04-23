package men.groupiron;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class DataManager {
    @Inject
    Client client;
    @Inject
    GroupIronmenTrackerConfig config;
    @Inject
    private Gson gson;
    @Inject
    private OkHttpClient okHttpClient;
    private static final String PUBLIC_BASE_URL = "https://groupiron.men";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Getter
    private final DataState inventory = new DataState("inventory", false);
    @Getter
    private final DataState bank = new DataState("bank", false);
    @Getter
    private final DataState equipment = new DataState("equipment", false);
    @Getter
    private final DataState sharedBank = new DataState("shared_bank", true);
    @Getter
    private final DataState resources = new DataState("stats", false);
    @Getter
    private final DataState skills = new DataState("skills", false);
    @Getter
    private final DataState quests = new DataState("quests", false);
    @Getter
    private final DataState position = new DataState("coordinates", false);

    public void submitToApi() {
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null || isBadWorldType()) return;

        String playerName = client.getLocalPlayer().getName();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", playerName);
        inventory.consumeState(updates);
        bank.consumeState(updates);
        equipment.consumeState(updates);
        sharedBank.consumeState(updates);
        resources.consumeState(updates);
        skills.consumeState(updates);
        quests.consumeState(updates);
        position.consumeState(updates);

        String url = getUpdateGroupMemberUrl();
        String groupToken = config.authorizationToken().trim();

        if (updates.size() > 1 && url != null && groupToken.length() > 0) {
            RequestBody body = RequestBody.create(JSON, gson.toJson(updates));
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", groupToken)
                    .post(body)
                    .build();
            Call call = okHttpClient.newCall(request);

            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    // log.error(response.body().string());
                    if (response.code() == 401) {
                        log.error("User not authorized to submit player data with current settings.");
                    } else {
                        log.error("Received response, but failed to submit the player data.");
                    }
                }
            } catch (Exception error) {
                log.error("Failed to submit player data.");
            }
        }
    }

    private String getUpdateGroupMemberUrl() {
        String baseUrlOverride = config.baseUrlOverride().trim();
        String url = PUBLIC_BASE_URL;
        if (baseUrlOverride.length() > 0) {
            url = baseUrlOverride;
        }

        String groupName = config.groupName().trim();
        if (groupName.length() == 0) {
            return null;
        }

        return String.format("%s/api/group/%s/update-group-member", url, groupName);
    }

    private boolean isBadWorldType() {
        EnumSet<WorldType> worldTypes = client.getWorldType();
        for (WorldType worldType : worldTypes) {
            if (worldType == WorldType.SEASONAL ||
                    worldType == WorldType.DEADMAN ||
                    worldType == WorldType.TOURNAMENT_WORLD) {
                return true;
            }
        }

        return false;
    }
}
