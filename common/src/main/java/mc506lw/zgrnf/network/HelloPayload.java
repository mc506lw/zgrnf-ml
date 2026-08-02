package mc506lw.zgrnf.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server "I have the zgrnf mod installed" presence packet, sent once
 * on every play-connection join. Wire format: 1 byte protocol version.
 */
public record HelloPayload(byte version) implements CustomPacketPayload {

    public static final Type<HelloPayload> TYPE = new Type<>(Identifier.parse("zgrnf:hello"));

    public static final StreamCodec<ByteBuf, HelloPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, HelloPayload::version,
            HelloPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
