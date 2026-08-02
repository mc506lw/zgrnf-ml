package mc506lw.zgrnf.fabric;

import mc506lw.zgrnf.ClientLogic;
import mc506lw.zgrnf.FlightCodec;
import mc506lw.zgrnf.MusicPlayer;
import mc506lw.zgrnf.NetworkClient;
import mc506lw.zgrnf.network.FlightPayload;
import mc506lw.zgrnf.network.HelloPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ZgrnfClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(ClientLogic.OPEN_KEY);
        KeyMappingHelper.registerKeyMapping(ClientLogic.TOGGLE_KEY);
        NetworkClient net = new FabricNetworkClient();
        ClientLogic.init(net);
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientLogic.tick(client, net));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientLogic.onJoin(net));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MusicPlayer.INSTANCE.stop());
    }

    private static final class FabricNetworkClient implements NetworkClient {

        @Override
        public void sendState(boolean playing, int volume, boolean jumping) {
            try {
                if (ClientPlayNetworking.canSend(FlightPayload.TYPE)) {
                    ClientPlayNetworking.send(new FlightPayload((byte) (playing ? 1 : 0),
                            Math.max(0, Math.min(100, volume)), (byte) (jumping ? 1 : 0)));
                }
            } catch (RuntimeException ignored) {
            }
        }

        @Override
        public void sendHello() {
            try {
                if (ClientPlayNetworking.canSend(HelloPayload.TYPE)) {
                    ClientPlayNetworking.send(new HelloPayload(FlightCodec.PROTOCOL_VERSION));
                }
            } catch (RuntimeException ignored) {
            }
        }
    }
}
