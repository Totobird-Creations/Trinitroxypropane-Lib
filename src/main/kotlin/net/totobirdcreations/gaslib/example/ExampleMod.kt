package net.totobirdcreations.gaslib.example

import net.totobirdcreations.gaslib.Mod
import net.totobirdcreations.gaslib.api.GasAPI


object ExampleMod {
    private var enabled : Boolean = false;

    fun enable() {
        if (! this.enabled) {
            this.enabled = true;
            GasAPI.register(IgnitedTestGas);
            Mod.LOGGER.info("Example Mod Enabled.");
        }
    }

}