package net.totobirdcreations.gaslib.common

import net.minecraft.fluid.Fluids
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import net.totobirdcreations.gaslib.api.GasAPI
import net.totobirdcreations.gaslib.util.RGBA
import org.joml.Vector3d
import kotlin.math.cosh


object SuperheatedGas : AbstractGasVariant(Identifier("c", "superheated")) {

    private var enabled : Boolean = false;
    fun enable() {
        if (! this.enabled) {
            this.enabled = true;
            GasAPI.registerGasVariant(this);
            GasAPI.registerBurnableFluid(Fluids.LAVA         , this, 0.001);
            GasAPI.registerBurnableFluid(Fluids.FLOWING_LAVA , this, 0.001);
        }
    }

    override fun volumePerAmount(world: ServerWorld, pos: BlockPos): Double {
        return 0.001;
    }

    override fun tick(world: ServerWorld, pos: BlockPos, motion: Vector3d, amount: Double): RGBA {
        return RGBA.ORANGE.withAlpha((0.25f * (1.0f - 1.0f / cosh(amount.toFloat() * 4.0f))).coerceAtLeast(0.0625f));
    }

}