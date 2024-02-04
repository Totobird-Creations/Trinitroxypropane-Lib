package net.totobirdcreations.gaslib.world

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandException
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.totobirdcreations.gaslib.Mod


internal object GasCommand {

    private val GAS_IDS = { _ : CommandContext<ServerCommandSource>, builder : SuggestionsBuilder ->
        for (gasId in GasRegistry.getRegisteredGasIds()) {
            builder.suggest(gasId.toString());
        }
        builder.buildFuture()
    };

    internal fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> dispatcher.register(
            literal("gas").requires { source -> source.hasPermissionLevel(2)}

                .then(
                    literal("modify")
                    .then(
                        argument("pos", BlockPosArgumentType.blockPos())
                    .then(
                        argument("gasId", IdentifierArgumentType.identifier()).suggests(GAS_IDS)
                    .then(
                        argument("amount", DoubleArgumentType.doubleArg())
                    .executes { ctx -> modify(ctx); 1 }
                ) ) ) )

                .then(
                    literal("purge")
                    .then(
                        argument("gasId", IdentifierArgumentType.identifier()).suggests(GAS_IDS)
                        .executes { ctx -> purge(ctx, true); 1 }
                    )
                    .executes { ctx -> purge(ctx, false); 1 }
                )

        ) };
    }

    private fun literal(name : String) : LiteralArgumentBuilder<ServerCommandSource> {
        return CommandManager.literal(name)
    }
    private fun <T> argument(name : String, value : ArgumentType<T>) : RequiredArgumentBuilder<ServerCommandSource, T> {
        return CommandManager.argument(name, value);
    }



    private fun modify(ctx : CommandContext<ServerCommandSource>) {
        val world  = ctx.source.world;
        val pos    = BlockPosArgumentType   .getBlockPos   (ctx, "pos"    );
        val gasId  = IdentifierArgumentType .getIdentifier (ctx, "gasId"  );
        val amount = DoubleArgumentType     .getDouble     (ctx, "amount" );
        val gas    = GasRegistry.getRegisteredGas(gasId) ?: throw CommandException(Text.translatable("command.${Mod.ID}.gas.modify.unknown_gas_variant", gasId));

        val success = gas.modifyAmount(
            world,
            pos,
            amount
        );

        val message = Text.translatable("command.${Mod.ID}.gas.modify.${if (amount >= 0.0) {"add"} else {"remove"}}.${if (success) {"success"} else {"exception"}}",
            gasId,
            amount,
            gas.getPressure(world, pos),
            world.dimensionKey.value,
            pos.x, pos.y, pos.z
        );
        if (success) {
            ctx.source.sendFeedback({ -> message }, true);
        } else {
            throw CommandException(message);
        }
    }


    private fun purge(ctx : CommandContext<ServerCommandSource>, specificGas : Boolean) {
        val world  = ctx.source.world;

        if (specificGas) {
            val gasId = IdentifierArgumentType.getIdentifier(ctx, "gasId");
            val gas   = GasRegistry.getRegisteredGas(gasId) ?: throw CommandException(Text.translatable("command.${Mod.ID}.gas.modify.unknown_gas_variant", gasId));

            gas.queuePurgeAllLoaded(world);
            ctx.source.sendFeedback({ -> Text.translatable("command.${Mod.ID}.gas.purge.specific.success",
                gasId,
                world.dimensionKey.value
            ) }, true);

        } else {
            GasServer.queuePurgeAllLoaded(world);
            ctx.source.sendFeedback({ -> Text.translatable("command.${Mod.ID}.gas.purge.general.success",
                world.dimensionKey.value
            ) }, true);
        }
    }

}