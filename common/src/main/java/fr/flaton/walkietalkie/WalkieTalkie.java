package fr.flaton.walkietalkie;

import dev.architectury.event.events.common.TickEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import fr.flaton.walkietalkie.block.ModBlocks;
import fr.flaton.walkietalkie.block.entity.ModBlockEntities;
import fr.flaton.walkietalkie.item.ModDataComponents;
import fr.flaton.walkietalkie.item.ModItemGroup;
import fr.flaton.walkietalkie.item.ModItems;
import fr.flaton.walkietalkie.network.ModMessages;
import fr.flaton.walkietalkie.radio.SoundManager;
import fr.flaton.walkietalkie.screen.ModScreenHandlers;

public class WalkieTalkie {

	public static void init() {
		ModBlocks.register();
		ModItems.register();
		ModDataComponents.register();
		ModItemGroup.register();

		ModBlockEntities.register();
		ModScreenHandlers.register();

		ModMessages.registerC2SPackets();

		ModSoundEvents.register();
		TickEvent.SERVER_POST.register(SoundManager::serverTick);
		EnvExecutor.runInEnv(Env.CLIENT, () -> WalkieTalkieClient::init);
	}
}