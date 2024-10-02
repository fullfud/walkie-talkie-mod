package fr.flaton.walkietalkie.client.gui.screen;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.client.gui.widget.CanalSlider;
import fr.flaton.walkietalkie.client.gui.widget.ToggleImageButton;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import fr.flaton.walkietalkie.network.packet.c2s.walkietalkie.ButtonWalkieTalkieC2SPacket;
import fr.flaton.walkietalkie.network.packet.c2s.walkietalkie.CanalWalkieTalkieC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

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
    private Button canalAddButton;
    private Button canalRemoveButton;

    private static final ResourceLocation BG_TEXTURE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/gui_walkietalkie.png");
    private static final ResourceLocation MUTE_TEXTURE = ResourceLocation.fromNamespaceAndPath("voicechat", "textures/icons/microphone_button.png");
    private static final ResourceLocation ACTIVATE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/icons/activate.png");

    public WalkieTalkieScreen(ItemStack stack) {
        super(Component.translatable("gui.walkietalkie.title"));
        instance = this;
        this.stack = stack;

        mute = WalkieTalkieItem.isMute(stack);
        activate = WalkieTalkieItem.isActivate(stack);
        canal = WalkieTalkieItem.getCanal(stack);

        Minecraft.getInstance().setScreen(this);
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - xSize) / 2;
        this.guiTop = (this.height - ySize) / 2;

        muteButton = new ToggleImageButton(guiLeft + 8, guiTop + ySize - 8 - 20, MUTE_TEXTURE, button -> sendButton(1, !mute), mute);
        this.addRenderableWidget(muteButton);

        activateButton = new ToggleImageButton(guiLeft + 30, guiTop + ySize - 28, ACTIVATE_TEXTURE, button -> sendButton(0, !activate), activate);
        this.addRenderableWidget(activateButton);

        canalSlider = this.addRenderableWidget(new WTCanalSlider(this.width / 2 - 70, guiTop + 20, 140, 20, Component.empty()));

        canalAddButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> sendCanal(canal + 1)).bounds(this.width / 2 - 10 + 80, guiTop + 20, 20, 20).build());
        canalRemoveButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> sendCanal(canal - 1)).bounds(this.width / 2 - 10 - 80, guiTop + 20, 20, 20).build());

    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.blit(BG_TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawCenteredText(context, this.font, this.title, this.width / 2, guiTop + 7, 4210752);
    }

    protected void drawCenteredText(GuiGraphics context, Font font, Component component, int centerX, int y, int color) {
        context.drawString(font, component, centerX - font.width(component) / 2, y, color, false);
    }

    private void sendButton(int index, boolean activate) {
        NetworkManager.sendToServer(new ButtonWalkieTalkieC2SPacket(index, activate));
    }

    private void sendCanal(int canal) {
        NetworkManager.sendToServer(new CanalWalkieTalkieC2SPacket(canal));
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

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.setDragging(false);
        if (this.canalSlider.isHoveredOrFocused()) {
            this.canalSlider.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return this.getChildAt(mouseX, mouseY).filter(element -> element.mouseReleased(mouseX, mouseY, button)).isPresent();
    }

    class WTCanalSlider extends CanalSlider {

        public WTCanalSlider(int x, int y, int width, int height, Component component) {
            super(x, y, width, height, component);
            sendCanal(canal);
        }

        @Override
        protected void updateCanal(int canal) {
            sendCanal(canal);
        }
    }

}
