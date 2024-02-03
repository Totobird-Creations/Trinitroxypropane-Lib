package net.totobirdcreations.gaslib.world

import dev.onyxstudios.cca.api.v3.component.Component
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.chunk.Chunk
import net.totobirdcreations.gaslib.Mod
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import net.totobirdcreations.gaslib.api.GasAPI
import org.joml.Vector3d
import java.util.concurrent.ConcurrentHashMap


internal class GasChunk(
    var gasWorld : GasWorld?,
    val chunk    : Chunk
) : Component {

    val chunkPos : ChunkPos get() = this.chunk.pos;

    internal val gasBlocks : ConcurrentHashMap<BlockPos, GasBlock> = ConcurrentHashMap();


    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyAmount]**
     */
    fun addAmount(pos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) : Boolean {
        val gasBlock = this.gasBlocks[pos];
        if (gasBlock != null) {
            if (gasBlock.gases.containsKey(gas)) {
                gasBlock.gases[gas] = gasBlock.gases[gas]!! + amount;
            } else {
                gasBlock.gases[gas] = amount;
            }
        } else {
            val map = ConcurrentHashMap<AbstractGasVariant, Double>();
            map[gas] = amount;
            this.gasBlocks[pos] = GasBlock(map);
        }
        if (shouldSave) { this.chunk.setNeedsSaving(true); }
        return true;
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyAmount]**
     */
    fun removeAmount(pos : BlockPos, gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) : Boolean {
        val gasBlock   = this.gasBlocks[pos]  ?: return false;
        val prevAmount = gasBlock.gases[gas] ?: return false;
        if (amount >= prevAmount) {
            gasBlock.gases.remove(gas);
        } else {
            gasBlock.gases[gas] = prevAmount - amount;
        }
        if (shouldSave) { this.chunk.setNeedsSaving(true); }
        return true;
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getPressure]**
     */
    fun getPressure(pos : BlockPos) : Double? {
        val world = this.gasWorld?.world ?: return null;
        return this.gasBlocks[pos]?.gases?.entries?.sumOf { (gas, amount) -> amount * gas.volumePerAmount(world, pos) } ?: 0.0;
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getPressure]**
     */
    fun getPressure(pos : BlockPos, gas : AbstractGasVariant) : Double? {
        val world = this.gasWorld?.world ?: return null;
        return this.getAmount(pos, gas) * gas.volumePerAmount(world, pos);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getAmount]**
     */
    fun getAmount(pos : BlockPos, gas : AbstractGasVariant) : Double {
        return this.gasBlocks[pos]?.gases?.get(gas) ?: 0.0;
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getMotion]**
     */
    fun getMotion(pos : BlockPos) : Vector3d {
        return this.gasBlocks[pos]?.motion ?: Vector3d();
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.queuePurgeAllLoaded]**
     */
    fun purgeAllLoaded(gas : AbstractGasVariant? = null) {
        if (gas == null) {
            this.gasBlocks.clear();
        } else {
            for ((_, gasBlock) in this.gasBlocks) {
                gasBlock.gases.remove(gas);
            }
        }
        this.chunk.setNeedsSaving(true);
    }


    override fun writeToNbt(nbt : NbtCompound) {
        val chunkNbt = NbtList();
        for ((pos, block) in this.gasBlocks) {
            if (block.gases.isNotEmpty()) {
                var save = false;
                val gasesNbt = NbtCompound();
                for ((id, amount) in block.gases) {
                    if (amount > 0.0) {
                        save = true;
                        gasesNbt.putDouble(id.id.toString(), amount);
                    }
                }
                if (save) {
                    val posNbt = NbtCompound();
                    posNbt.putInt("x", pos.x);
                    posNbt.putInt("y", pos.y);
                    posNbt.putInt("z", pos.z);
                    posNbt.putInt("vx", pos.x);
                    posNbt.putInt("vy", pos.y);
                    posNbt.putInt("vz", pos.z);
                    posNbt.put("gases", gasesNbt);
                    chunkNbt.add(posNbt);
                }
            }
        }
        nbt.put("${Mod.ID}_gases", chunkNbt);
    }

    override fun readFromNbt(nbt : NbtCompound) {
        this.gasBlocks.clear();
        try {
            val compoundType = NbtElement.COMPOUND_TYPE.toInt();
            val intType      = NbtElement.INT_TYPE.toInt();
            val doubleType   = NbtElement.DOUBLE_TYPE.toInt();
            val chunkNbt = nbt.getList("${Mod.ID}_gases", compoundType);
            for (posNbt in chunkNbt) {
                posNbt as NbtCompound;
                if (   posNbt.contains("x", intType)
                    && posNbt.contains("y", intType)
                    && posNbt.contains("z", intType)
                    && posNbt.contains("vx", doubleType)
                    && posNbt.contains("vy", doubleType)
                    && posNbt.contains("vz", doubleType)
                    && posNbt.contains("gases", compoundType)
                ) {
                    val pos      = BlockPos(posNbt.getInt("x"), posNbt.getInt("y"), posNbt.getInt("z"));
                    val gasesNbt = posNbt.getCompound("gases");
                    val gases    = ConcurrentHashMap<AbstractGasVariant, Double>();
                    for (idStr in gasesNbt.keys) {
                        try {
                            val id  = Identifier(idStr);
                            val gas = GasAPI.getRegisteredGas(id);
                            if (gas != null) {
                                if (gasesNbt.contains(idStr, doubleType)) {
                                    gases[gas] = gasesNbt.getDouble(idStr);
                                } else {
                                    Mod.LOGGER.warn("Voiding invalid gas VARIANT ${id} at BLOCK ${pos} in CHUNK ${this.chunkPos} in WORLD ${this.gasWorld?.id} containing invalid data.");
                                }
                            } else {
                                Mod.LOGGER.warn("Voiding unknown gas VARIANT ${id} at BLOCK ${pos} in CHUNK ${this.chunkPos} in WORLD ${this.gasWorld?.id}. Registry missing?");
                            }
                        } catch (_ : Exception) {
                            Mod.LOGGER.warn("Voiding invalid gas VARIANT `${idStr.replace("\\", "\\\\").replace("`", "\\`")}` at BLOCK ${pos} in CHUNK ${this.chunkPos} in WORLD ${this.gasWorld?.id} containing invalid data.");
                        }
                    }
                    if (gases.isNotEmpty()) {
                        this.gasBlocks[pos] = GasBlock(gases, motion = Vector3d(
                            posNbt.getDouble("vx"),
                            posNbt.getDouble("vy"),
                            posNbt.getDouble("vz")
                        )
                        );
                    }
                }
            }
        } catch (e : Exception) {
            Mod.LOGGER.warn("Voiding gas CHUNK ${this.chunkPos} in WORLD ${this.gasWorld?.id} failed to load:");
            Mod.LOGGER.warn("  ${e}");
            this.gasBlocks.clear();
        }
    }

}