package mc506lw.zgrnf;

/**
 * Client-side network facade implemented by each loader module.
 */
public interface NetworkClient {

    void sendState(boolean playing, int volume);

    void sendHello();
}
