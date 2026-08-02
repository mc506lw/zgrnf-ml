package mc506lw.zgrnf;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Loader-agnostic server component. Grants flight while the client plays music.
 * The loader module is responsible for network registration and for calling the
 * lifecycle methods here (hello, flight, disconnect, commands).
 */
public final class FlightServer {

    private final ServerConfig config;
    private final Map<UUID, PlayerState> states = new HashMap<>();

    private static final class PlayerState {
        boolean hasMod;
        boolean playing;
        int volume;
    }

    public FlightServer(ServerConfig config) {
        this.config = config;
    }

    public ServerConfig config() {
        return config;
    }

    public void loadConfig() {
        config.load();
    }

    public void onHello(ServerPlayer player) {
        state(player).hasMod = true;
    }

    public void onFlight(ServerPlayer player, boolean playing, int volume) {
        PlayerState st = state(player);
        st.hasMod = true;
        st.playing = playing;
        st.volume = volume;
        applyFlight(player, st.playing, st.volume);
    }

    public void onDisconnect(UUID id) {
        states.remove(id);
    }

    private PlayerState state(ServerPlayer player) {
        return states.computeIfAbsent(player.getUUID(), k -> new PlayerState());
    }

    private void applyFlight(ServerPlayer player, boolean playing, int volume) {
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            return;
        }
        Abilities a = player.getAbilities();
        if (playing && config.isAllowed(player.getGameProfile().name())) {
            int vol = Math.max(0, Math.min(100, volume));
            a.mayfly = true;
            a.flying = true;
            a.setFlyingSpeed(config.getMaxFlySpeed() * vol / 100f);
        } else {
            a.mayfly = false;
            a.flying = false;
            a.setFlyingSpeed(0.05f);
        }
        player.connection.send(new ClientboundPlayerAbilitiesPacket(a));
    }

    public void reapplyAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerState st = states.get(p.getUUID());
            if (st != null) {
                applyFlight(p, st.playing, st.volume);
            }
        }
    }

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(cmd("zgrnf")
                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(cmd("reload").executes(ctx -> {
                    config.load();
                    reapplyAll(ctx.getSource().getServer());
                    success(ctx, "zgrnf: 配置已重载,当前模式 " + config.getMode());
                    return 1;
                }))
                .then(cmd("list").executes(this::cmdList))
                .then(cmd("mode")
                        .then(wordArg("mode").executes(this::cmdMode)))
                .then(listCommand(true))
                .then(listCommand(false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> cmd(String name) {
        return LiteralArgumentBuilder.<CommandSourceStack>literal(name);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> wordArg(String name) {
        return RequiredArgumentBuilder.argument(name, StringArgumentType.word());
    }

    private int cmdList(CommandContext<CommandSourceStack> ctx) {
        List<String> withMod = new ArrayList<>();
        List<String> flying = new ArrayList<>();
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            PlayerState st = states.get(p.getUUID());
            if (st != null && st.hasMod) {
                withMod.add(p.getGameProfile().name());
            }
            if (st != null && st.playing) {
                flying.add(p.getGameProfile().name() + " (" + st.volume + "%)");
            }
        }
        success(ctx, "zgrnf: 已装 mod(" + withMod.size() + "): " + String.join(", ", withMod));
        success(ctx, "zgrnf: 正在飞(" + flying.size() + "): " + String.join(", ", flying));
        return 1;
    }

    private int cmdMode(CommandContext<CommandSourceStack> ctx) {
        String m = StringArgumentType.getString(ctx, "mode");
        ServerConfig.Mode mode = ServerConfig.parseMode(m);
        config.setMode(mode);
        config.save();
        reapplyAll(ctx.getSource().getServer());
        success(ctx, "zgrnf: 模式已设为 " + mode);
        return 1;
    }

    private LiteralArgumentBuilder<CommandSourceStack> listCommand(boolean isWhitelist) {
        String name = isWhitelist ? "whitelist" : "blacklist";
        return cmd(name)
                .then(cmd("list").executes(ctx -> {
                    Set<String> set = isWhitelist ? config.getWhitelist() : config.getBlacklist();
                    success(ctx, "zgrnf: " + name + "(" + set.size() + "): " + String.join(", ", set));
                    return 1;
                }))
                .then(cmd("add")
                        .then(wordArg("player").executes(ctx -> editList(ctx, isWhitelist, true))))
                .then(cmd("remove")
                        .then(wordArg("player").executes(ctx -> editList(ctx, isWhitelist, false))));
    }

    private int editList(CommandContext<CommandSourceStack> ctx, boolean isWhitelist, boolean add) {
        String p = StringArgumentType.getString(ctx, "player").toLowerCase(Locale.ROOT);
        Set<String> set = isWhitelist ? config.getWhitelist() : config.getBlacklist();
        String label = isWhitelist ? "白名单" : "黑名单";
        boolean changed = add ? set.add(p) : set.remove(p);
        if (changed) {
            config.save();
            reapplyAll(ctx.getSource().getServer());
            success(ctx, "zgrnf: 已" + (add ? "加入" : "移出") + label + ": " + p);
        } else {
            success(ctx, "zgrnf: " + p + (add ? " 已在" : " 不在") + label + "中");
        }
        return 1;
    }

    private static void success(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}
