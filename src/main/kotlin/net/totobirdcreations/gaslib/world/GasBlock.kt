@file: ApiStatus.Internal

package net.totobirdcreations.gaslib.world

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Direction
import net.totobirdcreations.gaslib.ModMain
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import net.totobirdcreations.gaslib.util.isZero
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3d
import java.util.concurrent.ConcurrentHashMap


internal data class GasBlock(
    internal val gasChunk : GasChunk,
    internal val blockPos : BlockPos,
    internal val gases    : ConcurrentHashMap<AbstractGasVariant, Double> = ConcurrentHashMap(),
    internal var motions  : Vector3d = Vector3d() // Only positive edges.
) {

    fun update() {

    }


    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.setAmount]**
     */
    fun setAmount(gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) {
        this.gases[gas] = amount;
        if (shouldSave) { this.gasChunk.chunk.setNeedsSaving(true); }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.addAmount]**
     */
    fun increaseAmount(gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) {
        if (this.gases.containsKey(gas)) {
            this.gases[gas] = this.gases[gas]!! + amount;
        } else {
            this.gases[gas] = amount;
        }
        if (shouldSave) { this.gasChunk.chunk.setNeedsSaving(true); }
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.addAmount]**
     */
    fun decreaseAmount(gas : AbstractGasVariant, amount : Double, shouldSave : Boolean = true) : Boolean {
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
    fun getAmount(gas : AbstractGasVariant) : Double {
        return this.gases[gas] ?: 0.0;
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getPressure]**
     */
    fun getPressure(gas : AbstractGasVariant? = null) : Double? {
        val world = this.gasChunk.gasWorld?.world ?: return null;
        return if (gas != null) {
            this.getAmount(gas) * gas.volumePerAmount(world, this.blockPos)
        } else {
            this.gases.entries.sumOf { (gas, amount) -> amount * gas.volumePerAmount(world, this.blockPos) }
        }
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.setMotion]**
     */
    fun setMotion(dir : Direction, amount : Double, shouldSave : Boolean = true) : Boolean {
        when (dir) {
            Direction.UP    -> { this.motions.y = amount; }
            Direction.SOUTH -> { this.motions.z = amount; }
            Direction.EAST  -> { this.motions.x = amount; }
            else            -> {
                val blockPos = this.blockPos.offset(dir);
                return this.gasChunk.gasWorld?.setMotion(ChunkPos(blockPos), blockPos, dir.opposite, amount, shouldSave = shouldSave) == true;
            }
        };
        if (shouldSave) { this.gasChunk.chunk.setNeedsSaving(true); };
        return true;
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.addMotion]**
     */
    fun addMotion(dir : Direction, amount : Double, shouldSave : Boolean = true) : Boolean {
        when (dir) {
            Direction.UP    -> { this.motions.y += amount; }
            Direction.SOUTH -> { this.motions.z += amount; }
            Direction.EAST  -> { this.motions.x += amount; }
            else            -> {
                val blockPos = this.blockPos.offset(dir);
                return this.gasChunk.gasWorld?.addMotion(ChunkPos(blockPos), blockPos, dir.opposite, -amount, shouldSave = shouldSave) == true;
            }
        };
        if (shouldSave) { this.gasChunk.chunk.setNeedsSaving(true); };
        return true;
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getMotion]**
     */
    fun getMotion(dir : Direction) : Double? {
        return when (dir) {
            Direction.UP    -> { this.motions.y }
            Direction.SOUTH -> { this.motions.z }
            Direction.EAST  -> { this.motions.x }
            else            -> {
                val blockPos = this.blockPos.offset(dir);
                this.gasChunk.gasWorld?.getMotion(ChunkPos(blockPos), blockPos, dir.opposite);
            }
        };
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.queuePurgeAllLoaded]**
     */
    fun purgeAllLoaded(gas : AbstractGasVariant) {
        this.gases.remove(gas);
    }


    fun clear() {
        this.gases.clear();
        this.motions.zero();
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
        if (! this.motions.x.isZero()) { nbt.putDouble("motionX", this.motions.x); }
        if (! this.motions.y.isZero()) { nbt.putDouble("motionY", this.motions.y); }
        if (! this.motions.z.isZero()) { nbt.putDouble("motionZ", this.motions.z); }
    }

    fun read(nbt : NbtCompound) {
        this.gases.clear();
        if (nbt.contains("gases", NbtElement.COMPOUND_TYPE.toInt())) {
            val gasesNbt = nbt.getCompound("gases");
            for (gasId in gasesNbt.keys) {
                try {
                    val gas = GasRegistry.getRegisteredGas(Identifier(gasId));
                    if (gas != null) {
                        this.gases[gas] = gasesNbt.getDouble(gasId);
                        continue;
                    }
                } catch (_ : Exception) {}
                ModMain.LOGGER.warn("Voiding invalid gas VARIANT ${gasId} at BLOCK ${blockPos} in CHUNK ${gasChunk.chunkPos} in WORLD ${gasChunk.gasWorld?.id} containing invalid data.");
            }
        }
        this.motions.set(
            nbt.getDouble("motionX"),
            nbt.getDouble("motionY"),
            nbt.getDouble("motionZ")
        );
    }

}