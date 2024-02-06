@file: ApiStatus.Internal

package net.totobirdcreations.gaslib.world

import dev.onyxstudios.cca.api.v3.component.Component
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.chunk.Chunk
import net.totobirdcreations.gaslib.ModMain
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import net.totobirdcreations.gaslib.util.getBlockPos
import net.totobirdcreations.gaslib.util.isZero
import net.totobirdcreations.gaslib.util.putBlockPos
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3d
import java.util.concurrent.ConcurrentHashMap


internal class GasChunk(
    var gasWorld : GasWorld?,
    val chunk    : Chunk
) : Component
{

    val chunkPos : ChunkPos get() = this.chunk.pos;

    internal val gasBlocks : ConcurrentHashMap<BlockPos, GasBlock> = ConcurrentHashMap();


    fun getBlock(pos : BlockPos) : GasBlock? {
        return this.gasBlocks[pos];
    }
    fun getOrPutBlock(pos : BlockPos) : GasBlock {
        return this.gasBlocks.getOrPut(pos) { -> GasBlock(this, pos, ConcurrentHashMap()) };
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.setAmount]**
     */
    fun setAmount(pos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) {
        if (amount > 0.0) {
            this.getOrPutBlock(pos).setAmount(gas, amount, shouldSave = shouldSave);
        } else {
            this.gasBlocks.remove(pos);
            if (shouldSave) { this.chunk.setNeedsSaving(true); }
        }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyAmount]**
     */
    fun addAmount(pos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) {
        if (amount > 0.0) {
            this.getOrPutBlock(pos).addAmount(gas, amount, shouldSave = shouldSave);
        }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyAmount]**
     */
    fun removeAmount(pos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) : Boolean {
        return if (amount > 0.0) {
            val gasBlock = this.getBlock(pos) ?: return false;
            val result   = gasBlock.removeAmount(gas, amount, shouldSave = shouldSave);
            if (gasBlock.isEmpty()) { this.gasBlocks.remove(pos); }
            result
        } else { false };
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getAmount]**
     */
    fun getAmount(pos : BlockPos, gas : AbstractGasVariant) : Double? {
        return this.getBlock(pos)?.getAmount(gas);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getPressure]**
     */
    fun getPressure(pos : BlockPos, gas : AbstractGasVariant? = null) : Double? {
        val gasBlock = this.getBlock(pos) ?: return null;
        return if (gas != null) { gasBlock.getPressure(gas) } else {gasBlock.getPressure() };
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.setMotion]**
     */
    fun setMotion(pos : BlockPos, vec : Vector3d, shouldSave : Boolean = true) {
        if (vec.isZero()) {
            val gasBlock = this.getBlock(pos) ?: return;
            gasBlock.setMotion(Vector3d(), shouldSave = shouldSave);
            if (gasBlock.isEmpty()) { this.gasBlocks.remove(pos); }
        } else {
            this.getOrPutBlock(pos).setMotion(vec, shouldSave = shouldSave);
        }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyMotion]**
     */
    fun modifyMotion(pos : BlockPos, vec : Vector3d, shouldSave : Boolean = true) {
        if (vec.isZero()) {return;}
        this.getOrPutBlock(pos).modifyMotion(vec, shouldSave = shouldSave);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getMotion]**
     */
    fun getMotion(pos : BlockPos) : Vector3d? {
        return this.getBlock(pos)?.getMotion();
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.queuePurgeAllLoaded]**
     */
    fun purgeAllLoaded(gas : AbstractGasVariant? = null, shouldSave : Boolean = true) {
        if (gas == null) {
            this.gasBlocks.clear();
        } else {
            for ((pos, gasBlock) in this.gasBlocks) {
                gasBlock.purgeAllLoaded(gas);
                if (gasBlock.isEmpty()) { this.gasBlocks.remove(pos); }
            }
        }
        if (shouldSave) { this.chunk.setNeedsSaving(true); }
    }


    override fun writeToNbt(nbt : NbtCompound) {
        val chunkNbt = NbtList();
        for ((pos, block) in this.gasBlocks) {
            val blockNbt = NbtCompound();
            block.write(blockNbt);
            if (! blockNbt.isEmpty) {
                blockNbt.putBlockPos("pos", pos);
                chunkNbt.add(blockNbt);
            }
        }
        nbt.put(ModMain.ID, chunkNbt);
    }

    override fun readFromNbt(nbt : NbtCompound) {
        this.gasBlocks.clear();
        try {
            val compoundType = NbtElement.COMPOUND_TYPE.toInt();
            val chunkNbt = nbt.getList(ModMain.ID, compoundType);
            for (blockNbt in chunkNbt) {
                blockNbt as NbtCompound;
                val pos = blockNbt.getBlockPos("pos");
                if (pos != null) {
                    GasBlock.read(this, pos, blockNbt);
                }
            }
        } catch (e : Exception) {
            ModMain.LOGGER.warn("Voiding gas CHUNK ${this.chunkPos} in WORLD ${this.gasWorld?.id} failed to load:");
            ModMain.LOGGER.warn("  ${e}");
            this.gasBlocks.clear();
        }
    }

}