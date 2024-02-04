package net.totobirdcreations.gaslib.world

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Direction
import net.totobirdcreations.gaslib.Mod
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import net.totobirdcreations.gaslib.util.RGBA
import org.joml.Vector3d
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign
import kotlin.properties.Delegates


private const val MAX_VOLUME_PER_BLOCK : Double = 1.0;

internal const val MAX_MOTION : Double = 100.0;


internal fun GasChunk.tick(gasWorld : GasWorld) {
    this.gasWorld = gasWorld;
    var save   = false;
    val blocks = mutableListOf<GasParticles.GasParticlesBlock>();
    for ((pos, block) in this.gasBlocks.entries.shuffled()) {
        var colour : RGBA? = null;
        val nextMotion     = Vector3d();

        val pressures = hashMapOf<AbstractGasVariant, Double>();
        var pressure = block.gases.entries.sumOf { (gas, amount) ->
            val p = amount * gas.volumePerAmount(gasWorld.world, pos);
            pressures[gas] = p;
            p
        };
        // Calculate how much of the gas needs to be forced out to neighboring blocks.
        val totalAmountToForceTransfer = (pressure - MAX_VOLUME_PER_BLOCK).coerceAtLeast(0.0);

        // Get info about neighboring blocks and decide how much of the gas each direction should get.
        var dirsPressureDiffSum = 0.0;
        val dirsInfos = mutableMapOf(*Direction.entries.shuffled().mapNotNull { dir ->
            val dirBlockPos  = pos.offset(dir);
            val dirChunkPos  = ChunkPos(dirBlockPos);
            val dirPressure  = gasWorld.getPressure(dirChunkPos, dirBlockPos) ?: return@mapNotNull null;
            val pressureDiff = (pressure - dirPressure).coerceAtLeast(0.0);
            dirsPressureDiffSum += pressureDiff;
            Pair(dir, DirNeighborInfo(
                dirBlockPos,
                dirChunkPos,
                dirPressure,
                pressureDiff
            ))
        }.toTypedArray());
        for ((_, dirInfo) in dirsInfos) {
            dirInfo.moveFrac /= dirsPressureDiffSum;
        }

        // Handle each gas individually.
        for ((gas, amount) in block.gases) {
            // If gas is thin enough, destroy it.
            val dissipateThreshold = gas.dissipateThreshold(gasWorld.world, pos);
            if (amount < dissipateThreshold) {
                block.gases.remove(gas);
                save = true;
                continue;
            }
        }

        val nextMotionLen = nextMotion.length();
        if (nextMotionLen > MAX_MOTION) {
            nextMotion.normalize().mul(MAX_MOTION);
        }
        if (block.motion.distanceSquared(nextMotion) > 0.01) {
            nextMotion.sub(block.motion);
            nextMotion.mul(0.5);
            block.motion.add(nextMotion);
            save = true;
        }
        if (colour != null && colour.a > 0.0) {
            blocks.add(GasParticles.GasParticlesBlock(
                pos,
                colour,
                block.motion
            ));
        }

    }
    if (save) {
        this.chunk.setNeedsSaving(true);
        GasParticles.sendUpdate(gasWorld.world, this.chunkPos, blocks);
    }
}


// Basically just a tuple.
private data class DirNeighborInfo(
    val blockPos : BlockPos,
    val chunkPos : ChunkPos,
    val pressure : Double,
    var moveFrac : Double
);
