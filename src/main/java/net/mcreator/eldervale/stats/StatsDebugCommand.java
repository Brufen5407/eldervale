package net.mcreator.eldervale.stats;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

import net.mcreator.eldervale.network.EldervaleModVariables;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * TEMPORARY test tool (op level 2). Delete this file when the attributes GUI is done.
 *   /estats show
 *   /estats points <n>            add free Attribute Points
 *   /estats spend <attr> <n>      attr: strength vitality defense agility intelligence dexterity
 *   /estats reset                 refund all invested points
 */
@EventBusSubscriber
public class StatsDebugCommand {

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("estats").requires(source -> source.hasPermission(2))
				.then(Commands.literal("show").executes(StatsDebugCommand::show))
				.then(Commands.literal("points").then(Commands.argument("n", IntegerArgumentType.integer(1, 1000)).executes(StatsDebugCommand::points)))
				.then(Commands.literal("spend").then(Commands.argument("attr", StringArgumentType.word()).then(Commands.argument("n", IntegerArgumentType.integer(1, 100)).executes(StatsDebugCommand::spend))))
				.then(Commands.literal("reset").executes(StatsDebugCommand::reset)));
	}

	private static int show(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		EldervaleModVariables.PlayerVariables v = p.getData(EldervaleModVariables.PLAYER_VARIABLES);
		PlayerAttributes a = p.getData(StatsAttachments.PLAYER_ATTRIBUTES);
		StringBuilder invested = new StringBuilder();
		for (StatAttribute attribute : StatAttribute.values())
			invested.append(attribute.key).append('=').append(a.get(attribute)).append(' ');
		say(ctx, "class=" + v.PLAYER_CLASS + " freePoints=" + v.STAT_POINT);
		say(ctx, "invested: " + invested);
		say(ctx, String.format("HP %.1f | ATT %.2f | DEF %.3f | ATTSPD %.2f | SPD %.4f", v.HEALT_MAX, v.ATTACK, v.DEFENCE, v.ATTACK_SPEED, v.SPEED));
		say(ctx, String.format("MANA %.1f | REGEN %.2f | CRIT %.1f%% x%.2f", v.MANA_MAX, v.MANA_REGEN, v.CRITICAL_CHANCE, v.CRITICAL_DAMAGE / 100.0));
		return 1;
	}

	private static int points(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		EldervaleModVariables.PlayerVariables v = p.getData(EldervaleModVariables.PLAYER_VARIABLES);
		v.STAT_POINT += IntegerArgumentType.getInteger(ctx, "n");
		v.markSyncDirty();
		say(ctx, "freePoints=" + v.STAT_POINT);
		return 1;
	}

	private static int spend(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		String key = StringArgumentType.getString(ctx, "attr");
		StatAttribute target = null;
		for (StatAttribute attribute : StatAttribute.values())
			if (attribute.key.equalsIgnoreCase(key))
				target = attribute;
		if (target == null) {
			say(ctx, "unknown attribute: " + key);
			return 0;
		}
		boolean ok = StatCalculator.spend(p, target, IntegerArgumentType.getInteger(ctx, "n"));
		say(ctx, ok ? "spent on " + target.key : "refused (no class, no points or cap reached)");
		return ok ? 1 : 0;
	}

	private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		EldervaleModVariables.PlayerVariables v = p.getData(EldervaleModVariables.PLAYER_VARIABLES);
		PlayerAttributes a = p.getData(StatsAttachments.PLAYER_ATTRIBUTES);
		int refund = 0;
		for (StatAttribute attribute : StatAttribute.values()) {
			refund += a.get(attribute);
			a.add(attribute, -a.get(attribute));
		}
		v.STAT_POINT += refund;
		v.markSyncDirty();
		StatCalculator.recalculate(p);
		StatsNetwork.sync(p);
		say(ctx, "refunded " + refund + " points");
		return 1;
	}

	private static void say(CommandContext<CommandSourceStack> ctx, String text) {
		ctx.getSource().sendSuccess(() -> Component.literal(text), false);
	}
}
