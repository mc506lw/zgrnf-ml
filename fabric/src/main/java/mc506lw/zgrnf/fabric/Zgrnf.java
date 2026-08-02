package mc506lw.zgrnf.fabric;

import mc506lw.zgrnf.FlightServer;
import mc506lw.zgrnf.ServerConfig;
import mc506lw.zgrnf.network.FlightPayload;
import mc506lw.zgrnf.network.HelloPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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
                server.onFlight(ctx.player(), payload.state() != 0,
                        payload.volume(), payload.jump() != 0));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, minecraftServer) ->
                server.onDisconnect(handler.getPlayer().getUUID()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                server.registerCommands(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(net.minecraft.server.MinecraftServer minecraftServer) {
        for (ServerPlayer p : minecraftServer.getPlayerList().getPlayers()) {
            if (!server.isFlyingActive(p)) {
                continue;
            }
            boolean jumping = server.isJumping(p);
            if (server.isJetpackMode()) {
                // Jetpack: flying state follows the jump key. Holding space
                // enables flight (climb speed = flyingSpeed, scaled by volume),
                // releasing disables it so the player falls freely. No manual
                // velocity is written, so Minecraft's own flight physics stay
                // smooth.
                boolean wantFlying = jumping;
                if (p.getAbilities().flying != wantFlying) {
                    p.getAbilities().flying = wantFlying;
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket(p.getAbilities()));
                }
                if (jumping && server.isParticlesEnabled()) {
                    spawnNote(p);
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
}
