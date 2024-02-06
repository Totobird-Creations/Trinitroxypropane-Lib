package net.totobirdcreations.gaslib.world

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Direction
import net.totobirdcreations.gaslib.ModMain
import net.totobirdcreations.gaslib.util.RGBA
import net.totobirdcreations.gaslib.util.isZero
import org.joml.Vector3d
import kotlin.math.absoluteValue
import kotlin.math.pow


private const val MAX_VOLUME_PER_BLOCK : Double = 1.0;

internal const val MAX_MOTION : Double = 100.0;


internal fun GasChunk.tick(gasWorld : GasWorld) {
    this.gasWorld = gasWorld;
    var save   = false;
    val blocks = mutableListOf<GasParticles.GasParticlesBlock>();
    for ((pos, block) in this.gasBlocks.entries.shuffled()) {
        var colour : RGBA? = null;
        val nextMotion     = Vector3d();

        val totalPressure = block.gases.entries.sumOf { (gas, amount) -> amount * gas.volumePerAmount(gasWorld.world, pos) };
        // Calculate how much of the gas needs to be forced out to neighboring blocks.
        val totalAmountToForceTransfer = (totalPressure - MAX_VOLUME_PER_BLOCK).coerceAtLeast(0.0);

        // Get info about neighboring blocks.
        val dirsInfos = mutableMapOf(*Direction.entries.shuffled().mapNotNull { dir ->
            val dirBlockPos = pos.offset(dir);
            val dirChunkPos = ChunkPos(dirBlockPos);
            Pair(dir, DirNeighborInfo(
                dirBlockPos,
                dirChunkPos,
                gasWorld.getPressure(dirChunkPos, dirBlockPos) ?: 0.0
            ))
        }.toTypedArray());

        // Handle each gas individually.
        for ((gas, amount) in block.gases) {
            // If gas is thin enough, destroy it.
            val dissipateThreshold = gas.dissipateThreshold;
            if (amount < dissipateThreshold) {
                block.gases.remove(gas);
                save = true;
                continue;
            }

            val pressure = gas.volumePerAmount(gasWorld.world, pos) * amount;

            // Calculate what proportion of the gas being pushed out should go in each direction.
            val dirsWeights = mutableMapOf(*dirsInfos.mapNotNull { (dir, dirInfo) ->
                // All directions should get at least some (unless blocked).
                var dirWeight = 500.0;

                val transferResistance = gas.transferResistance(gasWorld.world, pos, dir);

                // The lower the target block pressure is in comparison to the source block pressure, the more weight.
                var pressureDiff = (totalPressure - dirInfo.pressure);
                pressureDiff = pressureDiff.coerceAtLeast(0.0).pow(5.0) * 10.0;
                nextMotion.add(
                    dir.offsetX.toDouble() * pressureDiff * 10000.0 * transferResistance,
                    dir.offsetY.toDouble() * pressureDiff * 10000.0 * transferResistance,
                    dir.offsetZ.toDouble() * pressureDiff * 10000.0 * transferResistance
                );
                dirWeight += pressureDiff;

                // Movement along the motion has a higher weight.
                val veca = gasWorld.getMotion(dirInfo.chunkPos, dirInfo.blockPos) ?: Vector3d();
                //val veca = block.motion;
                val vecb = Vector3d(veca.x, veca.y, veca.z);
                if (vecb.length() > 0.0) {vecb.normalize();}
                dirWeight += (vecb.dot(
                    dir.offsetX.toDouble(),
                    dir.offsetY.toDouble(),
                    dir.offsetZ.toDouble()
                ) * 0.5 + 0.5).coerceAtLeast(0.0) * 10000.0;

                //val vecc = gasWorld.getMotion(dirInfo.chunkPos, dirInfo.blockPos) ?: Vector3d();
                val vecc = block.motion;
                val vecd = Vector3d(veca.x, veca.y, veca.z);
                if (vecd.length() > 0.0) {vecd.normalize();}
                dirWeight -= (vecd.dot(
                    dir.offsetX.toDouble(),
                    dir.offsetY.toDouble(),
                    dir.offsetZ.toDouble()
                ) * -1.0).coerceIn(0.0, 1.0) * 1000.0;

                // Blocked by walls.
                dirWeight *= transferResistance;

                if (dirWeight > 0.0) {Pair(dir, dirWeight)} else {null}
            }.toTypedArray());
            val totalDirsWeights = dirsWeights.values.sum();

            // Calculate how much of the gas is being pushed out.
            val amountToForceTransfer = totalAmountToForceTransfer * (pressure / totalPressure);
            val totalAmountToTransfer = amountToForceTransfer + (amount - amountToForceTransfer) * gas.transferProportion(gasWorld.world, pos);

            // Transfer gas to neighboring blocks depending on the weights calculated.
            var nextAmount = amount;
            for ((dir, dirWeight) in dirsWeights) {
                val dirInfo = dirsInfos[dir]!!;
                val amountToTransfer = dirWeight / totalDirsWeights * totalAmountToTransfer;

                if (
                    gasWorld.addAmount(dirInfo.chunkPos, dirInfo.blockPos, gas, amountToTransfer, shouldSave = false)
                ) {
                    nextAmount -= amountToTransfer;
                    nextMotion.add(
                        dir.offsetX.toDouble() * amountToTransfer * 0.25,
                        dir.offsetY.toDouble() * amountToTransfer * 0.25,
                        dir.offsetZ.toDouble() * amountToTransfer * 0.25
                    );
                }

            }

            // If gas is thin enough, destroy it.
            // If enough of a change was made, queue it for saving.
            if (nextAmount < dissipateThreshold) {
                block.gases.remove(gas);
                save = true;
            } else {
                block.gases[gas] = nextAmount;
                if ((nextAmount - amount).absoluteValue >= 0.001) {
                    save = true;
                }
                try {
                    val c = gas.tick(gasWorld.world, pos, block.motion, nextAmount);
                    if (c != null) { colour = colour?.mix(c) ?: c; }
                } catch (e : Exception) {
                    ModMain.LOGGER.error("Ticking gas VARIANT ${gas.id} at BLOCK ${pos} in CHUNK ${this.chunkPos} in WORLD ${this.gasWorld?.id} failed:");
                    ModMain.LOGGER.error("  ${e}");
                }
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
        } else if (nextMotion.isZero() && ! block.motion.isZero()) {
            block.motion.zero();
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
    val pressure : Double
);