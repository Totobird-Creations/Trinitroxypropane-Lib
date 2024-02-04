package net.totobirdcreations.gaslib.world

import dev.onyxstudios.cca.api.v3.chunk.ChunkComponentFactoryRegistry
import dev.onyxstudios.cca.api.v3.chunk.ChunkComponentInitializer
import dev.onyxstudios.cca.api.v3.component.ComponentKey
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.totobirdcreations.gaslib.Mod
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import org.joml.Vector3d
import java.util.concurrent.ConcurrentHashMap


internal object GasServer : ChunkComponentInitializer {

    internal val COMPONENT : ComponentKey<GasChunk> = ComponentRegistry.getOrCreate(Mod.id("gas_chunk"), GasChunk::class.java);
    override fun registerChunkComponentFactories(registry : ChunkComponentFactoryRegistry) {
        registry.register(COMPONENT, GasChunk::class.java) { chunk -> GasChunk(null, chunk) };
    }


    private val gasWorlds : ConcurrentHashMap<Identifier, GasWorld> = ConcurrentHashMap();


    fun init() {

        // Create a new gas world on server world load.
        ServerWorldEvents.LOAD.register { _, world -> this.gasWorlds[world.dimensionKey.value] = GasWorld(world) };

        // Queue deletion of a gas world on server world unload.
        ServerWorldEvents.UNLOAD.register { _, world -> this.gasWorlds.remove(world.dimensionKey.value)?.destroy = true; };

        // Load a gas chunk on a created/existing gas world on server chunk load.
        ServerChunkEvents.CHUNK_LOAD.register { world, chunk ->
            val gasWorld = this.gasWorlds[world.dimensionKey.value] ?: return@register;
            val gasChunk = COMPONENT.get(chunk);
            gasChunk.gasWorld = gasWorld;
            gasWorld.loadChunk(gasChunk);
        };

        // Tick gas chunks in a random order on server world tick, across MAX_CHUNK_THREADS threads.
        ServerTickEvents.START_WORLD_TICK.register { world -> this.gasWorlds[world.dimensionKey.value]?.tryTickChunks() };

        // Queue deletion of a gas chunk (if it exists) on server chunk unload.
        ServerChunkEvents.CHUNK_UNLOAD.register { world, chunk -> this.gasWorlds[world.dimensionKey.value]?.unloadChunk(chunk.pos) };

    }


    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyAmount]**
     */
    fun modifyAmount(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant, amount : Double) : Boolean {
        assert(GasRegistry.isGasRegistered(gas)) { -> "Gas variant `${gas.id}` is not registered." };
        val gasWorld = this.gasWorlds[world.dimensionKey.value] ?: return false;
        return if (amount > 0.0) {
            gasWorld.addAmount(ChunkPos(pos), pos, gas, amount)
        } else if (amount < 0.0) {
            gasWorld.removeAmount(ChunkPos(pos), pos, gas, -amount)
        } else {true};
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getPressure]**
     */
    fun getPressure(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant? = null) : Double? {
        assert(gas == null || GasRegistry.isGasRegistered(gas)) { -> "Gas variant `${gas!!.id}` is not registered." };
        val gasWorld = this.gasWorlds[world.dimensionKey.value] ?: return null;
        return gasWorld.getPressure(ChunkPos(pos), pos, gas);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getAmount]**
     */
    fun getAmount(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant) : Double? {
        assert(GasRegistry.isGasRegistered(gas)) { -> "Gas variant `${gas.id}` is not registered." };
        val gasWorld = this.gasWorlds[world.dimensionKey.value] ?: return null;
        return gasWorld.getAmount(ChunkPos(pos), pos, gas);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getAmount]**
     */
    fun getMotion(world : ServerWorld, pos : BlockPos) : Vector3d? {
        val gasWorld = this.gasWorlds[world.dimensionKey.value] ?: return null;
        return gasWorld.getMotion(ChunkPos(pos), pos);
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.queuePurgeAllLoaded]**
     */
    fun queuePurgeAllLoaded(world : ServerWorld, gas : AbstractGasVariant? = null) {
        assert(gas == null || GasRegistry.isGasRegistered(gas)) { -> "Gas variant `${gas!!.id}` is not registered." };
        val gasWorld = this.gasWorlds[world.dimensionKey.value] ?: return;
        gasWorld.queuePurgeAllLoaded(gas);
    }

}