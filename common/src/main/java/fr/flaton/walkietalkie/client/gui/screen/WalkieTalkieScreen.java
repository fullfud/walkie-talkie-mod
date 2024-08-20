package fr.flaton.walkietalkie.client.gui.screen;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.client.gui.widget.CanalSlider;
import fr.flaton.walkietalkie.client.gui.widget.ToggleImageButton;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import fr.flaton.walkietalkie.network.ModMessages;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class WalkieTalkieScreen extends Screen {

    private static WalkieTalkieScreen instance;

    private final int xSize = 195;
    private final int ySize = 76;

    private int guiLeft;
    private int guiTop;

    private final ItemStack stack;
    private boolean mute;
    private boolean activate;
    private int canal;

    private ToggleImageButton muteButton;
    private ToggleImageButton activateButton;

    private CanalSlider canalSlider;
    private ButtonWidget canalAddButton;
    private ButtonWidget canalRemoveButton;

    private static final Identifier BG_TEXTURE = new Identifier(Constants.MOD_ID, "textures/gui/gui_walkietalkie.png");
    private static final Identifier MUTE_TEXTURE = new Identifier("voicechat", "textures/icons/microphone_button.png");
    private static final Identifier ACTIVATE_TEXTURE = new Identifier(Constants.MOD_ID, "textures/icons/activate.png");

    public WalkieTalkieScreen(ItemStack stack) {
        super(Text.translatable("gui.walkietalkie.title"));
        instance = this;
        this.stack = stack;

        mute = WalkieTalkieItem.isMute(stack);
        activate = WalkieTalkieItem.isActivate(stack);
        canal = WalkieTalkieItem.getCanal(stack);

        MinecraftClient.getInstance().setScreen(this);
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - xSize) / 2;
        this.guiTop = (this.height - ySize) / 2;

        muteButton = new ToggleImageButton(guiLeft + 8, guiTop + ySize - 8 - 20, MUTE_TEXTURE, button -> sendButton(1, !mute), mute);
        this.addDrawableChild(muteButton);

        activateButton = new ToggleImageButton(guiLeft + 30, guiTop + ySize - 28, ACTIVATE_TEXTURE, button -> sendButton(0, !activate), activate);
        this.addDrawableChild(activateButton);

        canalSlider = this.addDrawableChild(new WTCanalSlider(this.width / 2 - 70, guiTop + 20, 140, 20, Text.empty()));

        canalAddButton = this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> sendCanal(canal + 1)).dimensions(this.width / 2 - 10 + 80, guiTop + 20, 20, 20).build());
        canalRemoveButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> sendCanal(canal - 1)).dimensions(this.width / 2 - 10 - 80, guiTop + 20, 20, 20).build());

    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.drawTexture(BG_TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawCenteredText(context, this.textRenderer, this.title, this.width / 2, guiTop + 7, 4210752);
    }

    protected void drawCenteredText(DrawContext context, TextRenderer textRenderer, Text text, int centerX, int y, int color) {
        context.drawText(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color, false);
    }

    private void sendButton(int index, boolean activate) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(index);
        buf.writeBoolean(activate);

        NetworkManager.sendToServer(ModMessages.BUTTON_WALKIETALKIE_C2S, buf);
    }

    private void sendCanal(int canal) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(canal);

        NetworkManager.sendToServer(ModMessages.CANAL_WALKIETALKIE_C2S, buf);
    }

    public void updateButtons(ItemStack stack) {
        mute = WalkieTalkieItem.isMute(stack);
        activate = WalkieTalkieItem.isActivate(stack);
        canal = WalkieTalkieItem.getCanal(stack);

        muteButton.setState(WalkieTalkieItem.isMute(stack));
        activateButton.setState(WalkieTalkieItem.isActivate(stack));
        canalSlider.setCanal((WalkieTalkieItem.getCanal(stack)));

        canalAddButton.active = WalkieTalkieItem.getCanal(stack) != ModConfig.maxCanal;
        canalRemoveButton.active = WalkieTalkieItem.getCanal(stack) != 1;
    }

    public static WalkieTalkieScreen getInstance() {
        return instance;
    }

    class WTCanalSlider extends CanalSlider {

        public WTCanalSlider(int x, int y, int width, int height, Text text) {
            super(x, y, width, height, text);
            sendCanal(canal);
        }

        @Override
        protected void updateCanal(int canal) {
            sendCanal(canal);
        }
    }

}
