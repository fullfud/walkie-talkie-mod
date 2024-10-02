package fr.flaton.walkietalkie.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class ToggleImageButton extends ImageButton{

    public void setState(boolean state) {
        this.state = state;
    }
    protected boolean state;

    public ToggleImageButton(int x, int y, ResourceLocation texture, PressAction onPress, boolean state) {
        super(x, y, texture, onPress);
        this.state = state;
    }

    @Override
    protected void renderImage(GuiGraphics context, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        if (state) {
            context.blit(texture, getX() + 2, getY() + 2, 16, 0, 16, 16, 32, 32);
        } else {
            context.blit(texture, getX() + 2, getY() + 2, 0, 0, 16, 16, 32, 32);
        }

    }
}
