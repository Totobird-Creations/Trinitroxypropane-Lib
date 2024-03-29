package net.totobirdcreations.gaslib.common

import net.minecraft.fluid.Fluids
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import net.totobirdcreations.gaslib.api.GasAPI
import net.totobirdcreations.gaslib.util.RGBA
import org.joml.Vector3d
import kotlin.math.cbrt
import kotlin.math.pow


object SuperheatedGas : AbstractGasVariant(Identifier("c", "superheated")) {

    private var enabled : Boolean = false;
    fun enable() {
        if (! this.enabled) {
            this.enabled = true;
            GasAPI.registerGasVariant(this);
            GasAPI.registerBurnableFluid(Fluids.LAVA         , this, 0.01);
            GasAPI.registerBurnableFluid(Fluids.FLOWING_LAVA , this, 0.01);
        }
    }

    override fun volumePerAmount(world: ServerWorld, pos: BlockPos): Double {
        return 0.001;
    }

    override fun transferResistance(world : ServerWorld, fromPos : BlockPos, inDirection : Direction) : Double {
        return if (world.getBlockState(fromPos.offset(inDirection)).isAir) {6.0 / 7.0} else {0.0};
    }

    override fun tick(world: ServerWorld, pos: BlockPos, motion: Vector3d, amount: Double): RGBA {
        //GasAPI.setAmount(world, pos, this, amount * 0.9375);
        val amountF = amount.toFloat();
        return RGBA.GREEN.withAlpha((0.125f * (1.5f * amountF - 1.0f).pow(2.5f) + 0.125f + 0.1f * amountF).coerceAtLeast(0.03125f));
    }

}