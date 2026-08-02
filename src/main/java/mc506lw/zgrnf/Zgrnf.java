package mc506lw.zgrnf;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Companion plugin for the zgrnf Fabric client mod. Listens on the
 * "zgrnf:flight" channel (2 bytes: state + volume 0-100): while the client is
 * playing music the player gets flight, and the volume becomes their flight
 * speed (100 = maxFlySpeed, 0 = hover in place). Pausing instantly cancels the
 * flight. Supports whitelist/blacklist/OFF modes via an editable, hot-reloadable
 * config.yml. All entity work is scheduled on the player's entity scheduler so
 * it is safe on both Paper and Folia.
 */
public final class Zgrnf extends JavaPlugin implements PluginMessageListener {

    public static final String CHANNEL = "zgrnf:flight";

    public enum Mode { WHITELIST, BLACKLIST, OFF }

    private static final class PlayerState {
        boolean hasMod;
        boolean playing;
        int volume;
    }

    private final Map<UUID, PlayerState> states = new HashMap<>();

    private Mode mode = Mode.OFF;
    private final Set<String> whitelist = new LinkedHashSet<>();
    private final Set<String> blacklist = new LinkedHashSet<>();
    private float maxFlySpeed = 0.05f;

    @Override
    public void onEnable() {
        loadConfig();
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getPluginManager().registerEvents(new ZgrnfListener(this), this);
        ZgrnfCommand command = new ZgrnfCommand(this);
        getServer().getPluginCommand("zgrnf").setExecutor(command);
        getServer().getPluginCommand("zgrnf").setTabCompleter(command);
        getLogger().info("zgrnf enabled, listening on " + CHANNEL);
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL);
        for (UUID id : new ArrayList<>(states.keySet())) {
            Player player = getServer().getPlayer(id);
            if (player != null) {
                disableFlight(player);
            }
        }
        states.clear();
    }

    public void reload() {
        loadConfig();
        for (Player player : new ArrayList<>(getServer().getOnlinePlayers())) {
            PlayerState st = states.get(player.getUniqueId());
            if (st != null) {
                scheduleFlight(player, st.playing, st.volume);
            }
        }
    }

    private void loadConfig() {
        File file = new File(getDataFolder(), "config.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        mode = parseMode(cfg.getString("mode"));
        whitelist.clear();
        for (String s : cfg.getStringList("whitelist")) {
            whitelist.add(norm(s));
        }
        blacklist.clear();
        for (String s : cfg.getStringList("blacklist")) {
            blacklist.add(norm(s));
        }
        maxFlySpeed = (float) Math.max(0.0, Math.min(1.0, cfg.getDouble("maxFlySpeed", 0.05)));
        saveConfig(file, cfg);
    }

    private void saveConfig(File file, YamlConfiguration cfg) {
        cfg.set("mode", mode.name().toLowerCase(Locale.ROOT));
        cfg.set("whitelist", new ArrayList<>(whitelist));
        cfg.set("blacklist", new ArrayList<>(blacklist));
        cfg.set("maxFlySpeed", maxFlySpeed);
        cfg.set("comment", "mode: whitelist/blacklist/off; whitelist+blacklist are lowercase player names; maxFlySpeed is the Minecraft fly speed at volume 100 (vanilla creative = 0.05)");
        try {
            cfg.save(file);
        } catch (Exception ignored) {
        }
    }

    void onQuit(Player player) {
        states.remove(player.getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || player == null || message.length < 2) {
            return;
        }
        boolean playing = message[0] != 0;
        int volume = message[1] & 0xFF;
        scheduleFlight(player, playing, volume);
    }

    private void scheduleFlight(Player player, boolean playing, int volume) {
        player.getScheduler().run(this, task -> {
            if (!player.isOnline()) {
                return;
            }
            PlayerState st = states.computeIfAbsent(player.getUniqueId(), k -> new PlayerState());
            st.hasMod = true;
            st.playing = playing;
            st.volume = volume;
            if (playing) {
                enableFlight(player, st);
            } else {
                disableFlight(player);
            }
        }, () -> {
        });
    }

    private void enableFlight(Player player, PlayerState st) {
        int vol = Math.max(0, Math.min(100, st.volume));
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (!isAllowed(player.getName())) {
            disableFlight(player);
            return;
        }
        player.setAllowFlight(true);
        player.setFlying(true);
        // Paper divides the value by 2 internally, so 2x makes maxFlySpeed == flyingSpeed.
        player.setFlySpeed(2f * maxFlySpeed * (vol / 100f));
    }

    private void disableFlight(Player player) {
        boolean wasActive = states.get(player.getUniqueId()) != null && player.getAllowFlight();
        if (wasActive || player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFlySpeed(0.1f);
        }
    }

    boolean isAllowed(String playerName) {
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

    Mode getMode() {
        return mode;
    }

    void setMode(Mode m) {
        mode = m;
    }

    Set<String> getWhitelist() {
        return whitelist;
    }

    Set<String> getBlacklist() {
        return blacklist;
    }

    float getMaxFlySpeed() {
        return maxFlySpeed;
    }

    List<String> listPlayersWithMod() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<UUID, PlayerState> e : states.entrySet()) {
            if (e.getValue().hasMod) {
                Player p = getServer().getPlayer(e.getKey());
                if (p != null) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }

    List<String> listFlying() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<UUID, PlayerState> e : states.entrySet()) {
            PlayerState st = e.getValue();
            if (st.playing) {
                Player p = getServer().getPlayer(e.getKey());
                if (p != null) {
                    out.add(p.getName() + " (" + st.volume + "%)");
                }
            }
        }
        return out;
    }

    static Mode parseMode(String s) {
        if (s == null) {
            return Mode.OFF;
        }
        try {
            return Mode.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return Mode.OFF;
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
