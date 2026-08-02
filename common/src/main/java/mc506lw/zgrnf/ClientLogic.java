package mc506lw.zgrnf;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * Loader-agnostic client logic. The loader module registers the key mapping and
 * calls tick()/onJoin() from its own event system.
 */
public final class ClientLogic {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("zgrnf", "categories"));

    public static final KeyMapping OPEN_KEY =
            new KeyMapping("key.zgrnf.open", InputConstants.Type.KEYSYM, InputConstants.KEY_G, CATEGORY);

    public static final KeyMapping TOGGLE_KEY =
            new KeyMapping("key.zgrnf.toggle", InputConstants.Type.KEYSYM, InputConstants.KEY_Y, CATEGORY);

    private static long lastVolumeSend;
    private static boolean lastJumping;

    private ClientLogic() {
    }

    public static void init(NetworkClient net) {
        MusicPlayer.INSTANCE.addStateListener(playing ->
                net.sendState(playing, Math.round(MusicPlayer.INSTANCE.getVolume()), lastJumping));
    }

    public static void tick(Minecraft client, NetworkClient net) {
        while (OPEN_KEY.consumeClick()) {
            openFlightScreen(client);
        }
        while (TOGGLE_KEY.consumeClick()) {
            MusicPlayer.INSTANCE.toggle();
        }
        boolean jump = client.options.keyJump.isDown();
        boolean playing = MusicPlayer.INSTANCE.isPlaying();
        long now = System.currentTimeMillis();
        if (jump != lastJumping || (playing && now - lastVolumeSend >= 1000)) {
            lastJumping = jump;
            lastVolumeSend = now;
            net.sendState(playing, Math.round(MusicPlayer.INSTANCE.getVolume()), jump);
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
        net.sendState(MusicPlayer.INSTANCE.isPlaying(), Math.round(MusicPlayer.INSTANCE.getVolume()), lastJumping);
    }
}
