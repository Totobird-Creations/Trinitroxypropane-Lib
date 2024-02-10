package net.totobirdcreations.gaslib.common.command

import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.EnumArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.math.Direction


class DirectionArgumentType : EnumArgumentType<Direction>(Direction.CODEC, Direction::values) {
    companion object {

        fun direction() : DirectionArgumentType {
            return DirectionArgumentType()
        }

        fun getDirection(ctx : CommandContext<ServerCommandSource>, key : String) : Direction {
            return ctx.getArgument(key, Direction::class.java);
        }

    }
}