package net.totobirdcreations.gaslib.api

import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.totobirdcreations.gaslib.world.GasRegistry
import net.totobirdcreations.gaslib.world.GasServer
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3d


@ApiStatus.Experimental
object GasAPI {

    /**
     * Returns the gas variant object from its id, if it exists.
     */
    @JvmStatic
    fun getRegisteredGas(gasId : Identifier) : AbstractGasVariant? {
        return GasRegistry.getRegisteredGas(gasId);
    }
    /**
     * Returns a `Collection` of the ids of all registered gas variants.
     */
    @JvmStatic
    fun getRegisteredGasIds() : Collection<Identifier> {
        return GasRegistry.getRegisteredGasIds();
    }
    /**
     * Registers a new gas variant.
     */
    @JvmStatic
    fun <T : AbstractGasVariant> register(gas : T) {
        GasRegistry.register(gas);
    }

    /**
     * Add/Remove an amount of a gas to/from a given position, assuming it is loaded.
     *
     * **If this fails for any reason, it will return `false`**
     */
    @JvmStatic
    fun modifyAmount(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant, amount : Double) : Boolean {
        return GasServer.modifyAmount(world, pos, gas, amount);
    }

    /**
     * Gets the amount times volumePerAmount of this gas variant (or all) at a given position and world.
     */
    @JvmStatic
    fun getPressure(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant? = null) : Double? {
        return GasServer.getPressure(world, pos, gas);
    }
    /**
     * Gets the amount of this gas variant at a given position and world.
     */
    @JvmStatic
    fun getAmount(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant) : Double? {
        return GasServer.getAmount(world, pos, gas);
    }
    /**
     * Gets the 'motion' of this gas variant at a given position and world.
     */
    @JvmStatic
    fun getMotion(world : ServerWorld, pos : BlockPos) : Vector3d? {
        return GasServer.getMotion(world, pos);
    }

    /**
     * Queues removal of all **loaded** gas of a gas variant (or all) in the given world.
     */
    @JvmStatic
    fun queuePurgeAllLoaded(world : ServerWorld, gas : AbstractGasVariant? = null) {
        GasServer.queuePurgeAllLoaded(world, gas);
    }

}