package mc506lw.zgrnf;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ZgrnfListener implements Listener {

    private final Zgrnf plugin;

    ZgrnfListener(Zgrnf plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.onQuit(event.getPlayer());
    }
}
