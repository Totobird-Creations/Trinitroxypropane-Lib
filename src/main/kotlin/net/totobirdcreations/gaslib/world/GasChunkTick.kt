package net.totobirdcreations.gaslib.world

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Direction
import net.totobirdcreations.gaslib.ModMain
import net.totobirdcreations.gaslib.util.RGBA
import org.joml.Vector3d
import kotlin.math.absoluteValue


internal fun GasChunk.tick(gasWorld : GasWorld) {
    this.gasWorld = gasWorld;
    var save       = false;
    val particles  = mutableListOf<GasParticles.GasParticlesBlock>();
    val blockCache = mutableMapOf<BlockPos, CachedBlock>();

    for ((pos, block) in this.gasBlocks.entries.shuffled()) {
        // Get and clean up some data.
        if (block.motion.length() > 1.0) {
            block.motion.normalize();
        }
        val nextMotion = Vector3d();

        if (block.gases.isNotEmpty()) {
            var colour : RGBA? = null;

            val localBlock = blockCache.getOrPut(this, pos) ?: continue;
            val neighborBlocks = mutableMapOf(*Direction.entries.mapNotNull { dir -> Pair(dir,
                blockCache.getOrPut(this, pos.offset(dir)) ?: return@mapNotNull null)
            }.toTypedArray());
            val neighborPressureWeights = mutableMapOf(*neighborBlocks.mapNotNull { (dir, block) ->
                val pressureDiff = (localBlock.pressure - block.pressure).coerceAtLeast(0.0);
                if (pressureDiff > 0.0) { Pair(dir, pressureDiff) } else { null }
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
                var nextAmount = amount;

                val neighborTransferWeights = mutableMapOf(*neighborPressureWeights.mapNotNull { (dir, weight) ->
                    val transferWeight = weight * gas.transferResistance(gasWorld.world, pos, dir);
                    if (transferWeight > 0.0) { Pair(dir, transferWeight) } else { null }
                }.toTypedArray());
                val neighborTransferWeightsSum = neighborTransferWeights.values.sum();

                // Transfer gas to neighbors, split based on previous calculations.
                for ((dir, transferWeight) in neighborTransferWeights) {
                    val neighborBlock = neighborBlocks[dir]!!;
                    val amountToTransfer = amount * transferWeight / neighborTransferWeightsSum;
                    neighborBlock.gasBlock.addAmount(gas, amountToTransfer, shouldSave = ! neighborBlock.sameChunk);
                    nextAmount -= amountToTransfer;
                    val motionAdd = Vector3d(
                        dir.offsetX * amountToTransfer * 0.05,
                        dir.offsetY * amountToTransfer * 0.05,
                        dir.offsetZ * amountToTransfer * 0.05
                    );
                    nextMotion.add(motionAdd);
                    neighborBlock.gasBlock.motion.add(motionAdd);
                }

                // If there was enough change, save it.
                // If it hasn't changed much, don't bother.
                if (nextAmount < dissipateThreshold) {
                    block.gases.remove(gas);
                    save = true;
                } else {
                    block.gases[gas] = nextAmount;
                    if ((nextAmount - amount).absoluteValue >= 0.001) {
                        save = true;
                    }
                    try {
                        // Tick the gas.
                        val c = gas.tick(gasWorld.world, pos, block.motion, nextAmount);
                        if (c != null) { colour = colour?.mix(c) ?: c; }
                    } catch (e : Exception) {
                        ModMain.LOGGER.error("Ticking gas VARIANT ${gas.id} at BLOCK ${pos} in CHUNK ${this.chunkPos} in WORLD ${this.gasWorld?.id} failed:");
                        ModMain.LOGGER.error("  ${e}");
                    }
                }
            }

            if (colour != null && colour.a > 0.0) {
                particles.add(GasParticles.GasParticlesBlock(
                    pos,
                    colour,
                    block.motion
                ));
            }
        }

        val nextMotionLen = nextMotion.length();
        if (nextMotionLen > 1.0) {
            nextMotion.normalize();
        }
        if (block.motion.distanceSquared(nextMotion) > 0.01) {
            nextMotion.sub(block.motion);
            nextMotion.mul(0.25);
            block.motion.add(nextMotion);
            save = true;
        }

    }
    if (save) {
        this.chunk.setNeedsSaving(true);
        GasParticles.sendUpdate(gasWorld.world, this.chunkPos, particles);
    }
}


// Basically just a tuple.
private data class CachedBlock(
    val sameChunk : Boolean,
    val gasBlock  : GasBlock,
    val pressure  : Double
);
private fun MutableMap<BlockPos, CachedBlock>.getOrPut(gasChunk : GasChunk, pos : BlockPos) : CachedBlock? {
    return this.getOrPut(pos) { ->
        val chunkPos = ChunkPos(pos);
        val gasBlock = gasChunk.gasWorld!!.getOrPutBlock(chunkPos, pos) ?: return null;
        val pressure = gasChunk.getPressure(pos) ?: 0.0;
        CachedBlock(
            gasChunk.chunkPos == chunkPos,
            gasBlock,
            pressure
        )
    };
}
