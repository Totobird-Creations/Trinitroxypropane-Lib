package net.totobirdcreations.gaslib.api

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.world.ClientWorld
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.totobirdcreations.gaslib.util.RGBA
import net.totobirdcreations.gaslib.world.GasServer
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3d


@Suppress("unused")
@ApiStatus.Experimental
abstract class AbstractGasVariant(
    val id : Identifier
) {

    /**
     * Amount of space one unit of this gas takes up. A single block can store 1 volume.
     *
     * This maximum CAN BE EXCEEDED, but all excess will be forced to neighboring blocks the next tick.
     *
     * **THIS VALUE MUST FALL IN THE RANGE 0 ~ 1**
     */
    open fun volumePerAmount(world : ServerWorld, pos : BlockPos) : Double {
        return 0.01;
    }

    /**
     * Every tick, tries to pass this proportion of this gas to neighboring blocks.
     */
    open fun transferProportion(world : ServerWorld, pos : BlockPos) : Double {
        return 6.0 / 7.0;
    }

    /**
     * What proportion of this gas trying to enter a neighboring block doesn't make it.
     * Setting this to 1 will prevent any of this gas from passing into the given block.
     *
     * The gas which failed to be transferred will be returned to the original block, and will apply force to the ship accordingly.
     *
     * **THIS VALUE MUST FALL IN THE RANGE 0 ~ 1**
     */
    open fun transferResistance(world : ServerWorld, fromPos : BlockPos, inDirection : Direction) : Double {
        return if (world.getBlockState(fromPos.offset(inDirection)).isAir) {0.0} else {1.0};
    }

    /**
     * If less than this amount of gas is in a single block, it will be considered to be dissipated and will be destroyed.
     */
    open fun dissipateThreshold(world : ServerWorld, pos : BlockPos) : Double {
        return 0.1;
    }

    /**
     * Called every gas update for each block containing this gas.
     *
     * Return the colour and alpha of the gas, or `null` if this gas should be invisible.
     */
    abstract fun tick(world : ServerWorld, pos : BlockPos, motion : Vector3d, amount : Double) : RGBA?;


    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.modifyAmount]**
     */
    fun modifyAmount(world : ServerWorld, pos : BlockPos, amount : Double) : Boolean {
        return GasServer.modifyAmount(world, pos, this, amount);
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.queuePurgeAllLoaded]**
     */
    fun queuePurgeAllLoaded(world : ServerWorld) {
        return GasServer.queuePurgeAllLoaded(world, this);
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getPressure]**
     */
    fun getPressure(world : ServerWorld, pos : BlockPos) : Double? {
        return GasServer.getPressure(world, pos, this);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getAmount]**
     */
    fun getAmount(world : ServerWorld, pos : BlockPos) : Double? {
        return GasServer.getAmount(world, pos, this);
    }


    final override fun equals(other : Any?): Boolean {
        return other is AbstractGasVariant && other.id == this.id;
    }
    final override fun hashCode(): Int {
        return id.hashCode();
    }

}