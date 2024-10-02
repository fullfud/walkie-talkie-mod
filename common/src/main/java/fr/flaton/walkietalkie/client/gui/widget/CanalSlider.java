package fr.flaton.walkietalkie.client.gui.widget;

import fr.flaton.walkietalkie.config.ModConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class CanalSlider extends AbstractSliderButton {

    private int prevCanal = 0;

    public CanalSlider(int x, int y, int width, int height, Component component) {
        super(x, y, width, height, component, 0);
    }

    private int getCanal() {
        return (int) Math.floor(value * (ModConfig.maxCanal - 1) + 1);
    }

    public void setCanal(int canal) {
        if (prevCanal != canal) {
            double value = 0.0;
            if (ModConfig.maxCanal != 1)
                value = (double) (canal - 1) / (ModConfig.maxCanal - 1);
            if (canal != getCanal())
                this.value = Mth.clamp(value, 0.0, 1.0);
            updateMessage();
            prevCanal = canal;
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
        updateCanal(getCanal());
    }

    protected abstract void updateCanal(int canal);

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(String.valueOf(getCanal())));
    }

    @Override
    protected void applyValue() {
    }

}
