@file: ApiStatus.Internal

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
import net.minecraft.util.math.Direction
import net.totobirdcreations.gaslib.ModMain
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import org.jetbrains.annotations.ApiStatus
import java.util.concurrent.ConcurrentHashMap


internal object GasServer : ChunkComponentInitializer {

    private val COMPONENT : ComponentKey<GasChunk> = ComponentRegistry.getOrCreate(ModMain.id("gas_chunk"), GasChunk::class.java);
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
     * **See [net.totobirdcreations.gaslib.api.GasAPI.setAmount]**
     */
    fun setAmount(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) : Boolean {
        assert(GasRegistry.isGasRegistered(gas)) { -> "Gas variant `${gas.id}` is not registered." };
        assert(amount.isFinite() && amount >= 0.0) { -> "Gas amount must be a finite, positive number." };
        val gasWorld = this.gasWorlds[world.dimensionKey.value] ?: return false;
        return gasWorld.setAmount(ChunkPos(pos), pos, gas, amount, shouldSave = shouldSave);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.addAmount]**
     */
    fun addAmount(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) : Boolean {
        assert(GasRegistry.isGasRegistered(gas)) { -> "Gas variant `${gas.id}` is not registered." };
        val gasWorld = this.gasWorlds[world.dimensionKey.value] ?: return false;
        return if (amount > 0.0) {
            gasWorld.increaseAmount(ChunkPos(pos), pos, gas, amount, shouldSave = shouldSave)
        } else if (amount < 0.0) {
            gasWorld.decreaseAmount(ChunkPos(pos), pos, gas, -amount, shouldSave = shouldSave)
        } else {true};
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getAmount]**
     */
    fun getAmount(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant) : Double? {
        assert(GasRegistry.isGasRegistered(gas)) { -> "Gas variant `${gas.id}` is not registered." };
        return this.gasWorlds[world.dimensionKey.value]?.getAmount(ChunkPos(pos), pos, gas);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getPressure]**
     */
    fun getPressure(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant? = null) : Double? {
        assert(gas == null || GasRegistry.isGasRegistered(gas)) { -> "Gas variant `${gas!!.id}` is not registered." };
        return this.gasWorlds[world.dimensionKey.value]?.getPressure(ChunkPos(pos), pos, gas);
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.setMotion]**
     */
    fun setMotion(world : ServerWorld, pos : BlockPos, dir : Direction, amount : Double, shouldSave : Boolean = true) : Boolean {
        val gasWorld = this.gasWorlds[world.dimensionKey.value] ?: return false;
        return gasWorld.setMotion(ChunkPos(pos), pos, dir, amount, shouldSave = shouldSave);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.addMotion]**
     */
    fun addMotion(world : ServerWorld, pos : BlockPos, dir : Direction, amount : Double, shouldSave : Boolean = true) : Boolean {
        val gasWorld = this.gasWorlds[world.dimensionKey.value] ?: return false;
        return gasWorld.addMotion(ChunkPos(pos), pos, dir, amount, shouldSave = shouldSave);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getAmount]**
     */
    fun getMotion(world : ServerWorld, pos : BlockPos, dir : Direction) : Double? {
        return this.gasWorlds[world.dimensionKey.value]?.getMotion(ChunkPos(pos), pos, dir);
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