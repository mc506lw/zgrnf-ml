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

    public enum FlightMode { CREATIVE, JETPACK }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private Mode mode = Mode.OFF;
    private final Set<String> whitelist = new LinkedHashSet<>();
    private final Set<String> blacklist = new LinkedHashSet<>();
    private float maxFlySpeed = 0.05f;
    private FlightMode flightMode = FlightMode.CREATIVE;
    private boolean particles = true;
    private float jetpackPower = 0.5f;

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
                flightMode = parseFlightMode(data.flightMode);
                particles = data.particles;
                jetpackPower = clamp(data.jetpackPower, 0.1f, 1.0f);
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

    public FlightMode getFlightMode() {
        return flightMode;
    }

    public void setFlightMode(FlightMode m) {
        flightMode = m;
    }

    public boolean isParticlesEnabled() {
        return particles;
    }

    public void setParticlesEnabled(boolean enabled) {
        particles = enabled;
    }

    public float getJetpackPower() {
        return jetpackPower;
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

    public static FlightMode parseFlightMode(String s) {
        if (s == null) {
            return FlightMode.CREATIVE;
        }
        try {
            return FlightMode.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return FlightMode.CREATIVE;
        }
    }

    private Data snapshot() {
        Data d = new Data();
        d.mode = mode.name().toLowerCase(Locale.ROOT);
        d.whitelist = new ArrayList<>(whitelist);
        d.blacklist = new ArrayList<>(blacklist);
        d.maxFlySpeed = maxFlySpeed;
        d.flightMode = flightMode.name().toLowerCase(Locale.ROOT);
        d.particles = particles;
        d.jetpackPower = jetpackPower;
        return d;
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static float clamp(double d) {
        return clamp(d, 0.0f, 1.0f);
    }

    private static float clamp(double d, float min, float max) {
        if (!Double.isFinite(d)) {
            return min;
        }
        return (float) Math.max(min, Math.min(max, d));
    }

    private static final class Data {
        String _comment = "zgrnf 服务端配置(中文说明):mode 放行规则(whitelist 白名单 / blacklist 黑名单 / off 全部放行);whitelist/blacklist 为小写玩家名列表;maxFlySpeed 音量100%时的飞行速度(原版创造约0.05);flightMode 飞行模式(creative 创造原生飞行,玩家双击空格起飞、速度随音量;jetpack 喷气背包,双击空格起飞后持续喷气上升并冒音符粒子,推力随 jetpackPower 与音量);particles 是否开启飞行时的音符粒子特效(true/false);jetpackPower 喷气推力系数(0.1~1.0,仅 jetpack 模式有效)。改完执行 /zgrnf reload 生效。";
        String mode = "off";
        List<String> whitelist = new ArrayList<>();
        List<String> blacklist = new ArrayList<>();
        float maxFlySpeed = 0.05f;
        String flightMode = "creative";
        boolean particles = true;
        float jetpackPower = 0.5f;
    }
}
