package men.groupiron;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PluginVersion {
    private static final String VERSION = loadVersion();

    public static String get() {
        return VERSION;
    }

    private static String loadVersion() {
        Properties props = new Properties();
        try (InputStream is = PluginVersion.class.getResourceAsStream("/men/groupiron/version.properties")) {
            if (is != null) {
                props.load(is);
                return props.getProperty("version", "unknown");
            }
        } catch (IOException e) {
            log.warn("Failed to load plugin version", e);
        }
        return "unknown";
    }

    private PluginVersion() {
    }
}
