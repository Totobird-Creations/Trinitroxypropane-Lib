package net.totobirdcreations.gaslib

import net.fabricmc.api.ModInitializer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import net.totobirdcreations.gaslib.command.GasCommand
import net.totobirdcreations.gaslib.example.ExampleMod
import net.totobirdcreations.gaslib.world.GasServer
import org.slf4j.LoggerFactory


internal object Mod : ModInitializer, ClientModInitializer {

	const val    ID     = "gaslib";
    internal val LOGGER = LoggerFactory.getLogger(ID);

	override fun onInitialize() {

		GasServer.init();
		GasCommand.register();

		LOGGER.info("READY.");

		if (FabricLoader.getInstance().isDevelopmentEnvironment) {
			ExampleMod.enable();
		}
	}


	@Environment(EnvType.CLIENT)
	private val LOGGER_CLIENT = LoggerFactory.getLogger(ID);

	@Environment(EnvType.CLIENT)
	override fun onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(Network.PARTICLE_CHANNEL) { client, _, packet, _ ->
			Network.clientReceiveParticle(client, packet);
		}
		LOGGER_CLIENT.info("READY.");
	}


	fun id(path : String) : Identifier {
		return Identifier(ID, path);
	}

}