@file: ApiStatus.Internal

package net.totobirdcreations.gaslib.world

import com.electronwill.nightconfig.core.conversion.InvalidValueException
import dev.onyxstudios.cca.api.v3.component.Component
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Direction
import net.minecraft.world.chunk.Chunk
import net.totobirdcreations.gaslib.ModMain
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import net.totobirdcreations.gaslib.util.getBlockPos
import net.totobirdcreations.gaslib.util.putBlockPos
import org.jetbrains.annotations.ApiStatus


internal class GasChunk(
    var gasWorld : GasWorld?,
    val chunk    : Chunk
) : Component
{

    val chunkPos : ChunkPos get() = this.chunk.pos;

    private val gasBlocks : List<List<List<GasBlock>>> = buildList {
        for (x in 0..<16) {
            this.add(buildList {
                for (y in this@GasChunk.chunk.heightLimitView.bottomY..<this@GasChunk.chunk.heightLimitView.topY) {
                    this.add(buildList {
                        for (z in 0..<16) {
                            val blockPos = this@GasChunk.chunkPos.getBlockPos(x, y, z);
                            this.add(GasBlock(this@GasChunk, blockPos));
                        }
                    })
                }
            });
        }
    };


    fun update() {
        for (gasBlock in this.iterator().shuffled()) {
            gasBlock.update();
        }
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.setAmount]**
     */
    fun setAmount(blockPos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) {
        if (amount > 0.0) {
            this[blockPos].setAmount(gas, amount, shouldSave = shouldSave);
        } else {
            throw InvalidValueException("GasChunk.setAmount amount must be > 0.0");
        }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.addAmount]**
     */
    fun increaseAmount(blockPos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) {
        if (amount > 0.0) {
            this[blockPos].increaseAmount(gas, amount, shouldSave = shouldSave);
        } else {
            throw InvalidValueException("GasChunk.setAmount amount must be > 0.0");
        }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.addAmount]**
     */
    fun decreaseAmount(blockPos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) {
        if (amount > 0.0) {
            this[blockPos].decreaseAmount(gas, amount, shouldSave = shouldSave);
        } else {
            throw InvalidValueException("GasChunk.setAmount amount must be > 0.0");
        };
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getAmount]**
     */
    fun getAmount(blockPos : BlockPos, gas : AbstractGasVariant) : Double {
        return this[blockPos].getAmount(gas);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getPressure]**
     */
    fun getPressure(blockPos : BlockPos, gas : AbstractGasVariant? = null) : Double? {
        val gasBlock = this[blockPos];
        return gasBlock.getPressure(gas);
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.setMotion]**
     */
    fun setMotion(blockPos : BlockPos, dir : Direction, amount : Double, shouldSave : Boolean = true) : Boolean {
        return this[blockPos].setMotion(dir, amount, shouldSave = shouldSave);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.addMotion]**
     */
    fun addMotion(blockPos : BlockPos, dir : Direction, amount : Double, shouldSave : Boolean = true) : Boolean {
        return this[blockPos].addMotion(dir, amount, shouldSave = shouldSave);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getMotion]**
     */
    fun getMotion(blockPos : BlockPos, dir : Direction) : Double? {
        return this[blockPos].getMotion(dir);
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.queuePurgeAllLoaded]**
     */
    fun purgeAllLoaded(gas : AbstractGasVariant? = null, shouldSave : Boolean = true) {
        if (gas == null) {
            this.clear();
        } else {
            for (gasBlock in this.iterator()) {
                gasBlock.purgeAllLoaded(gas);
            }
        }
        if (shouldSave) { this.chunk.setNeedsSaving(true); }
    }


    override fun writeToNbt(nbt : NbtCompound) {
        val chunkNbt = NbtList();
        for (gasBlock in this.iterator()) {
            val blockNbt = NbtCompound();
            gasBlock.write(blockNbt);
            if (! blockNbt.isEmpty) {
                blockNbt.putBlockPos("pos", gasBlock.blockPos);
                chunkNbt.add(blockNbt);
            }
        }
        nbt.put(ModMain.ID, chunkNbt);
    }

    override fun readFromNbt(nbt : NbtCompound) {
        this.clear();
        try {
            val compoundType = NbtElement.COMPOUND_TYPE.toInt();
            val chunkNbt = nbt.getList(ModMain.ID, compoundType);
            for (blockNbt in chunkNbt) {
                blockNbt as NbtCompound;
                val pos = blockNbt.getBlockPos("pos");
                if (pos != null) {
                    this[pos].read(blockNbt);
                }
            }
        } catch (e : Exception) {
            ModMain.LOGGER.warn("Voiding gas CHUNK ${this.chunkPos} in WORLD ${this.gasWorld?.id} failed to load:");
            ModMain.LOGGER.warn("  ${e}");
            this.clear();
        }
    }


    private fun clear() {
        for (gasBlock in this.iterator()) {
            gasBlock.clear();
        }
    }


    operator fun get(pos : BlockPos) : GasBlock {
        return this.gasBlocks[pos.x][pos.y][pos.z];
    }
    operator fun iterator() : List<GasBlock> {
        return this.gasBlocks.flatten().flatten();
    }

}
