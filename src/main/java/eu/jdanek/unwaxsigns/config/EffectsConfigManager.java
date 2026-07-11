package eu.jdanek.unwaxsigns.config;

import eu.jdanek.unwaxsigns.util.NamespacedKeyUtil;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EffectsConfigManager {

    private final JavaPlugin plugin;
    private final Logger logger;

    // success
    private Sound successSound;
    private float successVolume;
    private float successPitch;
    private SoundTarget successSoundTarget;
    private Particle successParticle;
    private boolean successParticleEnabled;
    private int successParticleCount;
    private double successOffsetX, successOffsetY, successOffsetZ;
    private double successExtra;

    // failure
    private Sound failureSound;
    private float failureVolume;
    private float failurePitch;
    private SoundTarget failureSoundTarget;
    private Particle failureParticle;
    private boolean failureParticleEnabled;
    private int failureParticleCount;
    private double failureOffsetX, failureOffsetY, failureOffsetZ;
    private double failureExtra;

    public EffectsConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "effects.yml");
        if (!file.exists()) {
            plugin.saveResource("effects.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // success
        this.successSound = loadSound(config, "effects.success.sound", Sound.ITEM_AXE_SCRAPE);
        this.successVolume = (float) config.getDouble("effects.success.volume", 1.0);
        this.successPitch = (float) config.getDouble("effects.success.pitch", 1.0);
        this.successSoundTarget = SoundTarget.fromString(config.getString("effects.success.sound-target"));
        this.successParticle = loadParticle(config, "effects.success.particle", Particle.WAX_OFF);
        this.successParticleEnabled = config.getBoolean("effects.success.particle-enabled", true);
        this.successParticleCount = config.getInt("effects.success.count", 15);
        this.successOffsetX = config.getDouble("effects.success.offset-x", 0.4);
        this.successOffsetY = config.getDouble("effects.success.offset-y", 0.4);
        this.successOffsetZ = config.getDouble("effects.success.offset-z", 0.4);
        this.successExtra = config.getDouble("effects.success.extra", 1.0);

        // failure
        this.failureSound = loadSound(config, "effects.failure.sound", Sound.ENTITY_VILLAGER_NO);
        this.failureVolume = (float) config.getDouble("effects.failure.volume", 1.0);
        this.failurePitch = (float) config.getDouble("effects.failure.pitch", 1.0);
        this.failureSoundTarget = SoundTarget.fromString(config.getString("effects.failure.sound-target"));
        this.failureParticle = loadParticle(config, "effects.failure.particle", null);
        this.failureParticleEnabled = config.getBoolean("effects.failure.particle-enabled", true);
        this.failureParticleCount = config.getInt("effects.failure.count", 5);
        this.failureOffsetX = config.getDouble("effects.failure.offset-x", 0.4);
        this.failureOffsetY = config.getDouble("effects.failure.offset-y", 0.4);
        this.failureOffsetZ = config.getDouble("effects.failure.offset-z", 0.4);
        this.failureExtra = config.getDouble("effects.failure.extra", 0.0);
    }

    public void playSuccess(Location location) {
        playSuccess(location, null, false);
    }

    public void playSuccess(Location location, boolean spawnParticle) {
        playSuccess(location, null, spawnParticle);
    }

    public void playSuccessAtPlayer(Player player, boolean spawnParticle) {
        playSuccess(player.getLocation(), player, spawnParticle);
    }

    public void playSuccess(Location location, Player player, boolean spawnParticle) {
        playSound(location, player, successSound, successVolume, successPitch, successSoundTarget);
        if (spawnParticle) {
            spawnSuccessParticle(location);
        }
    }

    public void playFailure(Location location) {
        playFailure(location, null, false);
    }

    public void playFailure(Location location, boolean spawnParticle) {
        playFailure(location, null, spawnParticle);
    }

    public void playFailureAtPlayer(Player player, boolean spawnParticle) {
        playFailure(player.getLocation(), player, spawnParticle);
    }

    public void playFailure(Location location, Player player, boolean spawnParticle) {
        playSound(location, player, failureSound, failureVolume, failurePitch, failureSoundTarget);
        if (spawnParticle) {
            spawnFailureParticle(location);
        }
    }

    public void spawnSuccessParticle(Location location) {
        if (!successParticleEnabled || location.getWorld() == null) return;

        location.getWorld().spawnParticle(
                successParticle,
                location.toCenterLocation(),
                successParticleCount,
                successOffsetX,
                successOffsetY,
                successOffsetZ,
                successExtra
        );
    }

    public void spawnFailureParticle(Location location) {
        if (!failureParticleEnabled || location.getWorld() == null) return;

        location.getWorld().spawnParticle(
                failureParticle,
                location.toCenterLocation(),
                failureParticleCount,
                failureOffsetX,
                failureOffsetY,
                failureOffsetZ,
                failureExtra
        );
    }

    private void playSound(Location location, Player player, Sound sound, float volume, float pitch, SoundTarget target) {
        if (target == SoundTarget.WORLD || player == null) {
            World world = location.getWorld();
            if (world != null) {
                world.playSound(location, sound, volume, pitch);
            }
        } else {
            player.playSound(location, sound, volume, pitch);
        }
    }

    private Sound loadSound(FileConfiguration config, String path, Sound defaultSound) {
        String soundName = config.getString(path);
        if (soundName == null) return defaultSound;

        var key = NamespacedKeyUtil.fromString(soundName);
        if (key == null) {
            logger.warning("Invalid sound '" + soundName + "' at " + path + ". Using default.");
            return defaultSound;
        }

        Sound sound = Registry.SOUNDS.get(key);
        if (sound == null) {
            logger.warning("Unknown sound '" + soundName + "' at " + path + ". Using default.");
            return defaultSound;
        }
        return sound;
    }

    private Particle loadParticle(FileConfiguration config, String path, Particle defaultParticle) {
        String particleName = config.getString(path);
        if (particleName == null || particleName.isBlank()) {
            return defaultParticle;
        }

        try {
            return Particle.valueOf(particleName.toUpperCase().replace('.', '_'));
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown particle '" + particleName + "' at " + path + ". Using default.");
            return defaultParticle;
        }
    }
}
