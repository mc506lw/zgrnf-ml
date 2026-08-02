package mc506lw.zgrnf;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * A vertical volume slider. Drawn entirely by hand via the extractor so it can
 * sit on the right edge of the screen like a proper audio volume fader.
 */
public final class VerticalVolumeSlider extends AbstractWidget {

    private static final int ACCENT = 0xFF4DA6FF;
    private static final int TRACK = 0x66FFFFFF;
    private static final int KNOB = 0xFFE6F4FF;

    private double value; // 0..1
    private boolean dragging;
    private Consumer<Float> onChange = ignored -> {
    };

    public VerticalVolumeSlider(int x, int y, int width, int height, float volumePercent) {
        super(x, y, width, height, Component.literal("Volume"));
        this.value = clamp(volumePercent / 100f);
    }

    public void setOnChange(Consumer<Float> onChange) {
        this.onChange = onChange;
    }

    public float getVolumePercent() {
        return Math.round(value * 100f);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float tickDelta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int trackW = 4;
        int trackX = x + w / 2 - trackW / 2;

        g.fill(trackX, y, trackX + trackW, y + h, TRACK);
        int filledH = (int) Math.round(h * value);
        if (filledH > 0) {
            g.fill(trackX, y + h - filledH, trackX + trackW, y + h, ACCENT);
        }
        g.fill(trackX, y, trackX + trackW, y + 1, 0x66FFFFFF);

        int knobH = 6;
        int knobY = y + (int) ((1f - value) * (h - knobH));
        g.fill(x, knobY, x + w, knobY + knobH, KNOB);
        if (isHoveredOrFocused()) {
            g.fill(x, knobY, x + w, knobY + 1, ACCENT);
        }
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
        if (!active || !isMouseOver(event.x(), event.y())) {
            return false;
        }
        dragging = true;
        setValueFromY(event.y());
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) {
            return false;
        }
        setValueFromY(event.y());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return false;
    }

    private void setValueFromY(double mouseY) {
        double v = 1.0 - (mouseY - getY()) / getHeight();
        value = clamp(v);
        onChange.accept(getVolumePercent());
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
