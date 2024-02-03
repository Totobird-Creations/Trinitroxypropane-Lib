package net.totobirdcreations.gaslib.world

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Direction
import net.totobirdcreations.gaslib.Mod
import org.joml.Vector3d
import kotlin.math.absoluteValue


private const val MAX_VOLUME_PER_BLOCK : Double = 1.0;

private const val MAX_VELOCITY : Double = 1.0;


internal fun GasChunk.tick(gasWorld : GasWorld) {
    this.gasWorld = gasWorld;
    var save = false;
    for ((pos, block) in this.gasBlocks.entries.shuffled()) {
        val nextVelocity = Vector3d();

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
                gasWorld.getPressure(dirChunkPos, dirBlockPos) ?: return@mapNotNull null
            ))
        }.toTypedArray());

        // Handle each gas individually.
        for ((gas, amount) in block.gases) {
            // If gas is thin enough, destroy it.
            val dissipateThreshold = gas.dissipateThreshold(gasWorld.world, pos);
            if (amount < dissipateThreshold) {
                block.gases.remove(gas);
                save = true;
                continue;
            }

            val pressure = gas.volumePerAmount(gasWorld.world, pos) * amount;

            // Calculate what proportion of the gas being pushed out should go in each direction.
            val dirsWeights = mutableMapOf(*dirsInfos.mapNotNull { (dir, dirInfo) ->
                var dirWeight = 0.0;

                // The lower the target block pressure is in comparison to the source block pressure, the more weight.
                dirWeight += (totalPressure - dirInfo.pressure).coerceAtLeast(0.0);

                // Movement with the velocity has a higher weight.
                dirWeight += (block.motion.dot(
                    dir.offsetX.toDouble(),
                    dir.offsetY.toDouble(),
                    dir.offsetZ.toDouble()
                ) + 1.0).coerceAtLeast(0.0) * 2.5;
                dirWeight += ((gasWorld.getMotion(dirInfo.chunkPos, dirInfo.blockPos) ?: Vector3d()).dot(
                    dir.offsetX.toDouble(),
                    dir.offsetY.toDouble(),
                    dir.offsetZ.toDouble()
                ) * 3.75 + 5.0).coerceAtLeast(0.0) * 10.0;

                // All directions should get at least some.
                dirWeight += 2.5;

                // Blocked by walls.
                dirWeight *= 1.0 - gas.transferResistance(gasWorld.world, pos, dir);

                if (dirWeight > 0.0) {Pair(dir, dirWeight.coerceAtLeast(0.0))} else {null}
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
                    nextVelocity.add(
                        dir.offsetX.toDouble() * amountToTransfer * 10.0,
                        dir.offsetY.toDouble() * amountToTransfer * 10.0,
                        dir.offsetZ.toDouble() * amountToTransfer * 10.0
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
                try {
                    gas.tick(gasWorld.world, pos, block.motion, nextAmount);
                } catch (e : Exception) {
                    Mod.LOGGER.error("Ticking gas VARIANT ${gas.id} at BLOCK ${pos} in CHUNK ${this.chunkPos} in WORLD ${this.gasWorld?.id} failed:");
                    Mod.LOGGER.error("  ${e}");
                }
                if ((nextAmount - amount).absoluteValue > 0.0025) {
                    save = true;
                }
            }
        }

        val nextVelocityLen = nextVelocity.length();
        if (nextVelocityLen > MAX_VELOCITY) {
            nextVelocity.mul(MAX_VELOCITY / nextVelocityLen);
        }
        if (block.motion.distanceSquared(nextVelocity) > 0.01) {
            nextVelocity.sub(block.motion);
            nextVelocity.mul(0.75);
            block.motion.add(nextVelocity);
            save = true;
        }
    }
    if (save) {
        this.chunk.setNeedsSaving(true);
    }
}


// Basically just a tuple.
private data class DirNeighborInfo(
    val blockPos : BlockPos,
    val chunkPos : ChunkPos,
    val pressure : Double
);
