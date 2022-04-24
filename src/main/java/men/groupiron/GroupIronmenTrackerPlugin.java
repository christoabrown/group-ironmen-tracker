package men.groupiron;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;

import javax.inject.Inject;
import java.time.temporal.ChronoUnit;

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
    private static final int SECONDS_BETWEEN_UPLOADS = 1;
    private static final int SECONDS_BETWEEN_INFREQUENT_DATA_CHANGES = 60;
    private static final int SAVE_SHARED_STORAGE = 47448098;
    private static final int BACK_TO_BANK_SHARED_STORAGE = 47448073;

    @Override
    protected void startUp() throws Exception {
        log.info("Group Ironmen Tracker started!");
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Group Ironmen Tracker stopped!");
    }

    @Schedule(
            period = SECONDS_BETWEEN_UPLOADS,
            unit = ChronoUnit.SECONDS,
            asynchronous = true
    )
    public void submitToApi() {
        dataManager.submitToApi();
    }

    @Schedule(
            period = SECONDS_BETWEEN_UPLOADS,
            unit = ChronoUnit.SECONDS
    )
    public void updateThingsThatDoChangeOften() {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) return;
        dataManager.getResources().update(new ResourcesState(client));

        Player player = client.getLocalPlayer();
        LocalPoint localPoint = player.getLocalLocation();
        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
        dataManager.getPosition().update(new LocationState(worldPoint));
    }

    @Schedule(
            period = SECONDS_BETWEEN_INFREQUENT_DATA_CHANGES,
            unit = ChronoUnit.SECONDS
    )
    public void updateThingsThatDoNotChangeOften() {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
            return;
        dataManager.getQuests().update(new QuestState(client));
    }

    @Subscribe
    public void onStatChanged(StatChanged statChanged) {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
            return; // Is this possible?
        dataManager.getSkills().update(new SkillState(client));
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        final int id = event.getContainerId();
        ItemContainer container = event.getItemContainer();

        if (id == InventoryID.BANK.getId()) {
            dataManager.getBank().update(new ItemContainerState(container, itemManager));
        } else if (id == InventoryID.INVENTORY.getId()) {
            dataManager.getInventory().update(new ItemContainerState(container, itemManager, 28));
        } else if (id == InventoryID.EQUIPMENT.getId()) {
            dataManager.getEquipment().update(new ItemContainerState(container, itemManager, 14));
        } else if (id == InventoryID.GROUP_STORAGE.getId()) {
            dataManager.getSharedBank().update(new ItemContainerState(container, itemManager));
        }
    }

    @Subscribe
    private void onMenuOptionClicked(MenuOptionClicked event) {
        final int param1 = event.getParam1();
        final MenuAction menuAction = event.getMenuAction();
        if (menuAction == MenuAction.CC_OP && (param1 == SAVE_SHARED_STORAGE || param1 == BACK_TO_BANK_SHARED_STORAGE)) {
            dataManager.getSharedBank().commitTransaction();
        }
    }

    @Provides
    GroupIronmenTrackerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GroupIronmenTrackerConfig.class);
    }
}
