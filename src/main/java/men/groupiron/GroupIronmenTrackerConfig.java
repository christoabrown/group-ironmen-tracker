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

    @ConfigItem(
            keyName = "groupName",
            name = "Group Name (on the website)",
            description = "This is the group name you provided on the website when creating your group",
            section = groupSection
    )
    default String groupName() {
        return "";
    }

    @ConfigSection(
            name = "Data Transmission",
            description = "Configure your data transmission settings.",
            position = 1,
            closedByDefault = true
    )
    String transmitSection = "TransmitSection";

    @ConfigItem(
            keyName = "locationOption",
            name = "Transmit In-Game Location",
            position = 1,
            description = "Display your player location on a map via the website. ",
            section = transmitSection
    )
    default boolean locationOption() {
        return true;
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
}
