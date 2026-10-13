package men.groupiron;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("GroupIronmenTracker")
public interface GroupIronmenTrackerConfig extends Config {
    @ConfigSection(
            name = "Group Config",
            description = "Enter the group details you created on the website here",
            position = 0
    )
    String groupSection = "GroupSection";

    @ConfigSection(
            name = "Self Hosted Config",
            description = "Configure your connection to a self hosted server",
            position = 1,
            closedByDefault = true
    )
    String connectionSection = "ConnectionSection";

    @ConfigSection(
            name = "Data Transmission",
            description = "Configure which data is sent to the website",
            position = 2,
            closedByDefault = true
    )
    String transmitSection = "TransmitSection";

    @ConfigItem(
            keyName = "groupName",
            name = "Group Name (on the website)",
            description = "This is the group name you provided on the website when creating your group",
            section = groupSection
    )
    default String groupName() {
        return "";
    }

    @ConfigItem(
            keyName = "groupToken",
            name = "Group Token",
            description = "Secret token for your group provided by the website. Get this from the member which created the group on the site, or create a new one by visiting the site.",
            secret = true,
            section = groupSection
    )
    default String authorizationToken() {
        return "";
    }

    @ConfigItem(
            keyName = "baseUrlOverride",
            name = "Server base URL override (leave blank to use public server)",
            description = "Overrides the public server URL used to send data. Only change this if you are hosting your own server.",
            section = connectionSection
    )
    default String baseUrlOverride() {
        return "";
    }

    @ConfigItem(
            keyName = "locationOption",
            name = "Transmit In-Game Location",
            description = "Display your player location on the map on the website",
            position = 0,
            section = transmitSection
    )
    default boolean locationOption() {
        return true;
    }

    @ConfigItem(
            keyName = "hideOnlineStatus",
            name = "Hide Online Status",
            description = "Stop sending live data (hitpoints, prayer, run energy, world, location and current target) so the website does not show you as online. Changes to items, skills, quests etc. are still sent and will briefly mark you as active.",
            position = 1,
            section = transmitSection
    )
    default boolean hideOnlineStatus() {
        return false;
    }
}
