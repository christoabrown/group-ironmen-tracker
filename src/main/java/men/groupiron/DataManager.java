package men.groupiron;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.WorldType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class DataManager {
    @Inject
    GroupIronmenTrackerConfig config;
    @Inject
    private CollectionLogManager collectionLogManager;
    @Inject
    private CollectionLogV2Manager collectionLogV2Manager;
    @Inject
    private HttpRequestService httpRequestService;
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
    private final DataState quiver = new DataState("quiver", false);
    @Getter
    private final DataState interacting = new DataState("interacting", false);
    @Getter
    private final DataState seedVault = new DataState("seed_vault", false);
    @Getter
    private final DataState achievementDiary = new DataState("diary_vars", false);
    @Getter
    private final DepositedItems deposited = new DepositedItems();

    public void submitToApi(String playerName) {
        if (skipNextNAttempts-- > 0) return;

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
            quiver.consumeState(updates);
            interacting.consumeState(updates);
            deposited.consumeState(updates);
            seedVault.consumeState(updates);
            achievementDiary.consumeState(updates);
            collectionLogManager.consumeCollections(updates);
            collectionLogManager.consumeNewItems(updates);
            collectionLogV2Manager.consumeClogItems(updates);

            if (updates.size() > 1) {
                HttpRequestService.HttpResponse response = httpRequestService.post(url, groupToken, updates);

                if (!response.isSuccessful()) {
                    skipNextNAttempts = 10;
                    if (response.getCode() == 401) {
                        isMemberInGroup = false;
                    }
                    restoreStateIfNothingUpdated();
                } else {
                    collectionLogV2Manager.clearClogItems();
                }
            } else {
                log.debug("Skip POST: no changes to send (fields={})", updates.size());
            }
        }
    }

    private boolean checkIfPlayerIsInGroup(String groupToken, String playerName) {
        String url = amIMemberOfGroupUrl(playerName);
        if (url == null) return false;

        HttpRequestService.HttpResponse response = httpRequestService.get(url, groupToken);

        return response.isSuccessful();
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
        quiver.restoreState();
        interacting.restoreState();
        deposited.restoreState();
        seedVault.restoreState();
        achievementDiary.restoreState();
        // collectionLogManager.restoreCollections();
        // collectionLogManager.restoreNewCollections();
    }

    private String baseUrl() {
        return httpRequestService.getBaseUrl();
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
}
