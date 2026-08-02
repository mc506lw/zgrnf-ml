package mc506lw.zgrnf.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server payload telling the server whether the player is "playing"
 * the music and at what volume (0-100). Wire format: 1 byte state (1 = playing,
 * 0 = paused/stopped) + 1 byte volume (0-100). Both are single bytes so the
 * wire format is endianness-independent and matches the Paper plugin.
 */
public record FlightPayload(byte state, int volume) implements CustomPacketPayload {

    public static final Type<FlightPayload> TYPE = new Type<>(Identifier.parse("zgrnf:flight"));

    public static final StreamCodec<ByteBuf, FlightPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, FlightPayload::state,
            StreamCodec.of((buf, v) -> buf.writeByte(v), buf -> (int) buf.readUnsignedByte()), FlightPayload::volume,
            FlightPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
