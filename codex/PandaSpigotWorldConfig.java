package com.hpfxd.pandaspigot.config;

import net.minecraft.server.EntityLiving;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigSerializable
@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
public class PandaSpigotWorldConfig {
    @Comment("Knockback settings with profile support.")
    public KnockbackConfig knockback = new KnockbackConfig();

    @ConfigSerializable
    public static class KnockbackConfig {
        @Comment("Selected knockback profile name.")
        public String profile = "default";

        @Comment("Named profiles, each one can contain behaviours.")
        public Map<String, KnockbackProfile> profiles = new LinkedHashMap<>();

        // Legacy fallback values (kept for backward compatibility)
        public double friction = 2.0;
        public double horizontal = 0.4;
        public double vertical = 0.4;
        public double verticalLimit = 0.4000000059604645;
        public double extraHorizontal = 0.5;
        public double extraVertical = 0.1;

        public KnockbackProfile getActiveProfile() {
            KnockbackProfile profileConfig = this.profiles.get(this.profile);
            if (profileConfig == null) {
                profileConfig = this.profiles.get("default");
            }
            return profileConfig;
        }

        public VelocityBehaviour getVelocityBehaviour() {
            KnockbackProfile profileConfig = this.getActiveProfile();
            if (profileConfig == null) {
                VelocityBehaviour fallback = new VelocityBehaviour();
                fallback.horizontal.value = this.horizontal;
                fallback.vertical.value = this.vertical;
                fallback.extraHorizontal = this.extraHorizontal;
                fallback.extraVertical = this.extraVertical;
                fallback.horizontalFriction = this.friction;
                fallback.verticalFriction = this.friction;
                fallback.verticalLimit = this.verticalLimit;
                return fallback;
            }
            return profileConfig.getVelocityBehaviour();
        }

        public HitDelayBehaviour getHitDelayBehaviour() {
            KnockbackProfile profileConfig = this.getActiveProfile();
            if (profileConfig == null) {
                return new HitDelayBehaviour();
            }
            return profileConfig.getHitDelayBehaviour();
        }
    }

    @ConfigSerializable
    public static class KnockbackProfile {
        public String name = "default";
        public List<KnockbackBehaviour> behaviours = new ArrayList<>();

        public VelocityBehaviour getVelocityBehaviour() {
            VelocityBehaviour selected = null;
            for (KnockbackBehaviour behaviour : this.behaviours) {
                if (behaviour instanceof VelocityBehaviour) {
                    if (selected == null || behaviour.priority > selected.priority) {
                        selected = (VelocityBehaviour) behaviour;
                    }
                }
            }
            return selected == null ? new VelocityBehaviour() : selected;
        }

        public HitDelayBehaviour getHitDelayBehaviour() {
            HitDelayBehaviour selected = null;
            for (KnockbackBehaviour behaviour : this.behaviours) {
                if (behaviour instanceof HitDelayBehaviour) {
                    if (selected == null || behaviour.priority > selected.priority) {
                        selected = (HitDelayBehaviour) behaviour;
                    }
                }
            }
            return selected == null ? new HitDelayBehaviour() : selected;
        }
    }

    @ConfigSerializable
    public static class KnockbackBehaviour {
        public String id = "";
        public String type = "";
        public int priority = 0;
    }

    @ConfigSerializable
    public static class VelocityBehaviour extends KnockbackBehaviour {
        public KnockbackValue horizontal = new KnockbackValue(0.4);
        public double extraHorizontal = 0.5;
        public double horizontalFriction = 2.0;
        public double extraVertical = 0.1;
        public boolean stopSprinting = true;
        public KnockbackValue vertical = new KnockbackValue(0.4);
        public double verticalLimit = 0.4000000059604645;
        public double sprintingSlowdown = 0.6;
        public double verticalFriction = 2.0;

        public VelocityBehaviour() {
            this.type = "velocities";
        }

        public double resolveHorizontal(EntityLiving entity) {
            return this.horizontal.resolve(entity.onGround);
        }

        public double resolveVertical(EntityLiving entity) {
            return this.vertical.resolve(entity.onGround);
        }
    }

    @ConfigSerializable
    public static class HitDelayBehaviour extends KnockbackBehaviour {
        public int hitDelay = 20;

        public HitDelayBehaviour() {
            this.type = "hitDelay";
        }
    }

    @ConfigSerializable
    public static class ProjectileBehaviour extends KnockbackBehaviour {
        public String projectileType = "POTION";
        public double fallSpeed = 0.1;
        public double offset = -16.0;
        public double throwSpeedMultiplier = 0.54;

        public ProjectileBehaviour() {
            this.type = "projectile";
        }
    }

    @ConfigSerializable
    public static class KnockbackValue {
        public double value = 0.4;
        public List<KnockbackModifier> modifiers = new ArrayList<>();

        public KnockbackValue() {
        }

        public KnockbackValue(double value) {
            this.value = value;
        }

        public double resolve(boolean onGround) {
            double result = this.value;
            for (KnockbackModifier modifier : this.modifiers) {
                if (modifier.applies(onGround)) {
                    result *= modifier.multiply;
                }
            }
            return result;
        }
    }

    @ConfigSerializable
    public static class KnockbackModifier {
        public String id = "";
        public String type = "ground";
        public double multiply = 1.0;

        public boolean applies(boolean onGround) {
            if ("ground".equalsIgnoreCase(this.type)) {
                return onGround;
            }
            return false;
        }
    }
}
