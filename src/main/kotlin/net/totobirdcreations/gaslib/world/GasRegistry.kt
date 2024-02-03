package net.totobirdcreations.gaslib.world

import net.minecraft.util.Identifier
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import org.valkyrienskies.core.util.toImmutableSet


internal object GasRegistry {

    internal val registeredGases : MutableMap<Identifier, AbstractGasVariant> = mutableMapOf();

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getRegisteredGas]**
     */
    fun getRegisteredGas(gasId : Identifier) : AbstractGasVariant? {
        return this.registeredGases[gasId];
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.getRegisteredGasIds]**
     */
    fun getRegisteredGasIds() : Collection<Identifier> {
        return this.registeredGases.keys.toImmutableSet();
    }

    /**
     * **See [net.totobirdcreations.gaslib.api.GasAPI.register]**
     */
    fun <T : AbstractGasVariant> register(gas : T) {
        val gasId = gas.id;
        assert(! this.registeredGases.contains(gasId)) { -> "Gas `${gasId} is already registered.`" };
        this.registeredGases[gasId] = gas;
    }

}