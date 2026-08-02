package mc506lw.zgrnf;

import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Plays the local MP3 on a background thread. Exposes progress, duration,
 * volume (0-100) and play/pause state used by both the UI and the network layer.
 */
public final class MusicPlayer {

    public static final MusicPlayer INSTANCE = new MusicPlayer();

    private static final int SAMPLE_RATE = 44100;
    private static final int BITS = 16;
    private static final int CHANNELS = 2;

    private final Object lock = new Object();
    private final List<Consumer<Boolean>> stateListeners = new ArrayList<>();
    private final List<Consumer<Float>> volumeListeners = new ArrayList<>();

    private Thread thread;
    private File file;
    private AudioInputStream pcmStream;
    private SourceDataLine line;

    private boolean playing;
    private boolean paused;
    private boolean running;
    private float volume = 80f;
    private long durationMs;
    private long bytesPlayed;
    private long bytesPerSecond = SAMPLE_RATE * (BITS / 8) * CHANNELS;
    private String errorMessage;

    private MusicPlayer() {
    }

    public void addStateListener(Consumer<Boolean> listener) {
        synchronized (lock) {
            stateListeners.add(listener);
        }
    }

    public void addVolumeListener(Consumer<Float> listener) {
        synchronized (lock) {
            volumeListeners.add(listener);
        }
    }

    public boolean isPlaying() {
        synchronized (lock) {
            return playing && !paused;
        }
    }

    public boolean isPaused() {
        synchronized (lock) {
            return paused;
        }
    }

    public boolean hasError() {
        synchronized (lock) {
            return errorMessage != null;
        }
    }

    public String getErrorMessage() {
        synchronized (lock) {
            return errorMessage;
        }
    }

    public long getPositionMs() {
        synchronized (lock) {
            return bytesPlayed * 1000L / bytesPerSecond;
        }
    }

    public long getDurationMs() {
        synchronized (lock) {
            return durationMs;
        }
    }

    public float getVolume() {
        synchronized (lock) {
            return volume;
        }
    }

    public void setVolume(float v) {
        boolean fire;
        synchronized (lock) {
            float clamped = Math.max(0f, Math.min(100f, v));
            fire = clamped != volume;
            volume = clamped;
            lock.notifyAll();
        }
        if (fire) {
            fireVolume();
        }
    }

