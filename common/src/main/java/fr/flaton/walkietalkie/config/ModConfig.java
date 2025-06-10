package fr.flaton.walkietalkie.config;

import fr.flaton.walkietalkie.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class ModConfig {

    private final File configFile;

    // --- Radio Settings ---
    public static int maxCanal = 16;
    public static boolean crossDimensionsEnabled = false;
    public static boolean applyDimensionScale = true;

    // --- Walkie-Talkie Tiers ---
    public static int woodenWalkieTalkieRange = 128;
    public static int stoneWalkieTalkieRange = 256;
    public static int ironWalkieTalkieRange = 512;
    public static int goldenWalkieTalkieRange = 1024;
    public static int diamondWalkieTalkieRange = 2048;
    public static int netheriteWalkieTalkieRange = 4096;

    // --- Speaker Settings ---
    public static int speakerDistance = 32;
    public static boolean voiceDuplication = false;

    // --- НОВЫЕ НАСТРОЙКИ ЗВУКОВЫХ ЭФФЕКТОВ ---
    public static boolean enablePttSounds = true;
    public static boolean enablePassiveNoise = true;
    public static float passiveNoiseVolume = 0.15f;


    public ModConfig(Path configFolder) {
        this.configFile = new File(configFolder.toString(), "WalkieTalkie.properties");
    }

    public void loadModConfig() {
        Properties properties = new Properties();

        if (configFile.exists()) {
            try (FileInputStream stream = new FileInputStream(configFile)) {
                properties.load(stream);
            } catch (IOException e) {
                Constants.LOG.error("Failed to load config file!", e);
            }
        }

        // --- Radio Settings ---
        maxCanal = getInt(properties, "max-canal", maxCanal);
        crossDimensionsEnabled = getBoolean(properties, "cross-dimensions-enabled", crossDimensionsEnabled);
        applyDimensionScale = getBoolean(properties, "apply-dimension-scale", applyDimensionScale);
        
        // --- Walkie-Talkie Tiers ---
        woodenWalkieTalkieRange = getInt(properties, "wooden-walkie-talkie-range", woodenWalkieTalkieRange);
        stoneWalkieTalkieRange = getInt(properties, "stone-walkie-talkie-range", stoneWalkieTalkieRange);
        ironWalkieTalkieRange = getInt(properties, "iron-walkie-talkie-range", ironWalkieTalkieRange);
        goldenWalkieTalkieRange = getInt(properties, "golden-walkie-talkie-range", goldenWalkieTalkieRange);
        diamondWalkieTalkieRange = getInt(properties, "diamond-walkie-talkie-range", diamondWalkieTalkieRange);
        netheriteWalkieTalkieRange = getInt(properties, "netherite-walkie-talkie-range", netheriteWalkieTalkieRange);
        
        // --- Speaker Settings ---
        speakerDistance = getInt(properties, "speaker-distance", speakerDistance);
        voiceDuplication = getBoolean(properties, "voice-duplication", voiceDuplication);

        // --- Sound Effect Settings ---
        enablePttSounds = getBoolean(properties, "enable-ptt-sounds", enablePttSounds);
        enablePassiveNoise = getBoolean(properties, "enable-passive-noise", enablePassiveNoise);
        passiveNoiseVolume = getFloat(properties, "passive-noise-volume", passiveNoiseVolume);


        createOrUpdateConfig();
    }

    private void createOrUpdateConfig() {
        StringBuilder sb = new StringBuilder();

        sb.append("# Walkie-Talkie Mod Configuration\n");

        sb.append("\n# --- Radio Settings ---\n");
        addIntOption(sb, "max-canal", maxCanal, "The maximum number of channels available.");
        addBooleanOption(sb, "cross-dimensions-enabled", crossDimensionsEnabled, "Allows communication across different dimensions (e.g., Overworld to Nether).");
        addBooleanOption(sb, "apply-dimension-scale", applyDimensionScale, "Applies dimension scale to range when crossing dimensions (e.g., Nether range is 8x smaller). Only works if cross-dimensions are enabled.");

        sb.append("\n# --- Walkie-Talkie Tiers ---\n");
        sb.append("# Defines the maximum transmission range in blocks for each walkie-talkie tier.\n");
        addIntOption(sb, "wooden-walkie-talkie-range", woodenWalkieTalkieRange, "");
        addIntOption(sb, "stone-walkie-talkie-range", stoneWalkieTalkieRange, "");
        addIntOption(sb, "iron-walkie-talkie-range", ironWalkieTalkieRange, "");
        addIntOption(sb, "golden-walkie-talkie-range", goldenWalkieTalkieRange, "");
        addIntOption(sb, "diamond-walkie-talkie-range", diamondWalkieTalkieRange, "");
        addIntOption(sb, "netherite-walkie-talkie-range", netheriteWalkieTalkieRange, "");

        sb.append("\n# --- Speaker Settings ---\n");
        addIntOption(sb, "speaker-distance", speakerDistance, "The maximum distance in blocks a player can be from a speaker to hear it.");
        addBooleanOption(sb, "voice-duplication", voiceDuplication, "If true, players will hear voices from both speakers and other players' walkie-talkies simultaneously.");

        sb.append("\n# --- Sound Effect Settings ---\n");
        addBooleanOption(sb, "enable-ptt-sounds", enablePttSounds, "Enable the ON/OFF click sounds when starting/stopping to talk (Push-to-Talk).");
        addBooleanOption(sb, "enable-passive-noise", enablePassiveNoise, "Enable the constant background hiss when a walkie-talkie is powered on but not in use.");
        addFloatOption(sb, "passive-noise-volume", passiveNoiseVolume, "The volume of the passive background hiss. Recommended range: 0.0 to 1.0. Default: 0.15");


        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(sb.toString());
        } catch (IOException e) {
            Constants.LOG.error("Failed to write config file!", e);
        }
    }

    // --- Helper methods for cleaner code ---

    private void addIntOption(StringBuilder sb, String key, int value, String comment) {
        if (!comment.isEmpty()) sb.append("# ").append(comment).append("\n");
        sb.append(key).append("=").append(value).append("\n");
    }

    private void addBooleanOption(StringBuilder sb, String key, boolean value, String comment) {
        if (!comment.isEmpty()) sb.append("# ").append(comment).append("\n");
        sb.append(key).append("=").append(value).append("\n");
    }

    private void addFloatOption(StringBuilder sb, String key, float value, String comment) {
        if (!comment.isEmpty()) sb.append("# ").append(comment).append("\n");
        sb.append(key).append("=").append(value).append("\n");
    }

    private int getInt(Properties props, String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBoolean(Properties props, String key, boolean defaultValue) {
        return Boolean.parseBoolean(props.getProperty(key, String.valueOf(defaultValue)));
    }
    
    private float getFloat(Properties props, String key, float defaultValue) {
        try {
            return Float.parseFloat(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}