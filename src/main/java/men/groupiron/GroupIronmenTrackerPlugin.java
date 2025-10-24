package men.groupiron;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
        name = "Group Ironmen Tracker"
)
public class GroupIronmenTrackerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private GroupIronmenTrackerConfig config;
    @Inject
    private DataManager dataManager;
    @Inject
    private ItemManager itemManager;
    @Inject
    private CollectionLogManager collectionLogManager;
    @Inject
    private CollectionLogV2Manager collectionLogV2Manager;
    @Inject
    private CollectionLogWidgetSubscriber collectionLogWidgetSubscriber;
    @Inject
    private HttpRequestService httpRequestService;
    @Inject
    ClientThread clientThread;
    private int itemsDeposited = 0;
    private static final int SECONDS_BETWEEN_UPLOADS = 1;
    private static final int SECONDS_BETWEEN_INFREQUENT_DATA_CHANGES = 60;
    private static final int DEPOSIT_ITEM = 12582914;
    private static final int DEPOSIT_INVENTORY = 12582916;
    private static final int DEPOSIT_EQUIPMENT = 12582918;
    private static final int CHATBOX_ENTERED = 681;
    private static final int GROUP_STORAGE_LOADER = 293;
    private static final int COLLECTION_LOG_INVENTORYID = 620;
    private static final Pattern COLLECTION_LOG_ITEM_PATTERN = Pattern.compile("New item added to your collection log: (.*)");
    private boolean notificationStarted = false;

    @Override
    protected void startUp() throws Exception {
        clientThread.invokeLater(() -> {
            collectionLogManager.initCollectionLog();
        });
        collectionLogWidgetSubscriber.startUp();
        log.info("Group Ironmen Tracker started!");
    }

    @Override
    protected void shutDown() throws Exception {
        collectionLogWidgetSubscriber.shutDown();
        log.info("Group Ironmen Tracker stopped!");
    }

    @Schedule(
            period = SECONDS_BETWEEN_UPLOADS,
            unit = ChronoUnit.SECONDS,
            asynchronous = true
    )
    public void submitToApi() {
        if(doNotUseThisData())
            return;

        String playerName = client.getLocalPlayer().getName();
        dataManager.submitToApi(playerName);
    }

    @Schedule(
            period = SECONDS_BETWEEN_UPLOADS,
            unit = ChronoUnit.SECONDS
    )
    public void updateThingsThatDoChangeOften() {
        if (doNotUseThisData())
            return;
        Player player = client.getLocalPlayer();
        String playerName = player.getName();
        dataManager.getResources().update(new ResourcesState(playerName, client));

        LocalPoint localPoint = player.getLocalLocation();
        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
        dataManager.getPosition().update(new LocationState(playerName, worldPoint));

        dataManager.getRunePouch().update(new RunePouchState(playerName, client));
        dataManager.getQuiver().update(new QuiverState(playerName, client, itemManager));
    }

    @Schedule(
            period = SECONDS_BETWEEN_INFREQUENT_DATA_CHANGES,
            unit = ChronoUnit.SECONDS
    )
    public void updateThingsThatDoNotChangeOften() {
        if (doNotUseThisData())
            return;
        String playerName = client.getLocalPlayer().getName();
        dataManager.getQuests().update(new QuestState(playerName, client));
        dataManager.getAchievementDiary().update(new AchievementDiaryState(playerName, client));
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (doNotUseThisData())
            return;

        final int varpId = event.getVarpId();
        if (varpId == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO || varpId == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT) {
            String playerName = client.getLocalPlayer().getName();
            dataManager.getQuiver().update(new QuiverState(playerName, client, itemManager));
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        if(doNotUseThisData())
            return;

        --itemsDeposited;
        updateInteracting();

        Widget groupStorageLoaderText = client.getWidget(GROUP_STORAGE_LOADER, 1);
        if (groupStorageLoaderText != null) {
            if (groupStorageLoaderText.getText().equalsIgnoreCase("saving...")) {
                dataManager.getSharedBank().commitTransaction();
            }
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged statChanged) {
        if (doNotUseThisData())
            return;
        String playerName = client.getLocalPlayer().getName();
        dataManager.getSkills().update(new SkillState(playerName, client));
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (doNotUseThisData())
            return;
        String playerName = client.getLocalPlayer().getName();
        final int id = event.getContainerId();
        ItemContainer container = event.getItemContainer();

        if (id == InventoryID.BANK) {
            dataManager.getDeposited().reset();
            dataManager.getBank().update(new ItemContainerState(playerName, container, itemManager));
        } else if (id == InventoryID.SEED_VAULT) {
            dataManager.getSeedVault().update(new ItemContainerState(playerName, container, itemManager));
        } else if (id == InventoryID.INV) {
            ItemContainerState newInventoryState = new ItemContainerState(playerName, container, itemManager, 28);
            if (itemsDeposited > 0) {
                updateDeposited(newInventoryState, (ItemContainerState) dataManager.getInventory().mostRecentState());
            }

            dataManager.getInventory().update(newInventoryState);
        } else if (id == InventoryID.WORN) {
            ItemContainerState newEquipmentState = new ItemContainerState(playerName, container, itemManager, 14);
            if (itemsDeposited > 0) {
                updateDeposited(newEquipmentState, (ItemContainerState) dataManager.getEquipment().mostRecentState());
            }

            dataManager.getEquipment().update(newEquipmentState);
        } else if (id == InventoryID.INV_GROUP_TEMP) {
            dataManager.getSharedBank().update(new ItemContainerState(playerName, container, itemManager));
        } else if (id == COLLECTION_LOG_INVENTORYID) {
            collectionLogManager.updateCollection(new ItemContainerState(playerName, container, itemManager));
        }
    }

    @Subscribe
    private void onScriptPostFired(ScriptPostFired event) {
        if(doNotUseThisData())
            return;

        if (event.getScriptId() == CHATBOX_ENTERED && client.getWidget(InterfaceID.BankDepositbox.INVENTORY) != null) {
            itemsMayHaveBeenDeposited();
        }
    }

    @Subscribe
    private void onMenuOptionClicked(MenuOptionClicked event) {
        if(doNotUseThisData())
            return;

        final int param1 = event.getParam1();
        final MenuAction menuAction = event.getMenuAction();
        if (menuAction == MenuAction.CC_OP) {
            if (param1 == DEPOSIT_ITEM || param1 == DEPOSIT_INVENTORY || param1 == DEPOSIT_EQUIPMENT) {
                itemsMayHaveBeenDeposited();
            }
        }
    }

    @Subscribe
    private void onInteractingChanged(InteractingChanged event) {
        if(doNotUseThisData())
            return;

        if (event.getSource() != client.getLocalPlayer()) return;
        updateInteracting();
    }

    @Subscribe
    private void onChatMessage(ChatMessage chatMessage) {
        if (doNotUseThisData())
            return;

        if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE) return;

        Matcher matcher = COLLECTION_LOG_ITEM_PATTERN.matcher(chatMessage.getMessage());
        if (matcher.find()) {
            String itemName = Text.removeTags(matcher.group(1));
            if (!StringUtils.isBlank(itemName)) {
                collectionLogManager.updateNewItem(itemName);
            }
        }
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired scriptPreFired)
    {
        if(doNotUseThisData())
            return;

        switch (scriptPreFired.getScriptId())
        {
            case ScriptID.NOTIFICATION_START:
                notificationStarted = true;
                break;
            case ScriptID.NOTIFICATION_DELAY:
                if (!notificationStarted) return;
                String topText = client.getVarcStrValue(VarClientID.NOTIFICATION_TITLE);
                String bottomText = client.getVarcStrValue(VarClientID.NOTIFICATION_MAIN);
                if (topText.equalsIgnoreCase("Collection log")) {
                    String entry = Text.removeTags(bottomText).substring("New item:".length());
                    collectionLogManager.updateNewItem(entry);
                }
                notificationStarted = false;
                break;
        }
    }

    private void itemsMayHaveBeenDeposited() {
        // NOTE: In order to determine if an item has gone through the deposit box we first detect if any of the menu
        // actions were performed OR a custom amount was entered while the deposit box inventory widget was opened.
        // Then we allow up to two game ticks were an inventory changed event can occur and at that point we assume
        // it must have been caused by the action detected just before. We can't check the inventory at the time of
        // either interaction since the inventory may have not been updated yet. We also cannot just check that the deposit
        // box window is open in the item container event since it is possible for a player to close the widget before
        // the event handler is called.
        itemsDeposited = 2;
    }

    private void updateInteracting() {
        Player player = client.getLocalPlayer();

        if (player != null) {
            Actor actor = player.getInteracting();

            if (actor != null) {
                String playerName = player.getName();
                dataManager.getInteracting().update(new InteractingState(playerName, actor, client));
            }
        }
    }

    private void updateDeposited(ItemContainerState newState, ItemContainerState previousState) {
        ItemContainerState deposited = newState.whatGotRemoved(previousState);
        dataManager.getDeposited().update(deposited);
    }

    /**
     * A guard blocking client callbacks that may write invalid state, such as when the player is not logged in to a main-game profile.
     */
    private boolean doNotUseThisData() {
        boolean isStandardProfile = RuneScapeProfileType.getCurrent(client) == RuneScapeProfileType.STANDARD;
        return client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null || !isStandardProfile;
    }

    @Provides
    GroupIronmenTrackerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GroupIronmenTrackerConfig.class);
    }
}
