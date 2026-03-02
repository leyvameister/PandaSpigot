package com.hpfxd.pandaspigot.config;

import com.google.common.base.Throwables;
import com.hpfxd.configurate.eoyaml.EOYamlConfigurationLoader;
import net.minecraft.server.World;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.util.MapFactories;
import org.spongepowered.configurate.util.NamingSchemes;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
public class PandaSpigotConfig {
    private static PandaSpigotConfig config;
    private static PandaSpigotWorldConfig defaultWorldConfig;
    private static final Map<String, PandaSpigotWorldConfig> worldConfigs = new HashMap<>();
    
    /**
     * Initialize the configuration, and load it from a file.
     * <p>
     * This is called once on server startup, and every reload.
     *
     * @param file The configuration file.
     */
    public static void init(File file) {
        EOYamlConfigurationLoader loader = EOYamlConfigurationLoader.builder()
                .file(file)
                .defaultOptions(o -> o
                        .header("This is the configuration file for PandaSpigot.\n" +
                                "Use caution when modifying settings, as some may impact gameplay in non-obvious ways.")
                        .mapFactory(MapFactories.insertionOrdered())
                        .serializers(build -> build.registerAnnotatedObjects(ObjectMapper.factoryBuilder()
                                .defaultNamingScheme(NamingSchemes.CAMEL_CASE)
                                .build())))
                .build();
        
        try {
            CommentedConfigurationNode root = loader.load();
            config = root.get(PandaSpigotConfig.class);
            
            // worlds
            CommentedConfigurationNode worldsNode = root.node("worlds")
                    .comment("The worlds section is for settings which can be configured per-world.\n" +
                            "\n" +
                            "Any settings in the \"default\" world will provide default values for\n" +
                            "other worlds which don't explicitly specify settings.\n" +
                            "\n" +
                            "To specify settings for a specific world, just add a new section with the world's name.");
            ConfigurationNode defaultWorldNode = worldsNode.node("default");
            defaultWorldConfig = defaultWorldNode.get(PandaSpigotWorldConfig.class);
            defaultWorldNode.set(defaultWorldConfig); // populate default in config
            
            root.set(config); // update backing node
            loader.save(root);
            
            // call after save
            initWorlds(worldsNode);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    
    private static void initWorlds(ConfigurationNode node) throws Exception {
        worldConfigs.clear();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            Object worldName = entry.getKey();
            if (worldName.equals("default")) continue; // skip "default"
            ConfigurationNode worldNode = entry.getValue();
            
            PandaSpigotWorldConfig worldConfig = worldNode.get(PandaSpigotWorldConfig.class);
            
            worldConfigs.put(worldName.toString(), worldConfig);
        }
    }
    
    public static PandaSpigotWorldConfig getWorldConfig(String worldName) {
        return worldConfigs.getOrDefault(worldName, defaultWorldConfig);
    }
    
    public static PandaSpigotWorldConfig getWorldConfig(World world) {
        return getWorldConfig(world.worldData.getName());
    }
    
    public static PandaSpigotConfig get() {
        return config;
    }
    
    //------------------------------------------------------------------------
}
