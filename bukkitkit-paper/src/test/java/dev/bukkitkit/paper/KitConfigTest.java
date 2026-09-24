package dev.bukkitkit.paper;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitConfigTest {

    public static final class SampleConfig {
        public String welcomeMessage = "Hello!";
        public boolean joinMessageEnabled = true;
        public int maxHomes = 3;
        public List<String> motdLines = new ArrayList<>(List.of("a", "b"));
    }

    public static final class NestedRoot {
        public String name = "demo";
        public Database database = new Database();

        public static final class Database {
            public String host = "localhost";
            public int port = 3306;
            public Pool pool = new Pool();
        }

        public static final class Pool {
            public int size = 8;
        }
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
    void seedsNestedSectionsWhenFileIsNew() throws Exception {
        NestedRoot config = new NestedRoot();
        YamlConfiguration yaml = new YamlConfiguration();

        boolean dirty = KitConfig.apply(config, yaml, false);

        assertTrue(dirty);
        assertEquals("demo", yaml.getString("name"));
        assertEquals("localhost", yaml.getString("database.host"));
        assertEquals(3306, yaml.getInt("database.port"));
        assertEquals(8, yaml.getInt("database.pool.size"));
        assertTrue(yaml.get("database") instanceof Map || yaml.isConfigurationSection("database"));
    }

    @Test
    void loadsNestedSectionsAndFillsMissingNestedKeys() throws Exception {
        NestedRoot config = new NestedRoot();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", "prod");
        yaml.set("database.host", "db.internal");
        // database.port and database.pool.size missing

        boolean dirty = KitConfig.apply(config, yaml, true);

        assertTrue(dirty);
        assertEquals("prod", config.name);
        assertEquals("db.internal", config.database.host);
        assertEquals(3306, config.database.port);
        assertEquals(8, config.database.pool.size);
        assertEquals(3306, yaml.getInt("database.port"));
        assertEquals(8, yaml.getInt("database.pool.size"));
    }

    @Test
    void instantiatesNullNestedFieldWhenSectionPresent() throws Exception {
        NestedRoot config = new NestedRoot();
        config.database = null;
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", "x");
        yaml.set("database.host", "remote");
        yaml.set("database.port", 5432);
        yaml.set("database.pool.size", 2);

        boolean dirty = KitConfig.apply(config, yaml, true);

        assertFalse(dirty);
        assertNotNull(config.database);
        assertEquals("remote", config.database.host);
        assertEquals(5432, config.database.port);
        assertEquals(2, config.database.pool.size);
    }

    @Test
    void reseedsWhenNestedKeyIsExplicitNull() throws Exception {
        NestedRoot config = new NestedRoot();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", "x");
        yaml.set("database", null);

        boolean dirty = KitConfig.apply(config, yaml, true);

        assertTrue(dirty);
        assertEquals("localhost", yaml.getString("database.host"));
        assertEquals(3306, yaml.getInt("database.port"));
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
    void rejectsNonSectionForNestedField() {
        NestedRoot config = new NestedRoot();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("database", "not-a-section");

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
