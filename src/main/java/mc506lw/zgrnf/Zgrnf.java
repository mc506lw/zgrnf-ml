package mc506lw.zgrnf;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Companion plugin for the zgrnf Fabric client mod. Listens on the
 * "zgrnf:flight" channel (2 bytes: state + volume 0-100): while the client is
 * playing music the player gets flight, and the volume becomes their flight
 * speed (100 = maxFlySpeed, 0 = hover in place). Pausing instantly cancels the
 * flight.
 *
 * <p>Two flight modes are supported:
 * <ul>
 *   <li>CREATIVE - grant native flight ability, player double-taps space to
 *       take off, speed follows volume (vanilla creative feel).</li>
 *   <li>JETPACK - grant flight, then a per-tick upward thrust while flying so
 *       the player climbs like a jetpack; note particles are emitted.</li>
 * </ul>
 *
 * <p>Supports whitelist/blacklist/OFF modes via an editable, hot-reloadable
 * config.yml with Chinese comments. All entity work is scheduled on the
 * player's entity scheduler (Paper/Folia), falling back to the global
 * scheduler on Spigot, so it is safe on Paper, Spigot and Folia.
 */
public final class Zgrnf extends JavaPlugin implements PluginMessageListener {

    public static final String CHANNEL = "zgrnf:flight";

    public enum Mode { WHITELIST, BLACKLIST, OFF }
    public enum FlightMode { CREATIVE, JETPACK }

    private interface TickTask {
        void cancel();
    }

    private static final class PlayerState {
        boolean hasMod;
        boolean playing;
        int volume;
        boolean jumping;
        TickTask fx;
    }

    private final Map<UUID, PlayerState> states = new HashMap<>();

    private Mode mode = Mode.OFF;
    private final Set<String> whitelist = new LinkedHashSet<>();
    private final Set<String> blacklist = new LinkedHashSet<>();
    private float maxFlySpeed = 0.05f;
    private FlightMode flightMode = FlightMode.CREATIVE;
    private boolean particles = true;
    private float jetpackPower = 0.5f;

