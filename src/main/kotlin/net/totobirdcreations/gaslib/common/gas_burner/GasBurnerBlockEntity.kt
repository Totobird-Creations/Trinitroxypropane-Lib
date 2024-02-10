package net.totobirdcreations.gaslib.common.gas_burner

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.minecraft.block.BlockState
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.totobirdcreations.gaslib.world.GasRegistry
import net.totobirdcreations.gaslib.world.GasServer
import org.jetbrains.annotations.ApiStatus
import kotlin.math.absoluteValue


@ApiStatus.AvailableSince("Create Mod")
class GasBurnerBlockEntity(pos: BlockPos, state: BlockState) : KineticBlockEntity(GasBurnerBlock.ENTITY, pos, state) {

    @Suppress("UnstableApiUsage")
    internal val tank : SingleVariantStorage<FluidVariant> = object : SingleVariantStorage<FluidVariant>() {

        override fun getBlankVariant() : FluidVariant {
            return FluidVariant.blank()!!;
        }

        override fun getCapacity(variant : FluidVariant) : Long {
            return (FluidConstants.NUGGET.toFloat() * (this@GasBurnerBlockEntity.speed.absoluteValue / 256.0f).coerceIn(0.0f, 1.0f)).toLong();
        }

        override fun canInsert(variant: FluidVariant): Boolean {
            return GasRegistry.isBurnableFluidRegistered(variant.fluid);
        }

        override fun canExtract(variant: FluidVariant): Boolean {
            return false;
        }

        override fun onFinalCommit() {
            val world = this@GasBurnerBlockEntity.world;
            if (world is ServerWorld && this.amount > 0 && ! this.variant.isBlank) {
                val offset = this@GasBurnerBlockEntity.pos.offset(this@GasBurnerBlockEntity.cachedState.get(Properties.FACING));
                val fluid  = this.variant.fluid;
                val amount = this.amount.toDouble();
                for ((gas, multiplier) in GasRegistry.getRegisteredBurnableFluidGasVariant(fluid)) {
                    GasServer.addAmount(world, offset, gas, amount * multiplier);
                }
            }
            this.amount  = 0;
            this.variant = this.blankVariant;
        }

    };

}