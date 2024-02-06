@file: ApiStatus.Internal

package net.totobirdcreations.gaslib

import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import net.totobirdcreations.gaslib.common.SteamGas
import net.totobirdcreations.gaslib.common.SuperheatedGas
import net.totobirdcreations.gaslib.common.gas_burner.GasBurnerBlock
import net.totobirdcreations.gaslib.world.GasCommand
import net.totobirdcreations.gaslib.world.GasServer
import org.jetbrains.annotations.ApiStatus
import org.slf4j.LoggerFactory


internal object ModMain : ModInitializer {

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


	fun id(path : String) : Identifier {
		return Identifier(ID, path);
	}

}