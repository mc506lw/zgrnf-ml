package mc506lw.zgrnf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Editable server-side config shared by every loader. Stored as
 * zgrnf-server.json in the loader-provided config directory and hot-reloadable.
 */
public final class ServerConfig {

    public enum Mode { WHITELIST, BLACKLIST, OFF }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private Mode mode = Mode.OFF;
    private final Set<String> whitelist = new LinkedHashSet<>();
    private final Set<String> blacklist = new LinkedHashSet<>();
    private float maxFlySpeed = 0.05f;

    public ServerConfig(Path configDir) {
        this.file = configDir.resolve("zgrnf-server.json");
    }

    public void load() {
        try {
            if (!Files.isRegularFile(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(snapshot()), StandardCharsets.UTF_8);
                return;
            }
            Data data = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class);
            if (data != null) {
                mode = parseMode(data.mode);
                whitelist.clear();
                if (data.whitelist != null) {
                    for (String s : data.whitelist) {
                        whitelist.add(norm(s));
                    }
                }
                blacklist.clear();
                if (data.blacklist != null) {
                    for (String s : data.blacklist) {
                        blacklist.add(norm(s));
                    }
                }
                maxFlySpeed = clamp(data.maxFlySpeed);
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(snapshot()), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public boolean isAllowed(String playerName) {
        String name = norm(playerName);
        switch (mode) {
            case WHITELIST:
                return whitelist.contains(name);
            case BLACKLIST:
                return !blacklist.contains(name);
            default:
                return true;
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode m) {
        mode = m;
    }

    public Set<String> getWhitelist() {
        return whitelist;
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }

    public float getMaxFlySpeed() {
        return maxFlySpeed;
    }

    public static Mode parseMode(String s) {
        if (s == null) {
            return Mode.OFF;
        }
        try {
            return Mode.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return Mode.OFF;
        }
    }

    private Data snapshot() {
        Data d = new Data();
        d.mode = mode.name().toLowerCase(Locale.ROOT);
        d.whitelist = new ArrayList<>(whitelist);
        d.blacklist = new ArrayList<>(blacklist);
        d.maxFlySpeed = maxFlySpeed;
        return d;
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static float clamp(double d) {
        if (!Double.isFinite(d)) {
            return 0.05f;
        }
        return (float) Math.max(0.0, Math.min(1.0, d));
    }

    private static final class Data {
        String mode = "off";
        List<String> whitelist = new ArrayList<>();
        List<String> blacklist = new ArrayList<>();
        float maxFlySpeed = 0.05f;
    }
}
