package net.mcreator.eldervale.team;

import java.util.function.Function;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

/**
 * Test commands for the Team core (the GUI will use the same TeamManager actions):
 *   /eteam create <name>     /eteam invite <player>   /eteam accept     /eteam decline
 *   /eteam leave             /eteam disband           /eteam kick <name>
 *   /eteam leader <name>     /eteam rename <name>     /eteam info
 * The root is "eteam" and not "team" to avoid clashing with the vanilla /team command.
 */
@EventBusSubscriber
public class TeamCommands {

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("eteam")
				.then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> run(c, p -> TeamManager.create(p, StringArgumentType.getString(c, "name"))))))
				.then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
					ServerPlayer target = EntityArgument.getPlayer(c, "player");
					return run(c, p -> TeamManager.invite(p, target));
				})))
				.then(Commands.literal("accept").executes(c -> run(c, TeamManager::accept)))
				.then(Commands.literal("decline").executes(c -> run(c, TeamManager::decline)))
				.then(Commands.literal("leave").executes(c -> run(c, TeamManager::leave)))
				.then(Commands.literal("disband").executes(c -> run(c, TeamManager::disband)))
				.then(Commands.literal("kick").then(Commands.argument("member", StringArgumentType.word()).executes(c -> run(c, p -> TeamManager.kick(p, StringArgumentType.getString(c, "member"))))))
				.then(Commands.literal("leader").then(Commands.argument("member", StringArgumentType.word()).executes(c -> run(c, p -> TeamManager.transfer(p, StringArgumentType.getString(c, "member"))))))
				.then(Commands.literal("rename").then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> run(c, p -> TeamManager.rename(p, StringArgumentType.getString(c, "name"))))))
				.then(Commands.literal("info").executes(c -> run(c, TeamManager::info))));
	}

	private static int run(CommandContext<CommandSourceStack> ctx, Function<ServerPlayer, TeamManager.Result> action) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		TeamManager.Result result = action.apply(player);
		if (result.ok())
			ctx.getSource().sendSuccess(() -> Component.literal(result.message()), false);
		else
			ctx.getSource().sendFailure(Component.literal(result.message()));
		return result.ok() ? 1 : 0;
	}
}
