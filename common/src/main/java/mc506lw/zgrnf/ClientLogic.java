package mc506lw.zgrnf;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Loader-agnostic client logic. The loader module registers the key mapping and
 * calls tick()/onJoin() from its own event system.
 */
public final class ClientLogic {

    public static final KeyMapping OPEN_KEY =
            new KeyMapping("key.zgrnf.open", InputConstants.Type.KEYSYM, InputConstants.KEY_G, KeyMapping.Category.MISC);

    private static long lastVolumeSend;

    private ClientLogic() {
    }

    public static void init(NetworkClient net) {
        MusicPlayer.INSTANCE.addStateListener(playing ->
                net.sendState(playing, Math.round(MusicPlayer.INSTANCE.getVolume())));
    }

    public static void tick(Minecraft client, NetworkClient net) {
        while (OPEN_KEY.consumeClick()) {
            openFlightScreen(client);
        }
        long now = System.currentTimeMillis();
        if (MusicPlayer.INSTANCE.isPlaying() && now - lastVolumeSend >= 1000) {
            lastVolumeSend = now;
            net.sendState(true, Math.round(MusicPlayer.INSTANCE.getVolume()));
        }
    }

    private static void openFlightScreen(Minecraft client) {
        try {
            Minecraft.class.getMethod("setScreen", Screen.class).invoke(client, new FlightScreen());
        } catch (NoSuchMethodException e) {
            try {
                client.gui.getClass().getMethod("setScreen", Screen.class).invoke(client.gui, new FlightScreen());
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Unable to open flight screen", ex);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to open flight screen", e);
        }
    }

    public static void onJoin(NetworkClient net) {
        net.sendHello();
        if (MusicPlayer.INSTANCE.isPlaying()) {
            net.sendState(true, Math.round(MusicPlayer.INSTANCE.getVolume()));
        }
    }
}
