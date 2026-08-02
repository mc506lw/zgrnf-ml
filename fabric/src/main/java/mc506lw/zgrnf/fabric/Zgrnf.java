package mc506lw.zgrnf.fabric;

import mc506lw.zgrnf.FlightServer;
import mc506lw.zgrnf.ServerConfig;
import mc506lw.zgrnf.network.FlightPayload;
import mc506lw.zgrnf.network.HelloPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class Zgrnf implements ModInitializer {

    private FlightServer server;

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(FlightPayload.TYPE, FlightPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);

        server = new FlightServer(new ServerConfig(FabricLoader.getInstance().getConfigDir()));
        server.loadConfig();

        ServerPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE, (payload, ctx) ->
                server.onHello(ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(FlightPayload.TYPE, (payload, ctx) ->
                server.onFlight(ctx.player(), payload.state() != 0, payload.volume()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, minecraftServer) ->
                server.onDisconnect(handler.getPlayer().getUUID()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                server.registerCommands(dispatcher));
    }
}
