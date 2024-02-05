package net.totobirdcreations.gaslib.world

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.world.ClientWorld
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.chunk.WorldChunk
import net.totobirdcreations.gaslib.Mod
import net.totobirdcreations.gaslib.util.RGBA
import org.joml.Vector3d
import team.lodestar.lodestone.setup.LodestoneParticles
import team.lodestar.lodestone.systems.rendering.particle.Easing
import team.lodestar.lodestone.systems.rendering.particle.WorldParticleBuilder
import team.lodestar.lodestone.systems.rendering.particle.data.ColorParticleData
import team.lodestar.lodestone.systems.rendering.particle.data.GenericParticleData
import team.lodestar.lodestone.systems.rendering.particle.data.SpinParticleData
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random


@Environment(EnvType.CLIENT)
internal object GasParticles {

    val SPAWN_CHANNEL : Identifier = Mod.id("spawn_gas_particle");


    @Environment(EnvType.CLIENT)
    private val chunks : ConcurrentHashMap<ChunkPos, MutableMap<BlockPos, Pair<WorldParticleBuilder, Vector3d>>> = ConcurrentHashMap();

    @Environment(EnvType.CLIENT)
    fun loadChunk(chunk : WorldChunk) {
        this.chunks[chunk.pos] = mutableMapOf();
    }
    @Environment(EnvType.CLIENT)
    fun unloadChunk(chunk : WorldChunk) {
        this.chunks.remove(chunk.pos);
    }

    @Environment(EnvType.CLIENT)
    fun tickClient(world : ClientWorld) {
        for ((_, chunk) in this.chunks) {
            for ((pos, particle) in chunk) {
                val (builder, motion) = particle;
                builder
                    .setMotion(motion.x, motion.y, motion.z)
                    .setLifetime(5)
                    .spawn(world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
                builder
                    .setMotion(0.0, 0.0, 0.0)
                    .setLifetime(5)
                    .spawn(world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
            }
        }
    }


    fun sendUpdate(world : ServerWorld, chunkPos : ChunkPos, blocks : Collection<GasParticlesBlock>) {
        val players = world.chunkManager.threadedAnvilChunkStorage.getPlayersWatchingChunk(chunkPos);
        if (players.isEmpty()) {return;}

        val packet = PacketByteBufs.create();
        packet.writeChunkPos(chunkPos);
        packet.writeInt(blocks.size);
        for (block in blocks) {
            packet.writeBlockPos (block.pos);
            packet.writeFloat    (block.colour.r.coerceIn(0.0f, 1.0f));
            packet.writeFloat    (block.colour.g.coerceIn(0.0f, 1.0f));
            packet.writeFloat    (block.colour.b.coerceIn(0.0f, 1.0f));
            packet.writeFloat    (block.colour.a.coerceIn(0.0f, 1.0f));
            packet.writeDouble   ((block.motion.x * 100.0).coerceIn(-1.0, 1.0));
            packet.writeDouble   ((block.motion.y * 100.0).coerceIn(-1.0, 1.0));
            packet.writeDouble   ((block.motion.z * 100.0).coerceIn(-1.0, 1.0));
        }
        for (player in players) {
            ServerPlayNetworking.send(player, SPAWN_CHANNEL, packet);
        }
    }


    @Environment(EnvType.CLIENT)
    fun clientReceiveUpdate(packet : PacketByteBuf) {
        val chunkPos = packet.readChunkPos();
        if (! this.chunks.containsKey(chunkPos)) {return;}
        val chunk = hashMapOf<BlockPos, Pair<WorldParticleBuilder, Vector3d>>();
        val count = packet.readInt();
        for (i in 0..<count) {
            val pos     = packet.readBlockPos();
            val red     = packet.readFloat();
            val green   = packet.readFloat();
            val blue    = packet.readFloat();
            val alpha   = packet.readFloat();
            val motionX = packet.readDouble();
            val motionY = packet.readDouble();
            val motionZ = packet.readDouble();
            chunk[pos] = Pair(
                WorldParticleBuilder.create(LodestoneParticles.WISP_PARTICLE)
                    .setColorData(ColorParticleData
                        .create(
                            red, green, blue,
                            0.0f, 0.0f, 0.0f
                        )
                        .build()
                    )
                    .setTransparencyData(GenericParticleData
                        .create(0.0f, alpha, 0.0f)
                        .setEasing(Easing.QUAD_OUT, Easing.LINEAR)
                        .build()
                    )
                    .setScaleData(GenericParticleData
                        .create(0.5f, 0.75f + Random.Default.nextFloat() * 0.25f, 0.75f)
                        .setEasing(Easing.QUAD_OUT, Easing.LINEAR)
                        .build()
                    )
                    .setSpinData(SpinParticleData
                        .create(Random.Default.nextFloat() * 0.03125f - 0.015625f, Random.Default.nextFloat() * 0.03125f - 0.015625f)
                        .setEasing(Easing.LINEAR)
                        .build()
                    )
                    .disableNoClip()
                    .setRandomOffset(1.0),
            Vector3d(motionX, motionY, motionZ));
        }

        this.chunks[chunkPos] = chunk;
    }


    data class GasParticlesBlock(
        val pos    : BlockPos,
        val colour : RGBA,
        val motion : Vector3d
    );

}