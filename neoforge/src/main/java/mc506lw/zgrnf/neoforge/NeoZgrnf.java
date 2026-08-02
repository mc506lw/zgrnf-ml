package mc506lw.zgrnf.neoforge;

import mc506lw.zgrnf.Constants;
import mc506lw.zgrnf.FlightCodec;
import mc506lw.zgrnf.FlightServer;
import mc506lw.zgrnf.ServerConfig;
import mc506lw.zgrnf.network.FlightPayload;
import mc506lw.zgrnf.network.HelloPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
        NeoForge.EVENT_BUS.addListener(this::onServerTick);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoClient.init(modBus);
        }
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Byte.toString(FlightCodec.PROTOCOL_VERSION));
        registrar.playToServer(HelloPayload.TYPE, HelloPayload.CODEC,
                (payload, ctx) -> server.onHello((ServerPlayer) ctx.player()));
        registrar.playToServer(FlightPayload.TYPE, FlightPayload.CODEC,
                (payload, ctx) -> server.onFlight((ServerPlayer) ctx.player(), payload.state() != 0,
                        payload.volume(), payload.jump() != 0));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        server.registerCommands(event.getDispatcher());
    }

    private void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            if (!server.isFlyingActive(p)) {
                continue;
            }
            boolean jumping = server.isJumping(p);
            if (server.isJetpackMode()) {
                // Jetpack: flying state follows the jump key. Hold space to
                // thrust up, release to fall freely (with fall damage).
                boolean wantFlying = jumping;
                if (p.getAbilities().flying != wantFlying) {
                    p.getAbilities().flying = wantFlying;
                    p.connection.send(new ClientboundPlayerAbilitiesPacket(p.getAbilities()));
                }
                if (jumping) {
                    double thrust = 0.15 + server.config().getJetpackPower()
                            * (0.3 + 0.5 * server.getVolume(p) / 100.0);
                    p.push(0, thrust * 0.1, 0);
                    if (server.isParticlesEnabled()) {
                        spawnNote(p);
                    }
                }
            } else if (p.getAbilities().flying) {
                if (server.isParticlesEnabled()) {
                    spawnNote(p);
                }
            }
        }
    }

    private void spawnNote(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        int note = level.getRandom().nextInt(25);
        level.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 1.0, p.getZ(),
                1, 0, 0, 0, note / 24.0);
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        server.onDisconnect(event.getEntity().getUUID());
    }
}
