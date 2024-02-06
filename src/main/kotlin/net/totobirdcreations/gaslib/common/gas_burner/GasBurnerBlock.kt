package net.totobirdcreations.gaslib.common.gas_burner

import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import net.minecraft.item.BlockItem
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.totobirdcreations.gaslib.ModMain
import org.jetbrains.annotations.ApiStatus


@ApiStatus.AvailableSince("Create Mod")
object GasBurnerBlock:
    DirectionalKineticBlock(FabricBlockSettings
        .copyOf(Blocks.IRON_BLOCK)
        .sounds(BlockSoundGroup.NETHERITE)
    ),
    BlockEntityProvider,
    ICogWheel
{

    val ITEM   : BlockItem                             = BlockItem(this, FabricItemSettings());
    val ENTITY : BlockEntityType<GasBurnerBlockEntity> = FabricBlockEntityTypeBuilder.create(this::createBlockEntity, this).build()

    private var enabled : Boolean = false;
    fun enable() {
        if (! this.enabled) {
            this.enabled = true;

            Registry.register(Registries.BLOCK             , ModMain.id("gas_burner"), this   );
            Registry.register(Registries.ITEM              , ModMain.id("gas_burner"), ITEM   );
            Registry.register(Registries.BLOCK_ENTITY_TYPE , ModMain.id("gas_burner"), ENTITY );
            @Suppress("UnstableApiUsage")
            FluidStorage.SIDED.registerForBlockEntity({ entity, dir ->
                if (entity.cachedState.get(Properties.FACING).opposite == dir) { entity.tank } else { null }
            }, ENTITY);

            if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
                BlockRenderLayerMap.INSTANCE.putBlock(this, RenderLayer.getCutout());
                BlockEntityRendererFactories
                    .register(ENTITY) { ctx -> GasBurnerRenderer.Compat(ctx) };
                InstancedRenderRegistry
                    .configure(ENTITY)
                    .factory { models, entity -> GasBurnerRenderer(models, entity) }
                    .apply();
            }

        }
    }

    override fun createBlockEntity(pos : BlockPos, state : BlockState) : GasBurnerBlockEntity {
        return GasBurnerBlockEntity(pos, state);
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.get(Properties.FACING).axis;
    }

}