    @Override
    public void onEnable() {
        saveDefaultConfig();
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
                scheduleFlight(player, st.playing, st.volume, st.jumping);
            }
        }
    }

    /** Persist the in-memory config to config.yml (only call on deliberate changes). */
    public void save() {
        saveConfigFile();
    }

    private void loadConfig() {
        reloadConfig();
        YamlConfiguration cfg = (YamlConfiguration) getConfig();
        mode = parseMode(cfg.getString("mode"));
        whitelist.clear();
        for (String s : cfg.getStringList("whitelist")) {
            whitelist.add(norm(s));
        }
        blacklist.clear();
        for (String s : cfg.getStringList("blacklist")) {
            blacklist.add(norm(s));
        }
        maxFlySpeed = (float) clamp(cfg.getDouble("maxFlySpeed", 0.05), 0.0, 1.0);
        flightMode = parseFlightMode(cfg.getString("flight-mode"));
        particles = cfg.getBoolean("particles", true);
        jetpackPower = (float) clamp(cfg.getDouble("jetpack-power", 0.5), 0.1, 1.0);
    }

    private void saveConfigFile() {
        getConfig().set("mode", mode.name().toLowerCase(Locale.ROOT));
        getConfig().set("whitelist", new ArrayList<>(whitelist));
        getConfig().set("blacklist", new ArrayList<>(blacklist));
        getConfig().set("maxFlySpeed", maxFlySpeed);
        getConfig().set("flight-mode", flightMode.name().toLowerCase(Locale.ROOT));
        getConfig().set("particles", particles);
        getConfig().set("jetpack-power", jetpackPower);
        saveConfig();
    }

    void onQuit(Player player) {
        cancelFx(player);
        states.remove(player.getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || player == null || message.length < 2) {
            return;
        }
        boolean playing = message[0] != 0;
        int volume = message[1] & 0xFF;
        boolean jumping = message.length >= 3 && message[2] != 0;
        scheduleFlight(player, playing, volume, jumping);
    }

    private void scheduleFlight(Player player, boolean playing, int volume, boolean jumping) {
        player.getScheduler().run(this, task -> {
            if (!player.isOnline()) {
                return;
            }
            PlayerState st = states.computeIfAbsent(player.getUniqueId(), k -> new PlayerState());
            st.hasMod = true;
            st.playing = playing;
            st.volume = volume;
            st.jumping = jumping;
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
        // Grant the ability to fly, never force flying. The player still takes
        // off by double-tapping space, so pausing/walking never yanks them up.
        player.setAllowFlight(true);
        // Paper divides the value by 2 internally, so 2x maps maxFlySpeed to
        // flyingSpeed. Jetpack mode keeps a higher baseline thrust.
        float speed;
        if (flightMode == FlightMode.JETPACK) {
            speed = 2f * maxFlySpeed * (0.5f + 0.5f * (vol / 100f)) * (1f + jetpackPower);
        } else {
            speed = 2f * maxFlySpeed * (vol / 100f);
        }
        player.setFlySpeed(Math.max(0.05f, speed));
        startFxTask(player);
    }

    private void disableFlight(Player player) {
        cancelFx(player);
        GameMode gm = player.getGameMode();
        // Creative and spectator already have native flight; only reset speed.
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) {
            player.setFlySpeed(0.1f);
            return;
        }
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f);
    }

    private void startFxTask(Player player) {
        PlayerState st = states.get(player.getUniqueId());
        if (st == null || st.fx != null) {
            return;
        }
        st.fx = scheduleTick(player);
    }

    private void cancelFx(Player player) {
        PlayerState st = states.get(player.getUniqueId());
        if (st != null && st.fx != null) {
            st.fx.cancel();
            st.fx = null;
        }
    }

    private void tickFx(Player player) {
        try {
            PlayerState st = states.get(player.getUniqueId());
            if (st == null || !st.playing) {
                return;
            }
            if (flightMode == FlightMode.JETPACK) {
                // Jetpack: flying state follows the jump key. Hold space to
                // thrust up, release to fall freely (with fall damage).
                if (st.jumping) {
                    if (!player.isFlying()) {
                        player.setFlying(true);
                    }
                    jetpackBoost(player, st.volume);
                    if (particles) {
                        spawnNote(player);
                    }
                } else if (player.isFlying()) {
                    player.setFlying(false);
                }
            } else if (player.isFlying()) {
                if (particles) {
                    spawnNote(player);
                }
            }
        } catch (RuntimeException e) {
            getLogger().log(java.util.logging.Level.WARNING, "zgrnf fx tick failed", e);
        }
    }

    private TickTask scheduleTick(Player player) {
        // Paper/Folia: run on the player's entity scheduler so it stays safe
        // on Folia's per-region threads. The first parameter of runAtFixedRate
        // is declared as Plugin, not JavaPlugin, so match it exactly.
        try {
            Method getScheduler = Player.class.getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(player);
            Method run = scheduler.getClass().getMethod("runAtFixedRate",
                    org.bukkit.plugin.Plugin.class, Consumer.class, Runnable.class, long.class, long.class);
            Object[] holder = new Object[1];
            Consumer<Object> consumer = task -> tickFx(player);
            holder[0] = run.invoke(scheduler, this, consumer, (Runnable) () -> { }, 1L, 1L);
            return () -> {
                try {
                    holder[0].getClass().getMethod("cancel").invoke(holder[0]);
                } catch (ReflectiveOperationException ignored) {
                }
            };
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Spigot: global scheduler.
            final org.bukkit.scheduler.BukkitTask task = getServer().getScheduler()
                    .runTaskTimer(this, () -> tickFx(player), 1L, 1L);
            return task::cancel;
        }
    }

    private void jetpackBoost(Player player, int volume) {
        double thrust = 0.15 + jetpackPower * (0.3 + 0.5 * volume / 100.0);
        org.bukkit.util.Vector v = player.getVelocity();
        player.setVelocity(new org.bukkit.util.Vector(
                v.getX(),
                Math.max(v.getY() + thrust * 0.12, thrust * 0.7),
                v.getZ()));
    }

    private void spawnNote(Player player) {
        Location loc = player.getLocation();
        int note = (int) (Math.random() * 25);
        player.getWorld().spawnParticle(
                Particle.NOTE, loc.getX(), loc.getY() + 1.0, loc.getZ(),
                1, 0, 0, 0, note);
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

    FlightMode getFlightMode() {
        return flightMode;
    }

    void setFlightMode(FlightMode m) {
        flightMode = m;
    }

    boolean isParticlesEnabled() {
        return particles;
    }

    void setParticlesEnabled(boolean enabled) {
        particles = enabled;
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

    static FlightMode parseFlightMode(String s) {
        if (s == null) {
            return FlightMode.CREATIVE;
        }
        try {
            return FlightMode.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return FlightMode.CREATIVE;
        }
    }

    private static double clamp(double d, double min, double max) {
        if (!Double.isFinite(d)) {
            return min;
        }
        return Math.max(min, Math.min(max, d));
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
