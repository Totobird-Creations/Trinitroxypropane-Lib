package net.totobirdcreations.gaslib.api

import net.minecraft.fluid.Fluid
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
     * Registers a new gas variant.
     */
    @JvmStatic
    fun <T : AbstractGasVariant> registerGasVariant(gas : T) {
        GasRegistry.registerGasVariant(gas);
    }
    /**
     * Returns `true` if the given gas is registered.
     */
    @JvmStatic
    fun <T : AbstractGasVariant> isGasRegistered(gas : T) : Boolean {
        return GasRegistry.isGasRegistered(gas);
    }
    /**
     * Returns the gas variant object from its id, if it exists.
     */
    @JvmStatic
    fun getRegisteredGasVariant(gasId : Identifier) : AbstractGasVariant? {
        return GasRegistry.getRegisteredGas(gasId);
    }
    /**
     * Returns a `Collection` of the ids of all registered gas variants.
     */
    @JvmStatic
    fun getRegisteredGasVariantIds() : Collection<Identifier> {
        return GasRegistry.getRegisteredGasIds();
    }

    /**
     * Register that a fluid is burnable to turn into a gas.
     */
    @JvmStatic
    fun <T : Fluid, G : AbstractGasVariant> registerBurnableFluid(fluid : T, gas : G, multiplier : Double = 1.0) {
        GasRegistry.registerBurnableFluid(fluid, gas, multiplier);
    }
    /**
     * Returns `true` if there are any gas variants tied to the given fluid.
     */
    @JvmStatic
    fun <T : Fluid> isBurnableFluidRegistered(fluid : T) : Boolean {
        return GasRegistry.isBurnableFluidRegistered(fluid);
    }
    /**
     * Returns the gas variant object(s) tied to the given fluid.
     */
    @JvmStatic
    fun <T : Fluid> getRegisteredBurnableFluidGasVariant(fluid : T) : Collection<Pair<AbstractGasVariant, Double>> {
        return GasRegistry.getRegisteredBurnableFluidGasVariant(fluid);
    }
    /**
     * Returns all fluids which have a gas variant tied to them.
     */
    @JvmStatic
    fun getRegisteredBurnableFluids() : Collection<Fluid> {
        return GasRegistry.getRegisteredBurnableFluids();
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
     * Gets the amount of this gas variant at a given position and world.
     */
    @JvmStatic
    fun getAmount(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant) : Double? {
        return GasServer.getAmount(world, pos, gas);
    }
    /**
     * Gets the amount times volumePerAmount of this gas variant (or all) at a given position and world.
     */
    @JvmStatic
    fun getPressure(world : ServerWorld, pos : BlockPos, gas : AbstractGasVariant? = null) : Double? {
        return GasServer.getPressure(world, pos, gas);
    }

    /**
     * Sets the 'motion' of this gas variant at a given position and world.
     */
    @JvmStatic
    fun setMotion(world : ServerWorld, pos : BlockPos, vec : Vector3d) : Boolean {
        return GasServer.setMotion(world, pos, vec);
    }
    /**
     * Adds to the 'motion' of this gas variant at a given position and world.
     */
    @JvmStatic
    fun modifyMotion(world : ServerWorld, pos : BlockPos, vec : Vector3d) : Boolean {
        return GasServer.modifyMotion(world, pos, vec);
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