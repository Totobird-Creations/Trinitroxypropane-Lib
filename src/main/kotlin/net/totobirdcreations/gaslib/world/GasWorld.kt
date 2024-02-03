package net.totobirdcreations.gaslib.world

import kotlinx.atomicfu.AtomicInt
import kotlinx.coroutines.sync.Mutex
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.totobirdcreations.gaslib.Mod
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


private const val THREADS_PER_WORLD : UInt = 16u;


internal class GasWorld(
    var world : ServerWorld
) {
    internal var destroy : Boolean = false;

    private var ticksBehind : UInt  = 0u;
    private var warningTime : UByte = 0u;

    private var gasChunks   : MutableMap<ChunkPos, GasChunk>   = mutableMapOf();
    private var purgeQueued : MutableList<AbstractGasVariant?> = mutableListOf();

    val id : Identifier get() = this.world.dimensionKey.value;


    // Everything that makes this thread safe.
    private val pendingTicks     : MutableList<ChunkPos> = Collections.synchronizedList(mutableListOf());
    private val pendingTickCount : AtomicInteger         = AtomicInteger();
    @Suppress("unused")
    private val threads : List<Thread> = buildList { ->
        for (i in 0u..<THREADS_PER_WORLD) {
            val thread = Thread { -> ticker()};
            thread.start();
            this.add(thread);
        }
    };


    fun loadChunk(gasChunk : GasChunk) {
        this.gasChunks[gasChunk.chunkPos] = gasChunk;
    }
    fun unloadChunk(chunkPos : ChunkPos) {
        this.gasChunks.remove(chunkPos);
    }

    fun tryTickChunks() {
        if (this.pendingTickCount.get() <= 0) {
            this.ticksBehind = UInt.MIN_VALUE;

            // If a purge has been queued, purge.
            for (gas in this.purgeQueued) {
                for ((_, gasChunk) in this.gasChunks) {
                    gasChunk.purgeAllLoaded(gas);
                }
                Mod.LOGGER.info("Gas WORLD ${this.id} has been purged.");
            }
            this.purgeQueued.clear();

            // Add new updates to queue
            synchronized(this.pendingTicks) { ->
                val count = this.gasChunks.size;
                if (count > 0) {
                    this.pendingTickCount.addAndGet(count);
                    this.pendingTicks.addAll(this.gasChunks.keys.shuffled());
                }
            }

        } else {
            if (this.ticksBehind < UInt.MAX_VALUE) {
                this.ticksBehind++;
            }
            if (this.ticksBehind >= 5u) {
                if (this.warningTime > 0u) {
                    this.warningTime--;
                } else {
                    Mod.LOGGER.warn("Gas WORLD ${this.id} has fallen ${this.ticksBehind} ticks behind.");
                    this.warningTime = 20u;
                }
            }
        }
    }

    private fun ticker() { while (true) {
        if (this.destroy) {return;}

        val chunkPos = synchronized(this.pendingTicks) { ->
            if (this.pendingTicks.size > 0) {
                this.pendingTicks.removeAt(0)
            } else {null}
        } ?: continue;

        try {
            this.gasChunks[chunkPos]?.tick(this);
        } catch (_ : Exception) {}
        if (this.pendingTickCount.decrementAndGet() <= 0) {
        }
    } };

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyAmount]**
     */
    fun addAmount(chunkPos : ChunkPos, pos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) : Boolean {
        val gasChunk = this.gasChunks[chunkPos] ?: return false;
        return gasChunk.addAmount(pos, gas, amount, shouldSave = shouldSave);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyAmount]**
     */
    fun removeAmount(chunkPos : ChunkPos, pos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) : Boolean {
        val gasChunk = this.gasChunks[chunkPos] ?: return false;
        return gasChunk.removeAmount(pos, gas, amount, shouldSave = shouldSave);
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getPressure]**
     */
    fun getPressure(chunkPos : ChunkPos, pos : BlockPos, gas : AbstractGasVariant? = null) : Double? {
        val gasChunk = this.gasChunks[chunkPos] ?: return null;
        return if (gas != null) { gasChunk.getPressure(pos, gas) } else { gasChunk.getPressure(pos) }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getAmount]**
     */
    fun getAmount(chunkPos : ChunkPos, pos : BlockPos, gas : AbstractGasVariant) : Double? {
        val gasChunk = this.gasChunks[chunkPos] ?: return null;
        return gasChunk.getAmount(pos, gas);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getMotion]**
     */
    fun getMotion(chunkPos : ChunkPos, pos : BlockPos) : Vector3d? {
        val gasChunk = this.gasChunks[chunkPos] ?: return null;
        return gasChunk.getMotion(pos);
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.queuePurgeAllLoaded]**
     */
    fun queuePurgeAllLoaded(gas : AbstractGasVariant? = null) {
        this.purgeQueued.add(gas);
    }

}