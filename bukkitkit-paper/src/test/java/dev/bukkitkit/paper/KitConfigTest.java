package dev.bukkitkit.paper;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitConfigTest {

    public static final class SampleConfig {
        public String welcomeMessage = "Hello!";
        public boolean joinMessageEnabled = true;
        public int maxHomes = 3;
        public List<String> motdLines = new ArrayList<>(List.of("a", "b"));
    }

    @Test
    void seedsDefaultsWhenFileIsNew() throws Exception {
        SampleConfig config = new SampleConfig();
        YamlConfiguration yaml = new YamlConfiguration();

        boolean dirty = KitConfig.apply(config, yaml, false);

        assertTrue(dirty);
        assertEquals("Hello!", yaml.getString("welcomeMessage"));
        assertTrue(yaml.getBoolean("joinMessageEnabled"));
        assertEquals(3, yaml.getInt("maxHomes"));
        assertEquals(List.of("a", "b"), yaml.getStringList("motdLines"));
    }

    @Test
    void loadsExistingValuesAndFillsMissingKeys() throws Exception {
        SampleConfig config = new SampleConfig();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("welcomeMessage", "Hey");
        yaml.set("joinMessageEnabled", false);
        // maxHomes and motdLines intentionally missing

        boolean dirty = KitConfig.apply(config, yaml, true);

        assertTrue(dirty);
        assertEquals("Hey", config.welcomeMessage);
        assertFalse(config.joinMessageEnabled);
        assertEquals(3, yaml.getInt("maxHomes"));
        assertEquals(List.of("a", "b"), yaml.getStringList("motdLines"));
    }

    @Test
    void rejectsWrongValueType() {
        SampleConfig config = new SampleConfig();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("maxHomes", "nope");

        assertThrows(dev.bukkitkit.api.BukkitKitException.class,
                () -> KitConfig.apply(config, yaml, true));
    }

    @Test
    void rejectsPathTraversalFileNames() {
        assertThrows(dev.bukkitkit.api.BukkitKitException.class,
                () -> KitConfig.validateRelativeFileName("../evil.yml"));
        assertThrows(dev.bukkitkit.api.BukkitKitException.class,
                () -> KitConfig.validateRelativeFileName("/tmp/evil.yml"));
    }

    @Test
    void acceptsRelativeSubfolderFileNames() {
        KitConfig.validateRelativeFileName("config.yml");
        KitConfig.validateRelativeFileName("data/settings.yml");
    }
}
