package fr.flaton.walkietalkie.client.gui.screen;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.client.gui.widget.CanalSlider;
import fr.flaton.walkietalkie.client.gui.widget.ToggleImageButton;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.network.ModMessages;
import fr.flaton.walkietalkie.screen.SpeakerScreenHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SpeakerScreen extends HandledScreen<SpeakerScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(Constants.MOD_ID, "textures/gui/gui_walkietalkie.png");
    private static final Identifier ACTIVATE_TEXTURE = new Identifier(Constants.MOD_ID, "textures/icons/activate.png");

    private final int xSize = 195;
    private final int ySize = 76;

    private int guiLeft;
    private int guiTop;

    private ToggleImageButton activateButton;
    private CanalSlider canalSlider;
    private ButtonWidget canalAddButton;
    private ButtonWidget canalRemoveButton;

    public SpeakerScreen(SpeakerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawCenteredText(context, this.textRenderer, title.getString(), this.width / 2, guiTop + 7, 4210752);

        updateActivateState();
    }

    protected void drawCenteredText(DrawContext context, TextRenderer textRenderer, String text, int centerX, int y, int color) {
        context.drawText(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color, false);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
    }

    private void updateActivateState() {
        activateButton.setState(handler.isActivate());
        canalSlider.setCanal(handler.getCanal());

        canalAddButton.active = handler.getCanal() != ModConfig.maxCanal;
        canalRemoveButton.active = handler.getCanal() != 1;
    }

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - xSize) / 2;
        this.guiTop = (this.height - ySize) / 2;

        activateButton = this.addDrawableChild(new ToggleImageButton(guiLeft + 8, guiTop + ySize - 8 - 20, ACTIVATE_TEXTURE, button -> sendButton(!handler.isActivate()), handler.isActivate()));

        canalSlider = this.addDrawableChild(new SpeakerCanalSlider(this.width / 2 - 70, guiTop + 20, 140, 20, Text.empty()));

        canalAddButton = this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> sendCanal(handler.getCanal() + 1)).dimensions(this.width / 2 - 10 + 80, guiTop + 20, 20, 20).build());
        canalRemoveButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> sendCanal(handler.getCanal() - 1)).dimensions(this.width / 2 - 10 - 80, guiTop + 20, 20, 20).build());
    }

    private void sendButton(boolean activate) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBoolean(activate);

        NetworkManager.sendToServer(ModMessages.BUTTON_SPEAKER_C2S, buf);
    }

    private void sendCanal(int canal) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(canal);

        NetworkManager.sendToServer(ModMessages.CANAL_SPEAKER_C2S, buf);
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
        if (this.canalSlider.isSelected()) {
            this.canalSlider.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return this.hoveredElement(mouseX, mouseY).filter(element -> element.mouseReleased(mouseX, mouseY, button)).isPresent();
    }

    class SpeakerCanalSlider extends CanalSlider {

        public SpeakerCanalSlider(int x, int y, int width, int height, Text text) {
            super(x, y, width, height, text);
        }

        @Override
        protected void updateCanal(int canal) {
            sendCanal(canal);
        }
    }
}
