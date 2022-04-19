package men.groupiron;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GroupIronmenTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GroupIronmenTrackerPlugin.class);
		RuneLite.main(args);
	}
}