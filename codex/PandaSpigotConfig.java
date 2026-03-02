package com.hpfxd.pandaspigot.config;

import com.google.common.base.Throwables;
import com.hpfxd.configurate.eoyaml.EOYamlConfigurationLoader;
import net.minecraft.server.World;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
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
            applyKnockbackConfig(defaultWorldNode, defaultWorldConfig);
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
            applyKnockbackConfig(worldNode, worldConfig);
            worldConfigs.put(worldName.toString(), worldConfig);
        }
    }
    

    private static void applyKnockbackConfig(ConfigurationNode worldNode, PandaSpigotWorldConfig worldConfig) {
        ConfigurationNode knockbackNode = worldNode.node("knockback");
        if (knockbackNode.virtual()) {
            return;
        }

        worldConfig.knockback.profile = knockbackNode.node("profile").getString(worldConfig.knockback.profile);

        ConfigurationNode profilesNode = knockbackNode.node("profiles");
        worldConfig.knockback.profiles.clear();
        if (profilesNode.virtual()) {
            return;
        }

        for (Map.Entry<Object, ? extends ConfigurationNode> profileEntry : profilesNode.childrenMap().entrySet()) {
            String profileName = String.valueOf(profileEntry.getKey());
            ConfigurationNode profileNode = profileEntry.getValue();

            PandaSpigotWorldConfig.KnockbackProfile profile = new PandaSpigotWorldConfig.KnockbackProfile();
            profile.name = profileNode.node("name").getString(profileName);

            for (ConfigurationNode behaviourNode : profileNode.node("behaviours").childrenList()) {
                String type = behaviourNode.node("type").getString("");
                if ("velocities".equalsIgnoreCase(type)) {
                    PandaSpigotWorldConfig.VelocityBehaviour velocity = new PandaSpigotWorldConfig.VelocityBehaviour();
                    velocity.id = behaviourNode.node("id").getString("velocities");
                    velocity.priority = behaviourNode.node("priority").getInt(0);

                    velocity.horizontal = parseKnockbackValue(behaviourNode.node("horizontal"), velocity.horizontal.value);
                    velocity.extraHorizontal = behaviourNode.node("extraHorizontal").getDouble(velocity.extraHorizontal);
                    velocity.horizontalFriction = behaviourNode.node("horizontalFriction").getDouble(velocity.horizontalFriction);
                    velocity.extraVertical = behaviourNode.node("extraVertical").getDouble(velocity.extraVertical);
                    velocity.stopSprinting = behaviourNode.node("stopSprinting").getBoolean(velocity.stopSprinting);
                    velocity.vertical = parseKnockbackValue(behaviourNode.node("vertical"), velocity.vertical.value);
                    velocity.verticalLimit = behaviourNode.node("verticalLimit").getDouble(velocity.verticalLimit);
                    velocity.sprintingSlowdown = behaviourNode.node("sprintingSlowdown").getDouble(velocity.sprintingSlowdown);
                    velocity.verticalFriction = behaviourNode.node("verticalFriction").getDouble(velocity.verticalFriction);

                    profile.behaviours.add(velocity);
                } else if ("hitDelay".equalsIgnoreCase(type)) {
                    PandaSpigotWorldConfig.HitDelayBehaviour hitDelay = new PandaSpigotWorldConfig.HitDelayBehaviour();
                    hitDelay.id = behaviourNode.node("id").getString("hitDelay");
                    hitDelay.priority = behaviourNode.node("priority").getInt(0);
                    hitDelay.hitDelay = behaviourNode.node("hitDelay").getInt(hitDelay.hitDelay);
                    profile.behaviours.add(hitDelay);
                } else if ("projectile".equalsIgnoreCase(type)) {
                    PandaSpigotWorldConfig.ProjectileBehaviour projectile = new PandaSpigotWorldConfig.ProjectileBehaviour();
                    projectile.id = behaviourNode.node("id").getString("projectile");
                    projectile.priority = behaviourNode.node("priority").getInt(0);
                    projectile.projectileType = behaviourNode.node("projectileType").getString(projectile.projectileType);
                    projectile.fallSpeed = behaviourNode.node("fallSpeed").getDouble(projectile.fallSpeed);
                    projectile.offset = behaviourNode.node("offset").getDouble(projectile.offset);
                    projectile.throwSpeedMultiplier = behaviourNode.node("throwSpeedMultiplier").getDouble(projectile.throwSpeedMultiplier);
                    profile.behaviours.add(projectile);
                }
            }

            worldConfig.knockback.profiles.put(profileName, profile);
        }
    }

    private static PandaSpigotWorldConfig.KnockbackValue parseKnockbackValue(ConfigurationNode node, double defaultValue) {
        PandaSpigotWorldConfig.KnockbackValue value = new PandaSpigotWorldConfig.KnockbackValue(defaultValue);
        if (node == null || node.virtual()) {
            return value;
        }

        if (node.isMap()) {
            value.value = node.node("value").getDouble(defaultValue);
            for (ConfigurationNode modNode : node.node("modifiers").childrenList()) {
                PandaSpigotWorldConfig.KnockbackModifier modifier = new PandaSpigotWorldConfig.KnockbackModifier();
                modifier.id = modNode.node("id").getString("");
                modifier.type = modNode.node("type").getString("ground");
                modifier.multiply = modNode.node("multiply").getDouble(1.0D);
                value.modifiers.add(modifier);
            }
            return value;
        }

        value.value = node.getDouble(defaultValue);
        return value;
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
