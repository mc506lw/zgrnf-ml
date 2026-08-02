package mc506lw.zgrnf.neoforge;

import mc506lw.zgrnf.ClientLogic;
import mc506lw.zgrnf.FlightCodec;
import mc506lw.zgrnf.MusicPlayer;
import mc506lw.zgrnf.NetworkClient;
import mc506lw.zgrnf.network.FlightPayload;
import mc506lw.zgrnf.network.HelloPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

final class NeoClient {

    private static final NetworkClient NET = new NeoNetworkClient();

    private NeoClient() {
    }

    static void init(IEventBus modBus) {
        ClientLogic.init(NET);
        modBus.addListener(NeoClient::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(NeoClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(NeoClient::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(NeoClient::onLoggingOut);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClientLogic.OPEN_KEY);
        event.register(ClientLogic.TOGGLE_KEY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientLogic.tick(Minecraft.getInstance(), NET);
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientLogic.onJoin(NET);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MusicPlayer.INSTANCE.stop();
    }

    private static final class NeoNetworkClient implements NetworkClient {

        @Override
        public void sendState(boolean playing, int volume, boolean jumping) {
            try {
                ClientPacketDistributor.sendToServer(new FlightPayload(
                        (byte) (playing ? 1 : 0), Math.max(0, Math.min(100, volume)),
                        (byte) (jumping ? 1 : 0)));
            } catch (RuntimeException ignored) {
            }
        }

        @Override
        public void sendHello() {
            try {
                ClientPacketDistributor.sendToServer(new HelloPayload(FlightCodec.PROTOCOL_VERSION));
            } catch (RuntimeException ignored) {
            }
        }
    }
}
