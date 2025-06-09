package fr.flaton.walkietalkie;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import fr.flaton.walkietalkie.client.KeyBindings;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import fr.flaton.walkietalkie.network.ModMessages;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Random;

@Environment(EnvType.CLIENT)
public class WalkieTalkieClient implements VoicechatPlugin {

    private static final Random random = new Random();
    // Размер аудио-кадра в Simple Voice Chat (960 сэмплов = 20 миллисекунд аудио)
    private static final int FRAME_SIZE = 960;

    /**
     * Этот метод будет вызван Architectury для инициализации клиента.
     */
    public static void init() {
        // Регистрируем наш мод как клиентский плагин для Simple Voice Chat
        // Эта строка должна быть одной из первых, чтобы плагин успел загрузиться
        VoicechatApi.registerPlugin(new WalkieTalkieClient());

        // Твой оригинальный код инициализации
        ModMessages.registerS2CPackets();
        KeyBindings.register();

        ItemPropertiesRegistry.registerGeneric(new Identifier(Constants.MOD_ID, "activate"), ((stack, world, entity, seed) -> {
            if (!stack.hasNbt()) {
                return 0;
            }
            return stack.getNbt().getBoolean(WalkieTalkieItem.NBT_KEY_ACTIVATE) ? 1.0f : 0.0f;
        }));
    }

    // --- МЕТОДЫ ИЗ ИНТЕРФЕЙСА VOICECHATPLUGIN ---

    @Override
    public String getPluginId() {
        return Constants.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        // Мы зарегистрировались. Теперь мы можем подписаться на события.
        // Регистрируем наш перехватчик звука.
        api.registerClientSoundEvent(this::onClientSound);
    }

    // --- НАША НОВАЯ ЛОГИКА ---

    /**
     * Этот метод будет вызываться для каждого аудио-кадра с микрофона.
     * Здесь мы будем подменять тишину на фоновый шум.
     */
    private void onClientSound(ClientSoundEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        // Убеждаемся, что игрок существует
        if (client.player == null) {
            return;
        }

        // Проверяем, держит ли игрок в руках активную и не выключенную рацию
        ItemStack heldStack = Util.getWalkieTalkieInHand(client.player);
        if (heldStack == null || !heldStack.hasNbt() || !heldStack.getNbt().getBoolean(WalkieTalkieItem.NBT_KEY_ACTIVATE) || heldStack.getNbt().getBoolean(WalkieTalkieItem.NBT_KEY_MUTE)) {
            // Рация не активна, поэтому мы никак не вмешиваемся в звук
            return;
        }

        // Если мы дошли до сюда, значит рация активна.

        // Проверяем, молчит ли игрок (микрофон не передает звук)
        if (event.getRawAudio() == null) {
            // Игрок молчит. Вместо тишины отправляем на сервер пакет с шумом.
            event.setRawAudio(generateRadioStatic());
        }
        
        // Если игрок говорит (event.getRawAudio() не равен null), мы ничего не делаем.
        // Его чистый голос отправится на сервер, где наш серверный плагин (WalkieTalkieVoiceChatPlugin)
        // применит к нему все крутые эффекты, которые мы написали.
    }

    /**
     * Генерирует короткий кусок тихого радио-шума, чтобы имитировать "открытый канал".
     * @return массив short со статическими помехами.
     */
    private short[] generateRadioStatic() {
        short[] staticNoise = new short[FRAME_SIZE];
        for (int i = 0; i < FRAME_SIZE; i++) {
            // Генерируем очень тихий шум, чтобы он не был навязчивым
            // Амплитуда 300 - это примерно 1% от максимальной громкости
            staticNoise[i] = (short) ((random.nextFloat() * 2F - 1F) * 300F);
        }
        return staticNoise;
    }
}