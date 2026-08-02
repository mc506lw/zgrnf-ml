package mc506lw.zgrnf;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FlightScreen extends Screen {

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 230;
    private static final int ACCENT = 0xFF4DA6FF;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int TRACK = 0x4DFFFFFF;

    private Button playButton;
    private VerticalVolumeSlider volumeSlider;

    public FlightScreen() {
        super(Component.literal("Chinese Can Fly"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int panelX = cx - PANEL_W / 2;
        int panelY = height / 2 - PANEL_H / 2;

        playButton = Button.builder(Component.literal("PLAY"), b -> MusicPlayer.INSTANCE.toggle())
                .bounds(cx - 70, panelY + 128, 140, 20)
                .build();
        addRenderableWidget(playButton);

        volumeSlider = new VerticalVolumeSlider(panelX + PANEL_W - 34, panelY + 44, 18, 120,
                MusicPlayer.INSTANCE.getVolume());
        volumeSlider.setOnChange(MusicPlayer.INSTANCE::setVolume);
        addRenderableWidget(volumeSlider);
    }

    @Override
    public void tick() {
        playButton.setMessage(Component.literal(MusicPlayer.INSTANCE.isPlaying() ? "PAUSE" : "PLAY"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float tickDelta) {
        int cx = width / 2;
        int panelX = cx - PANEL_W / 2;
        int panelY = height / 2 - PANEL_H / 2;

        g.fill(0, 0, width, height, 0xB0000000);
        fillVerticalGradient(g, panelX, panelY, panelX + PANEL_W, panelY + PANEL_H,
                0xF0242430, 0xF0181820);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 2, ACCENT);
        g.fill(panelX, panelY, panelX + 2, panelY + PANEL_H, 0x33FFFFFF);

        g.centeredText(font, Component.literal("Chinese Can Fly"), cx, panelY + 12, 0xFFFFFFFF);

        long pos = MusicPlayer.INSTANCE.getPositionMs();
        long dur = MusicPlayer.INSTANCE.getDurationMs();
        float progress = dur > 0 ? (float) pos / dur : 0f;

        int barX = panelX + 24;
        int barY = panelY + 48;
        int barW = PANEL_W - 96;
        int barH = 8;
        g.fill(barX, barY, barX + barW, barY + barH, TRACK);
        int fillW = Math.round(barW * Math.min(1f, Math.max(0f, progress)));
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + barH, ACCENT);
        }
        g.fill(barX, barY, barX + barW, barY + 1, 0x66FFFFFF);

        String current = formatTime(pos);
        String total = formatTime(dur);
        g.text(font, Component.literal(current), barX, barY + barH + 6, TEXT);
        g.text(font, Component.literal(total), barX + barW - font.width(total), barY + barH + 6, TEXT);

        if (MusicPlayer.INSTANCE.hasError()) {
            g.centeredText(font, Component.literal(MusicPlayer.INSTANCE.getErrorMessage()), cx, panelY + 190, 0xFFFF6B6B);
        } else {
            String status = MusicPlayer.INSTANCE.isPlaying() ? "Playing..." : "Paused";
            g.centeredText(font, Component.literal(status), cx, panelY + 190, TEXT);
        }

        g.text(font, Component.literal("VOL"), volumeSlider.getX(), panelY + 12, TEXT);
        g.centeredText(font, Component.literal(Math.round(MusicPlayer.INSTANCE.getVolume()) + "%"),
                volumeSlider.getX() + volumeSlider.getWidth() / 2, panelY + PANEL_H - 26, ACCENT);

        super.extractRenderState(g, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String formatTime(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60);
    }

    private static void fillVerticalGradient(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int top, int bottom) {
        int steps = Math.max(1, y2 - y1);
        for (int i = 0; i < steps; i++) {
            float t = steps > 1 ? i / (float) (steps - 1) : 0f;
            g.fill(x1, y1 + i, x2, y1 + i + 1, lerpColor(top, bottom, t));
        }
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a1 = c1 >>> 24, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = c2 >>> 24, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        return ((Math.round(a1 + (a2 - a1) * t) & 0xFF) << 24)
                | ((Math.round(r1 + (r2 - r1) * t) & 0xFF) << 16)
                | ((Math.round(g1 + (g2 - g1) * t) & 0xFF) << 8)
                | (Math.round(b1 + (b2 - b1) * t) & 0xFF);
    }
}
