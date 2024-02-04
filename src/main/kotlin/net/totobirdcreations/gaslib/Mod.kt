package net.totobirdcreations.gaslib

import net.fabricmc.api.ModInitializer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import net.totobirdcreations.gaslib.common.SteamGas
import net.totobirdcreations.gaslib.common.SuperheatedGas
import net.totobirdcreations.gaslib.common.gas_burner.GasBurnerBlock
import net.totobirdcreations.gaslib.world.GasCommand
import net.totobirdcreations.gaslib.world.GasServer
import net.totobirdcreations.gaslib.world.GasParticles
import org.slf4j.LoggerFactory


internal object Mod : ModInitializer, ClientModInitializer {

	const val    ID     = "gaslib";
    internal val LOGGER = LoggerFactory.getLogger(ID);

	override fun onInitialize() {

		GasServer.init();
		GasCommand.register();

		LOGGER.info("READY.");

		if (FabricLoader.getInstance().isDevelopmentEnvironment) {
			GasBurnerBlock .enable();
			SteamGas       .enable();
			SuperheatedGas .enable();
		}
	}


	@Environment(EnvType.CLIENT)
	internal val LOGGER_CLIENT = LoggerFactory.getLogger(ID);

	@Environment(EnvType.CLIENT)
	override fun onInitializeClient() {
		ClientChunkEvents.CHUNK_LOAD   .register { _, chunk -> GasParticles.loadChunk   (chunk); };
		ClientChunkEvents.CHUNK_UNLOAD .register { _, chunk -> GasParticles.unloadChunk (chunk); };
		ClientTickEvents.END_WORLD_TICK.register { world -> GasParticles.tickClient(world); };
		ClientPlayNetworking.registerGlobalReceiver(GasParticles.SPAWN_CHANNEL) { _, _, packet, _ -> GasParticles.clientReceiveUpdate(packet) };
		LOGGER_CLIENT.info("READY.");
	}


	fun id(path : String) : Identifier {
		return Identifier(ID, path);
	}

}