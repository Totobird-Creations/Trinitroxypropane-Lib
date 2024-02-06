package net.totobirdcreations.gaslib

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.totobirdcreations.gaslib.world.GasParticles
import org.slf4j.LoggerFactory


internal object ModClient : ClientModInitializer {

    @Environment(EnvType.CLIENT)
    internal val LOGGER_CLIENT = LoggerFactory.getLogger("${ModMain.ID}_client");

    @Environment(EnvType.CLIENT)
    override fun onInitializeClient() {
        ClientChunkEvents.CHUNK_LOAD   .register { _, chunk -> GasParticles.loadChunk   (chunk); };
        ClientChunkEvents.CHUNK_UNLOAD .register { _, chunk -> GasParticles.unloadChunk (chunk); };
        ClientTickEvents.END_WORLD_TICK.register { world -> GasParticles.tickClient(world); };
        ClientPlayNetworking.registerGlobalReceiver(GasParticles.SPAWN_CHANNEL) { _, _, packet, _ -> GasParticles.clientReceiveUpdate(packet) };
        LOGGER_CLIENT.info("READY.");
    }

}