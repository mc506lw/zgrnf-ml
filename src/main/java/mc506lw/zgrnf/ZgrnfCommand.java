package mc506lw.zgrnf;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static mc506lw.zgrnf.Zgrnf.FlightMode;
import static mc506lw.zgrnf.Zgrnf.Mode;

public final class ZgrnfCommand implements CommandExecutor, TabCompleter {

    private final Zgrnf plugin;

    ZgrnfCommand(Zgrnf plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            send(sender, "用法: /zgrnf reload | list | mode <whitelist|blacklist|off> | flightmode <creative|jetpack> | particles <on|off> | whitelist add|remove|list <玩家> | blacklist add|remove|list <玩家>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                plugin.reload();
                send(sender, "zgrnf: 配置已重载,当前模式 " + plugin.getMode() + ",飞行模式 " + plugin.getFlightMode());
                return true;
            case "list":
                send(sender, "zgrnf: 已装 mod(" + plugin.listPlayersWithMod().size() + "): " + String.join(", ", plugin.listPlayersWithMod()));
                send(sender, "zgrnf: 正在飞(" + plugin.listFlying().size() + "): " + String.join(", ", plugin.listFlying()));
                return true;
            case "mode":
                if (args.length < 2) {
                    send(sender, "zgrnf: 当前模式 " + plugin.getMode());
                    return true;
                }
                plugin.setMode(Zgrnf.parseMode(args[1]));
                plugin.save();
                plugin.reload();
                send(sender, "zgrnf: 模式已设为 " + plugin.getMode());
                return true;
            case "flightmode":
                if (args.length < 2) {
                    send(sender, "zgrnf: 当前飞行模式 " + plugin.getFlightMode());
                    return true;
                }
                FlightMode old = plugin.getFlightMode();
                plugin.setFlightMode(Zgrnf.parseFlightMode(args[1]));
                plugin.save();
                plugin.reload();
                if (old != plugin.getFlightMode()) {
                    plugin.resetFlightState();
                }
                send(sender, "zgrnf: 飞行模式已设为 " + plugin.getFlightMode());
                return true;
            case "particles":
                if (args.length < 2) {
                    send(sender, "zgrnf: 粒子特效 " + (plugin.isParticlesEnabled() ? "开" : "关"));
                    return true;
                }
                plugin.setParticlesEnabled("on".equalsIgnoreCase(args[1]) || "true".equalsIgnoreCase(args[1]));
                plugin.save();
                plugin.reload();
                send(sender, "zgrnf: 粒子特效已" + (plugin.isParticlesEnabled() ? "开启" : "关闭"));
                return true;
            case "whitelist":
            case "blacklist":
                editList(sender, args[0].toLowerCase(Locale.ROOT).equals("whitelist"), args);
                return true;
            default:
                send(sender, "zgrnf: 未知子命令 " + args[0]);
                return true;
        }
    }

    private void editList(CommandSender sender, boolean isWhitelist, String[] args) {
        String label = isWhitelist ? "白名单" : "黑名单";
        Set<String> set = isWhitelist ? plugin.getWhitelist() : plugin.getBlacklist();
        if (args.length < 2) {
            send(sender, "用法: /zgrnf " + (isWhitelist ? "whitelist" : "blacklist") + " add|remove|list [玩家]");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list":
                send(sender, "zgrnf: " + label + "(" + set.size() + "): " + String.join(", ", set));
                return;
            case "add":
                if (args.length < 3) {
                    send(sender, "zgrnf: 请提供玩家名");
                    return;
                }
                if (set.add(args[2].toLowerCase(Locale.ROOT))) {
                    plugin.save();
                    plugin.reload();
                    send(sender, "zgrnf: 已加入" + label + ": " + args[2]);
                } else {
                    send(sender, "zgrnf: " + args[2] + " 已在" + label + "中");
                }
                return;
            case "remove":
                if (args.length < 3) {
                    send(sender, "zgrnf: 请提供玩家名");
                    return;
                }
                if (set.remove(args[2].toLowerCase(Locale.ROOT))) {
                    plugin.save();
                    plugin.reload();
                    send(sender, "zgrnf: 已移出" + label + ": " + args[2]);
                } else {
                    send(sender, "zgrnf: " + args[2] + " 不在" + label + "中");
                }
                return;
            default:
                send(sender, "zgrnf: 未知操作 " + args[1]);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : new String[]{"reload", "list", "mode", "flightmode", "particles", "whitelist", "blacklist"}) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && "mode".equalsIgnoreCase(args[0])) {
            for (Mode m : Mode.values()) {
                String n = m.name().toLowerCase(Locale.ROOT);
                if (n.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(n);
                }
            }
        } else if (args.length == 2 && "flightmode".equalsIgnoreCase(args[0])) {
            for (FlightMode m : FlightMode.values()) {
                String n = m.name().toLowerCase(Locale.ROOT);
                if (n.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(n);
                }
            }
        } else if (args.length == 2 && "particles".equalsIgnoreCase(args[0])) {
            for (String s : new String[]{"on", "off"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && ("whitelist".equalsIgnoreCase(args[0]) || "blacklist".equalsIgnoreCase(args[0]))) {
            for (String s : new String[]{"add", "remove", "list"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 3 && ("whitelist".equalsIgnoreCase(args[0]) || "blacklist".equalsIgnoreCase(args[0]))) {
            for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
                out.add(p.getName());
            }
        }
        return out;
    }

    private static void send(CommandSender sender, String text) {
        sender.sendMessage(text);
    }
}
