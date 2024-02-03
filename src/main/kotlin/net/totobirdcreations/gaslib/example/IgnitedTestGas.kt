package net.totobirdcreations.gaslib.example

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.totobirdcreations.gaslib.Mod
import net.totobirdcreations.gaslib.Network.PARTICLE_CHANNEL
import net.totobirdcreations.gaslib.api.AbstractGasVariant
import org.joml.Vector3d
import kotlin.math.cosh


object IgnitedTestGas : AbstractGasVariant(Identifier("${Mod.ID}_example", "test_ignited")) {

    override fun volumePerAmount(world: ServerWorld, pos: BlockPos): Double {
        return 1.0;
    }

    override fun tick(world: ServerWorld, pos: BlockPos, velocity : Vector3d, amount: Double) {
        for (i in 0..<world.players.size) {
            val player = world.players[0];
            if (player.blockPos.isWithinDistance(pos, 512.0)) {
                val packet = PacketByteBufs.create();
                packet.writeBlockPos(pos);
                packet.writeFloat((0.75f * (1.0f - 1.0f / cosh(amount.toFloat() * 4.0f))).coerceAtLeast(0.0625f));
                packet.writeDouble(velocity.x * 0.75);
                packet.writeDouble(velocity.y * 0.75);
                packet.writeDouble(velocity.z * 0.75);
                ServerPlayNetworking.send(player, PARTICLE_CHANNEL, packet);
            }
        }
    }

}