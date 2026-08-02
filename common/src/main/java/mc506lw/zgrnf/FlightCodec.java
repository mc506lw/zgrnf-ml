package mc506lw.zgrnf;

/**
 * Pure byte-level wire codec shared by every loader. The wire format is
 * deliberately simple and endianness-independent so any client (Fabric, Forge,
 * NeoForge) can talk to any server (modded or the Paper/Folia plugin):
 * <ul>
 *   <li>flight channel "zgrnf:flight": 1 byte state (1=playing) + 1 byte volume
 *       (0-100) + 1 byte jump (1=space held, added in protocol 2)</li>
 *   <li>hello channel "zgrnf:hello": 1 byte protocol version</li>
 * </ul>
 * The decoder is backwards compatible with 2-byte protocol-1 messages (jump=0).
 */
public final class FlightCodec {

    public static final String FLIGHT_CHANNEL = "zgrnf:flight";
    public static final String HELLO_CHANNEL = "zgrnf:hello";
    public static final byte PROTOCOL_VERSION = 2;

    private FlightCodec() {
    }

    public static byte[] encodeFlight(boolean playing, int volume, boolean jumping) {
        return new byte[]{
                (byte) (playing ? 1 : 0),
                (byte) Math.max(0, Math.min(100, volume)),
                (byte) (jumping ? 1 : 0)
        };
    }

    public static byte[] encodeHello() {
        return new byte[]{PROTOCOL_VERSION};
    }

    public record FlightMessage(boolean playing, int volume, boolean jumping) {
    }

    public static FlightMessage decodeFlight(byte[] data) {
        if (data == null || data.length < 2) {
            return new FlightMessage(false, 0, false);
        }
        boolean jumping = data.length >= 3 && data[2] != 0;
        return new FlightMessage(data[0] != 0, data[1] & 0xFF, jumping);
    }
}
