package net.totobirdcreations.gaslib.common.gas_burner

import com.jozufozu.flywheel.api.Instancer
import com.jozufozu.flywheel.api.MaterialManager
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import net.minecraft.block.BlockState
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.state.property.Properties
import org.jetbrains.annotations.ApiStatus


@ApiStatus.AvailableSince("Create Mod")
class GasBurnerRenderer(models : MaterialManager, entity : GasBurnerBlockEntity):
    SingleRotatingInstance<GasBurnerBlockEntity>(models, entity)
{

    override fun getModel(): Instancer<RotatingData> {
        return this.rotatingMaterial.getModel(
            AllPartialModels.MECHANICAL_PUMP_COG,
            this.blockEntity.cachedState,
            this.blockEntity.cachedState.get(Properties.FACING)
        );
    }


    class Compat(ctx : BlockEntityRendererFactory.Context):
        KineticBlockEntityRenderer<GasBurnerBlockEntity>(ctx)
    {

        override fun getRotatedModel(entity : GasBurnerBlockEntity, state : BlockState) : SuperByteBuffer {
            return CachedBufferer.partialFacing(AllPartialModels.MECHANICAL_PUMP_COG, state);
        }

    }

}