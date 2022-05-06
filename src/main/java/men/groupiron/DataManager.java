package men.groupiron;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.client.RuneLiteProperties;
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
    private static final String USER_AGENT = "GroupIronmenTracker/1.1 " + "RuneLite/" + RuneLiteProperties.getVersion();
    private boolean isMemberInGroup = false;
    private int skipNextNAttempts = 0;

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
    @Getter
    private final DataState runePouch = new DataState("rune_pouch", false);
    @Getter
    private final DataState interacting = new DataState("interacting", false);
    @Getter
    private final DepositedItems deposited = new DepositedItems();

    public void submitToApi() {
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null || isBadWorldType()) return;
        if (skipNextNAttempts-- > 0) return;

        String playerName = client.getLocalPlayer().getName();
        String groupToken = config.authorizationToken().trim();

        if (groupToken.length() > 0) {
            // NOTE: We do this check so characters who are not authorized won't waste time serializing and sending
            // their data. It is OK if the user switches characters or is removed from the group since the update call
            // below will return a 401 where we set isMemberOfGroup = false again.
            if (!isMemberInGroup) {
                boolean isMember = checkIfPlayerIsInGroup(groupToken, playerName);

                if (!isMember) {
                    // NOTE: We don't really need to check this everytime I don't think.
                    // Waiting for a game state event is not what we really want either
                    // since membership can change at anytime from the website.
                    skipNextNAttempts = 10;
                    return;
                }
                isMemberInGroup = true;
            }

            String url = getUpdateGroupMemberUrl();
            if (url == null) return;

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
            runePouch.consumeState(updates);
            interacting.consumeState(updates);
            deposited.consumeState(updates);

            if (updates.size() > 1) {
                RequestBody body = RequestBody.create(JSON, gson.toJson(updates));
                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", groupToken)
                        .header("User-Agent", USER_AGENT)
                        .post(body)
                        .build();
                Call call = okHttpClient.newCall(request);

                try (Response response = call.execute()) {
                    if (!response.isSuccessful()) {
                        // log.error(response.body().string());
                        skipNextNAttempts = 10;
                        if (response.code() == 401) {
                            // log.error("User not authorized to submit player data with current settings.");
                            isMemberInGroup = false;
                        }

                        restoreStateIfNothingUpdated();
                    }
                } catch (Exception _error) {
                    skipNextNAttempts = 10;
                    restoreStateIfNothingUpdated();
                }
            }
        }
    }

    private boolean checkIfPlayerIsInGroup(String groupToken, String playerName) {
        String url = amIMemberOfGroupUrl(playerName);
        if (url == null) return false;

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", groupToken)
                .header("User-Agent", USER_AGENT)
                .get()
                .build();
        Call call = okHttpClient.newCall(request);

        try (Response response = call.execute()) {
            // log.error(response.body().string());
            return response.isSuccessful();
        } catch (Exception _error) {
            return false;
        }
    }

    // NOTE: These states should only be restored if a new update did not come in at some point before calling this
    private void restoreStateIfNothingUpdated() {
        inventory.restoreState();
        bank.restoreState();
        equipment.restoreState();
        sharedBank.restoreState();
        resources.restoreState();
        skills.restoreState();
        quests.restoreState();
        position.restoreState();
        runePouch.restoreState();
        interacting.restoreState();
        deposited.restoreState();
    }

    private String baseUrl() {
        String baseUrlOverride = config.baseUrlOverride().trim();
        if (baseUrlOverride.length() > 0) return baseUrlOverride;
        return PUBLIC_BASE_URL;
    }

    private String groupName() {
        String groupName = config.groupName().trim();
        if (groupName.length() == 0) {
            return null;
        }

        return groupName;
    }

    private String getUpdateGroupMemberUrl() {
        String baseUrl = baseUrl();
        String groupName = groupName();

        if (baseUrl == null || groupName == null) return null;

        return String.format("%s/api/group/%s/update-group-member", baseUrl, groupName);
    }

    private String amIMemberOfGroupUrl(String playerName) {
        String baseUrl = baseUrl();
        String groupName = groupName();

        if (baseUrl == null || groupName == null) return null;

        return String.format("%s/api/group/%s/am-i-in-group?member_name=%s", baseUrl, groupName, playerName);
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
