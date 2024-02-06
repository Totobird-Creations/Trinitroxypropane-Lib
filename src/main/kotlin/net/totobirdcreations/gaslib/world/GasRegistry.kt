@file: ApiStatus.Internal

package net.totobirdcreations.gaslib.world

import net.minecraft.fluid.Fluid
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import org.jetbrains.annotations.ApiStatus
import org.valkyrienskies.core.util.toImmutableSet


internal object GasRegistry {

    private val registeredGasVariants : MutableMap<Identifier, AbstractGasVariant> = mutableMapOf();
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.registerGasVariant]**
     */
    fun <T : AbstractGasVariant> registerGasVariant(gas : T) {
        val gasId = gas.id;
        assert(! this.registeredGasVariants.containsKey(gasId)) { -> "Gas variant `${gasId}` is already registered." };
        this.registeredGasVariants[gasId] = gas;
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.isGasRegistered]**
     */
    fun <T : AbstractGasVariant> isGasRegistered(gas : T) : Boolean {
        return this.registeredGasVariants.containsKey(gas.id);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getRegisteredGasVariant]**
     */
    fun getRegisteredGas(gasId : Identifier) : AbstractGasVariant? {
        return this.registeredGasVariants[gasId];
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getRegisteredGasVariantIds]**
     */
    fun getRegisteredGasIds() : Collection<Identifier> {
        return this.registeredGasVariants.keys.toImmutableSet();
    }

    private val registeredBurnableFluids : MutableMap<Identifier, MutableMap<Identifier, Pair<AbstractGasVariant, Double>>> = mutableMapOf();
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.registerBurnableFluid]**
     */
    fun <T : Fluid, G : AbstractGasVariant> registerBurnableFluid(fluid : T, gas : G, multiplier : Double = 1.0) {
        val gasVariants = registeredBurnableFluids.getOrPut(Registries.FLUID.getId(fluid)) { -> mutableMapOf() };
        val gasId       = gas.id;
        assert(! gasVariants.containsKey(gasId)) { -> "Gas variant `${gasId}` has already been attached to fluid `${fluid}`." };
        gasVariants[gas.id] = Pair(gas, multiplier);
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.isBurnableFluidRegistered]**
     */
    fun <T : Fluid> isBurnableFluidRegistered(fluid : T) : Boolean {
        return this.registeredBurnableFluids.containsKey(Registries.FLUID.getId(fluid));
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getRegisteredBurnableFluidGasVariant]**
     */
    fun <T : Fluid> getRegisteredBurnableFluidGasVariant(fluid : T) : Collection<Pair<AbstractGasVariant, Double>> {
        return (this.registeredBurnableFluids[Registries.FLUID.getId(fluid)] ?: return listOf())
            .values.toImmutableSet();
    }
    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getRegisteredBurnableFluids]**
     */
    fun getRegisteredBurnableFluids() : Collection<Fluid> {
        return this.registeredBurnableFluids.keys
            .map { id -> Registries.FLUID.get(id) }
            .toImmutableSet();
    }

}