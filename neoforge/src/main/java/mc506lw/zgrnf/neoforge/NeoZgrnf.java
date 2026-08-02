package mc506lw.zgrnf.neoforge;

import mc506lw.zgrnf.Constants;
import mc506lw.zgrnf.FlightCodec;
import mc506lw.zgrnf.FlightServer;
import mc506lw.zgrnf.ServerConfig;
import mc506lw.zgrnf.network.FlightPayload;
import mc506lw.zgrnf.network.HelloPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Constants.MOD_ID)
public class NeoZgrnf {

    private final FlightServer server;

    public NeoZgrnf(IEventBus modBus) {
        server = new FlightServer(new ServerConfig(FMLPaths.GAMEDIR.get()));
        server.loadConfig();

        modBus.addListener(this::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoClient.init(modBus);
        }
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Byte.toString(FlightCodec.PROTOCOL_VERSION));
        registrar.playToServer(HelloPayload.TYPE, HelloPayload.CODEC,
                (payload, ctx) -> server.onHello((ServerPlayer) ctx.player()));
        registrar.playToServer(FlightPayload.TYPE, FlightPayload.CODEC,
                (payload, ctx) -> server.onFlight((ServerPlayer) ctx.player(), payload.state() != 0, payload.volume()));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        server.registerCommands(event.getDispatcher());
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        server.onDisconnect(event.getEntity().getUUID());
    }
}
