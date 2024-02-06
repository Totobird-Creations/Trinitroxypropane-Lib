@file: ApiStatus.Internal

package net.totobirdcreations.gaslib.world

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.totobirdcreations.gaslib.ModMain
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import net.totobirdcreations.gaslib.util.getVector3d
import net.totobirdcreations.gaslib.util.isZero
import net.totobirdcreations.gaslib.util.putVector3d
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3d
import java.util.concurrent.ConcurrentHashMap


internal data class GasBlock(
    internal val gasChunk : GasChunk,
    internal val blockPos : BlockPos,
    internal val gases    : ConcurrentHashMap<AbstractGasVariant, Double>,
    internal var motion   : Vector3d = Vector3d()
) {

    fun isEmpty() : Boolean {
        return this.motion.isZero() && this.gases.all { (gas, amount) -> amount < gas.dissipateThreshold };
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.setAmount]**
     */
    fun setAmount(gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) {
        this.gases[gas] = amount;
        if (shouldSave) { this.gasChunk.chunk.setNeedsSaving(true); }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyAmount]**
     */
    fun addAmount(gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) {
        if (this.gases.containsKey(gas)) {
            this.gases[gas] = this.gases[gas]!! + amount;
        } else {
            this.gases[gas] = amount;
        }
        if (shouldSave) { this.gasChunk.chunk.setNeedsSaving(true); }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyAmount]**
     */
    fun removeAmount(gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) : Boolean {
        val prevAmount = this.gases[gas] ?: return false;
        if (prevAmount <= 0.0) {return false;}
        if (amount >= prevAmount) {
            this.gases.remove(gas);
        } else {
            this.gases[gas] = prevAmount - amount;
        }
        if (shouldSave) { this.gasChunk.chunk.setNeedsSaving(true); }
        return true;
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getAmount]**
     */
    fun getAmount(gas : AbstractGasVariant) : Double? {
        return this.gases[gas];
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getPressure]**
     */
    fun getPressure(gas : AbstractGasVariant) : Double? {
        val world = this.gasChunk.gasWorld?.world ?: return null;
        return this.getPressure(world, gas);
    }
    fun getPressure(world : ServerWorld, gas : AbstractGasVariant) : Double? {
        return (this.getAmount(gas) ?: return null) * gas.volumePerAmount(world, this.blockPos);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getPressure]**
     */
    fun getPressure() : Double? {
        val world = this.gasChunk.gasWorld?.world ?: return null;
        return this.getPressure(world);
    }
    fun getPressure(world : ServerWorld) : Double {
        return this.gases.entries.sumOf { (gas, amount) -> amount * gas.volumePerAmount(world, this.blockPos) };
    }
    fun getGasPressures(world : ServerWorld) : MutableMap<AbstractGasVariant, Double> {
        return mutableMapOf(*(this.gases.map { (gas, amount) -> Pair(gas, amount * gas.volumePerAmount(world, this.blockPos)) }).toTypedArray());
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.setMotion]**
     */
    fun setMotion(vec : Vector3d, shouldSave : Boolean = true) {
        this.motion.set(vec);
        if (shouldSave) { this.gasChunk.chunk.setNeedsSaving(true); }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyMotion]**
     */
    fun modifyMotion(vec : Vector3d, shouldSave : Boolean = true) {
        this.motion.add(vec);
        if (shouldSave) { this.gasChunk.chunk.setNeedsSaving(true); }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getMotion]**
     */
    fun getMotion() : Vector3d {
        return this.motion;
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.queuePurgeAllLoaded]**
     */
    fun purgeAllLoaded(gas : AbstractGasVariant) {
        this.gases.remove(gas);
    }


    fun write(nbt : NbtCompound) {
        val gasesNbt = NbtCompound();
        for ((gas, amount) in this.gases) {
            if (amount >= gas.dissipateThreshold) {
                gasesNbt.putDouble(gas.toString(), amount);
            }
        }
        if (! gasesNbt.isEmpty) {
            nbt.put("gases", gasesNbt);
        }
        if (! this.motion.isZero()) {
            nbt.putVector3d("motion", this.motion);
        }
    }

    companion object {

        fun read(gasChunk : GasChunk, blockPos : BlockPos, nbt : NbtCompound) {
            val gases = ConcurrentHashMap<AbstractGasVariant, Double>();
            if (nbt.contains("gases", NbtElement.COMPOUND_TYPE.toInt())) {
                val gasesNbt = nbt.getCompound("gases");
                for (gasId in gasesNbt.keys) {
                    try {
                        val gas = GasRegistry.getRegisteredGas(Identifier(gasId));
                        if (gas != null) {
                            gases[gas] = gasesNbt.getDouble(gasId);
                            continue;
                        }
                    } catch (_ : Exception) {}
                    ModMain.LOGGER.warn("Voiding invalid gas VARIANT ${gasId} at BLOCK ${blockPos} in CHUNK ${gasChunk.chunkPos} in WORLD ${gasChunk.gasWorld?.id} containing invalid data.");
                }
            }
            val motion = nbt.getVector3d("motion") ?: Vector3d();
            gasChunk.gasBlocks[blockPos] = GasBlock(gasChunk, blockPos, gases, motion = motion);
        }

    }

}