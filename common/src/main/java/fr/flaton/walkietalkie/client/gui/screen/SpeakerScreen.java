package fr.flaton.walkietalkie.client.gui.screen;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.client.gui.widget.CanalSlider;
import fr.flaton.walkietalkie.client.gui.widget.ToggleImageButton;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.network.packet.c2s.speaker.ButtonSpeakerC2SPacket;
import fr.flaton.walkietalkie.network.packet.c2s.speaker.CanalSpeakerC2SPacket;
import fr.flaton.walkietalkie.screen.SpeakerScreenHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SpeakerScreen extends AbstractContainerScreen<SpeakerScreenHandler> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/gui_walkietalkie.png");
    private static final ResourceLocation ACTIVATE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/icons/activate.png");

    private final int xSize = 195;
    private final int ySize = 76;

    private int guiLeft;
    private int guiTop;

    private ToggleImageButton activateButton;
    private CanalSlider canalSlider;
    private Button canalAddButton;
    private Button canalRemoveButton;

    public SpeakerScreen(SpeakerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawCenteredText(context, this.font, title.getString(), this.width / 2, guiTop + 7, 4210752);

        updateActivateState();
    }

    protected void drawCenteredText(GuiGraphics context, Font textRenderer, String text, int centerX, int y, int color) {
        context.drawString(textRenderer, text, centerX - textRenderer.width(text) / 2, y, color, false);
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        context.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
    }

    private void updateActivateState() {
        activateButton.setState(menu.isActivate());
        canalSlider.setCanal(menu.getCanal());

        canalAddButton.active = menu.getCanal() != ModConfig.maxCanal;
        canalRemoveButton.active = menu.getCanal() != 1;
    }

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - xSize) / 2;
        this.guiTop = (this.height - ySize) / 2;

        activateButton = this.addRenderableWidget(new ToggleImageButton(guiLeft + 8, guiTop + ySize - 8 - 20, ACTIVATE_TEXTURE, button -> sendButton(!menu.isActivate()), menu.isActivate()));

        canalSlider = this.addRenderableWidget(new SpeakerCanalSlider(this.width / 2 - 70, guiTop + 20, 140, 20, Component.empty()));

        canalAddButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> sendCanal(menu.getCanal() + 1)).bounds(this.width / 2 - 10 + 80, guiTop + 20, 20, 20).build());
        canalRemoveButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> sendCanal(menu.getCanal() - 1)).bounds(this.width / 2 - 10 - 80, guiTop + 20, 20, 20).build());
    }

    private void sendButton(boolean activate) {
        NetworkManager.sendToServer(new ButtonSpeakerC2SPacket(activate));
    }

    private void sendCanal(int canal) {
        NetworkManager.sendToServer(new CanalSpeakerC2SPacket(canal));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.getFocused() != null && this.isDragging() && button == 0) {
            return this.getFocused().mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.setDragging(false);
        if (this.canalSlider.isHoveredOrFocused()) {
            this.canalSlider.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return this.getChildAt(mouseX, mouseY).filter(element -> element.mouseReleased(mouseX, mouseY, button)).isPresent();
    }

    class SpeakerCanalSlider extends CanalSlider {

        public SpeakerCanalSlider(int x, int y, int width, int height, Component component) {
            super(x, y, width, height, component);
        }

        @Override
        protected void updateCanal(int canal) {
            sendCanal(canal);
        }
    }
}