    public void toggle() {
        if (isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    public void play() {
        synchronized (lock) {
            if (playing) {
                paused = false;
                lock.notifyAll();
                fireState();
                return;
            }
            File file = extractBundledSong();
            if (file == null) {
                errorMessage = "Bundled song (assets/zgrnf/song.mp3) is missing!";
                fireState();
                return;
            }
            stopLocked();
            errorMessage = null;
            try {
                open(file);
            } catch (Exception e) {
                errorMessage = "Cannot play: " + e.getMessage();
                closeLineLocked();
                fireState();
                return;
            }
            running = true;
            playing = true;
            paused = false;
            thread = new Thread(this::runLoop, "zgrnf-music");
            thread.setDaemon(true);
            thread.start();
            fireState();
        }
    }

    public void pause() {
        synchronized (lock) {
            if (!playing) {
                return;
            }
            paused = true;
            if (line != null) {
                line.flush();
            }
            lock.notifyAll();
            fireState();
        }
    }

    public void stop() {
        synchronized (lock) {
            stopLocked();
        }
    }

    private void stopLocked() {
        running = false;
        paused = false;
        playing = false;
        Thread t = thread;
        thread = null;
        if (t != null) {
            t.interrupt();
        }
        lock.notifyAll();
        closeLineLocked();
        fireState();
    }

    private void runLoop() {
        byte[] buf = new byte[8192];
        while (true) {
            synchronized (lock) {
                while (running && paused) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (!running) {
                    return;
                }
            }
            try {
                int n;
                synchronized (lock) {
                    n = pcmStream.read(buf);
                }
                if (n <= 0) {
                    boolean reopened = false;
                    synchronized (lock) {
                        if (!running) {
                            break;
                        }
                        try {
                            pcmStream.close();
                            pcmStream = openStreamLocked();
                            reopened = true;
                        } catch (Exception e) {
                            if (errorMessage == null) {
                                errorMessage = "Loop error: " + e.getMessage();
                            }
                        }
                    }
                    if (!reopened) {
                        finish();
                        return;
                    }
                    continue;
                }
                float vol;
                synchronized (lock) {
                    vol = volume;
                }
                applyVolume(buf, n, (vol + 30) / 100f);
                synchronized (lock) {
                    bytesPlayed += n;
                }
                SourceDataLine out = line;
                if (out != null) {
                    out.write(buf, 0, n);
                }
            } catch (IOException e) {
                if (running) {
                    finish();
                }
                return;
            }
        }
    }

    private void finish() {
        synchronized (lock) {
            running = false;
            playing = false;
            paused = false;
            if (line != null) {
                line.drain();
            }
            closeLineLocked();
        }
        fireState();
    }

    private File extractBundledSong() {
        try (InputStream in = MusicPlayer.class.getResourceAsStream("/assets/zgrnf/song.mp3")) {
            if (in == null) {
                return null;
            }
            Path temp = Files.createTempFile("zgrnf-song", ".mp3");
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            return temp.toFile();
        } catch (IOException e) {
            return null;
        }
    }

    private void open(File file) throws Exception {
        this.file = file;
        pcmStream = openStreamLocked();
        AudioFormat actual = pcmStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, actual);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(actual);
        line.start();
    }

    private AudioInputStream openStreamLocked() throws Exception {
        MpegAudioFileReader reader = new MpegAudioFileReader();
        AudioFileFormat aff = reader.getAudioFileFormat(file);
        Object durationProp = aff.getProperty("duration");
        long micros = durationProp instanceof Number n ? n.longValue() : -1L;

        AudioInputStream mp3 = reader.getAudioInputStream(file);
        AudioFormat pcmFormat = new AudioFormat(SAMPLE_RATE, BITS, CHANNELS, true, false);
        AudioInputStream stream = new MpegFormatConversionProvider().getAudioInputStream(pcmFormat, mp3);
        AudioFormat actual = stream.getFormat();

        float sampleRate = actual.getSampleRate();
        int frameSize = actual.getFrameSize();
        if (sampleRate > 0 && frameSize > 0) {
            bytesPerSecond = Math.round(sampleRate * frameSize);
        }

        if (micros > 0) {
            durationMs = micros / 1000;
        } else {
            durationMs = estimateDurationMs(file, stream, aff, sampleRate, frameSize);
        }

        bytesPlayed = 0;
        return stream;
    }

    private long estimateDurationMs(File file, AudioInputStream stream, AudioFileFormat aff, float sampleRate, int frameSize) {
        long frames = stream.getFrameLength();
        if (frames > 0 && sampleRate > 0) {
            return frames * 1000L / (long) sampleRate;
        }
        Object bitrateProp = aff.getProperty("mp3.bitrate");
        double bitrateBps = 128000d;
        if (bitrateProp instanceof Number b) {
            double v = b.doubleValue();
            bitrateBps = v < 1000 ? v * 1000 : v;
        }
        if (file.length() > 0 && bitrateBps > 0) {
            return (long) (file.length() * 8d / bitrateBps * 1000d);
        }
        return 0;
    }

    private static void applyVolume(byte[] buf, int len, float factor) {
        for (int i = 0; i + 1 < len; i += 2) {
            int s = (short) ((buf[i] & 0xFF) | (buf[i + 1] << 8));
            s = (int) (s * factor);
            if (s > 32767) {
                s = 32767;
            } else if (s < -32768) {
                s = -32768;
            }
            buf[i] = (byte) (s & 0xFF);
            buf[i + 1] = (byte) ((s >> 8) & 0xFF);
        }
    }

    private void closeLineLocked() {
        try {
            if (pcmStream != null) {
                pcmStream.close();
            }
        } catch (IOException ignored) {
        }
        pcmStream = null;
        if (line != null) {
            try {
                line.flush();
                line.stop();
                line.close();
            } catch (RuntimeException ignored) {
            }
        }
        line = null;
    }

    private void fireState() {
        List<Consumer<Boolean>> listeners;
        synchronized (lock) {
            listeners = new ArrayList<>(stateListeners);
        }
        boolean nowPlaying = isPlaying();
        for (Consumer<Boolean> l : listeners) {
            try {
                l.accept(nowPlaying);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void fireVolume() {
        List<Consumer<Float>> listeners;
        synchronized (lock) {
            listeners = new ArrayList<>(volumeListeners);
        }
        float v = getVolume();
        for (Consumer<Float> l : listeners) {
            try {
                l.accept(v);
            } catch (RuntimeException ignored) {
            }
        }
    }
}
