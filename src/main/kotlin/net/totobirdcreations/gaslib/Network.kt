package net.totobirdcreations.gaslib

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import org.joml.Vector3d
import team.lodestar.lodestone.setup.LodestoneParticles
import team.lodestar.lodestone.systems.rendering.particle.Easing
import team.lodestar.lodestone.systems.rendering.particle.WorldParticleBuilder
import team.lodestar.lodestone.systems.rendering.particle.data.ColorParticleData
import team.lodestar.lodestone.systems.rendering.particle.data.GenericParticleData
import team.lodestar.lodestone.systems.rendering.particle.data.SpinParticleData
import kotlin.random.Random


internal object Network {

    val PARTICLE_CHANNEL : Identifier = Mod.id("display_particle");

    @Environment(EnvType.CLIENT)
    fun clientReceiveParticle(client : MinecraftClient, packet : PacketByteBuf) {
        try {
            val world = client.world;
            if (world != null) {
                val pos          = packet.readBlockPos();
                val transparency = packet.readFloat();
                val velocity     = packet.readVec3d().mul(1.5);
                WorldParticleBuilder.create(LodestoneParticles.SMOKE_PARTICLE)
                    .setColorData(ColorParticleData
                        .create(0.75f, 0.375f, 0.0f, 0.75f, 0.375f, 0.0f)
                        .build()
                    )
                    .setScaleData(GenericParticleData
                        .create(0.5f, 0.75f + Random.Default.nextFloat() * 0.25f, 0.75f)
                        .setEasing(Easing.QUAD_OUT, Easing.LINEAR)
                        .build()
                    )
                    .setTransparencyData(GenericParticleData
                        .create(0.0f, transparency, 0.0f)
                        .setEasing(Easing.QUAD_OUT, Easing.LINEAR)
                        .build()
                    )
                    .setSpinData(SpinParticleData
                        .create(Random.Default.nextFloat() * 0.03125f - 0.015625f, Random.Default.nextFloat() * 0.03125f - 0.015625f)
                        .setEasing(Easing.LINEAR)
                        .build()
                    )
                    .addMotion(velocity.x, velocity.y, velocity.z)
                    .disableNoClip()
                    .setRandomOffset(0.5)
                    .setLifetime(5)
                    .spawn(world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
            }
        } catch (_ : Exception) {}
    }


    private fun PacketByteBuf.writeVec3d(vector : Vector3d) : PacketByteBuf {
        this.writeDouble(vector.x);
        this.writeDouble(vector.y);
        this.writeDouble(vector.z);
        return this;
    }
    private fun PacketByteBuf.readVec3d() : Vector3d {
        return Vector3d(this.readDouble(), this.readDouble(), this.readDouble());
    }

}