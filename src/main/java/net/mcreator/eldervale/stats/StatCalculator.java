package net.mcreator.eldervale.stats;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.core.Holder;

import net.mcreator.eldervale.network.EldervaleModVariables;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The ONLY writer of the final combat stats. Always recomputes from scratch
 * (class base + attribute bonuses + equipment + temporary), so it can be called any number
 * of times without stacking bonuses. See StatsConfig for the formulas.
 *
 * Layers: 1 Base (ClassProfile), 2 Attribute bonuses, 3 Equipment bonuses, 4 Temporary bonuses, 5 Final.
 * Existing buffs (Berserk Mode, Flow State) are vanilla attribute modifiers applied on top: untouched.
 *
 * compute() is a pure function (no player, no side effects): the GUI uses it for previews.
 */
public final class StatCalculator {
	private StatCalculator() {
	}

	public enum Stat {
		HP, ATT, DEF, ATT_SPEED, SPEED, MANA, MANA_REGEN, CRIT_CHANCE, CRIT_DAMAGE
	}

	/** Final stats. def is a fraction (0.10 = 10), critChance in %, critDamage in % (150 = x1.5). */
	public record Result(double hp, double att, double def, double attSpeed, double speed, double mana, double manaRegen, double critChance, double critDamage) {
	}

	/** Flat and percent bonuses per stat. */
	public static final class Bonus {
		private final double[] flat = new double[Stat.values().length];
		private final double[] pct = new double[Stat.values().length];

		public void addFlat(Stat stat, double value) {
			flat[stat.ordinal()] += value;
		}

		public void addPct(Stat stat, double value) {
			pct[stat.ordinal()] += value;
		}

		public void add(Bonus other) {
			for (int i = 0; i < flat.length; i++) {
				flat[i] += other.flat[i];
				pct[i] += other.pct[i];
			}
		}

		public double flat(Stat stat) {
			return flat[stat.ordinal()];
		}

		public double pct(Stat stat) {
			return pct[stat.ordinal()];
		}
	}

	/** Hook for future equipment / temporary systems. Nothing is registered yet. */
	public interface BonusProvider {
		void collect(ServerPlayer player, Bonus out);
	}

	private static final List<BonusProvider> EQUIPMENT = new CopyOnWriteArrayList<>();
	private static final List<BonusProvider> TEMPORARY = new CopyOnWriteArrayList<>();

	public static void registerEquipmentProvider(BonusProvider provider) {
		EQUIPMENT.add(provider);
	}

	public static void registerTemporaryProvider(BonusProvider provider) {
		TEMPORARY.add(provider);
	}

	// ---------------------------------------------------------------
	//  Pure calculation (shared by server and GUI)
	// ---------------------------------------------------------------

	/** Bonus given by ONE attribute with the given invested points. */
	public static Bonus attributeBonus(ClassProfile profile, StatAttribute attribute, int points) {
		Bonus bonus = new Bonus();
		addAttributeBonus(bonus, profile, attribute, points);
		return bonus;
	}

	/** Final stats from class + invested points (+ optional extra bonuses from equipment / buffs). */
	public static Result compute(ClassProfile profile, int[] points, Bonus extra) {
		Bonus total = new Bonus();
		for (StatAttribute attribute : StatAttribute.values())
			addAttributeBonus(total, profile, attribute, points[attribute.ordinal()]);
		if (extra != null)
			total.add(extra);

		return new Result(
				Math.max(1.0, finalValue(profile.hp(), Stat.HP, total)),
				Math.max(0.0, finalValue(profile.att(), Stat.ATT, total)),
				Mth.clamp(finalValue(profile.def(), Stat.DEF, total), 0.0, StatsConfig.MAX_DEFENCE),
				Math.max(0.1, finalValue(profile.attSpeed(), Stat.ATT_SPEED, total)),
				Math.max(0.01, finalValue(profile.speed(), Stat.SPEED, total)),
				Math.max(1.0, finalValue(profile.mana(), Stat.MANA, total)),
				Math.max(0.0, finalValue(profile.manaRegen(), Stat.MANA_REGEN, total)),
				Mth.clamp(finalValue(StatsConfig.BASE_CRIT_CHANCE, Stat.CRIT_CHANCE, total), 0.0, StatsConfig.MAX_CRIT_CHANCE),
				Mth.clamp(finalValue(StatsConfig.BASE_CRIT_DAMAGE, Stat.CRIT_DAMAGE, total), 100.0, StatsConfig.MAX_CRIT_DAMAGE));
	}

	private static void addAttributeBonus(Bonus out, ClassProfile profile, StatAttribute attribute, int points) {
		double eff = points * profile.multiplier(attribute);
		switch (attribute) {
			case STRENGTH -> out.addPct(Stat.ATT, Math.min(eff * StatsConfig.STRENGTH_ATT_PCT, StatsConfig.STRENGTH_ATT_PCT_CAP));
			case VITALITY -> out.addFlat(Stat.HP, Math.min(eff * StatsConfig.VITALITY_HP, StatsConfig.VITALITY_HP_CAP));
			case DEFENSE -> out.addFlat(Stat.DEF, Math.min(eff * StatsConfig.DEFENSE_DEF, StatsConfig.DEFENSE_DEF_CAP));
			case AGILITY -> {
				out.addPct(Stat.SPEED, Math.min(eff * StatsConfig.AGILITY_SPEED_PCT, StatsConfig.AGILITY_SPEED_PCT_CAP));
				out.addPct(Stat.ATT_SPEED, Math.min(eff * StatsConfig.AGILITY_ATT_SPEED_PCT, StatsConfig.AGILITY_ATT_SPEED_PCT_CAP));
			}
			case INTELLIGENCE -> {
				out.addFlat(Stat.MANA, Math.min(eff * StatsConfig.INTELLIGENCE_MANA, StatsConfig.INTELLIGENCE_MANA_CAP));
				out.addFlat(Stat.MANA_REGEN, Math.min(eff * StatsConfig.INTELLIGENCE_REGEN, StatsConfig.INTELLIGENCE_REGEN_CAP));
			}
			case DEXTERITY -> {
				out.addFlat(Stat.CRIT_CHANCE, Math.min(eff * StatsConfig.DEXTERITY_CRIT_CHANCE, StatsConfig.DEXTERITY_CRIT_CHANCE_CAP));
				out.addFlat(Stat.CRIT_DAMAGE, Math.min(eff * StatsConfig.DEXTERITY_CRIT_DAMAGE, StatsConfig.DEXTERITY_CRIT_DAMAGE_CAP));
			}
		}
	}

	private static double finalValue(double base, Stat stat, Bonus total) {
		return (base + total.flat(stat)) * (1.0 + total.pct(stat));
	}

	// ---------------------------------------------------------------
	//  Server side
	// ---------------------------------------------------------------

	/** Recalculate only if the class changed since the last calculation (used when the class menu closes). */
    public static void recalculateIfClassChanged(ServerPlayer player) {
	       EldervaleModVariables.PlayerVariables vars = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
	    if (ClassProfile.get(vars.PLAYER_CLASS) == null)
		return;
	    if (!vars.PLAYER_CLASS.equals(player.getData(StatsAttachments.PLAYER_ATTRIBUTES).appliedClass)) {
		recalculate(player);
		vars.MANA = vars.MANA_MAX; // full mana when a class is chosen
		vars.markSyncDirty();
	 }
 }

	public static void recalculate(ServerPlayer player) {
		EldervaleModVariables.PlayerVariables vars = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		ClassProfile profile = ClassProfile.get(vars.PLAYER_CLASS);
		if (profile == null)
			return; // no class chosen yet: leave everything as it is
		PlayerAttributes attributes = player.getData(StatsAttachments.PLAYER_ATTRIBUTES);

		Bonus extra = new Bonus();
		for (BonusProvider provider : EQUIPMENT)
			provider.collect(player, extra);
		for (BonusProvider provider : TEMPORARY)
			provider.collect(player, extra);

		Result r = compute(profile, attributes.snapshot(), extra);

		vars.HEALT_MAX = r.hp();
		vars.ATTACK = r.att();
		vars.DEFENCE = r.def();
		vars.ATTACK_SPEED = r.attSpeed();
		vars.SPEED = r.speed();
		vars.MANA_MAX = r.mana();
		vars.MANA_REGEN = r.manaRegen();
		vars.CRITICAL_CHANCE = r.critChance();
		vars.CRITICAL_DAMAGE = r.critDamage();
		if (vars.MANA > vars.MANA_MAX)
			vars.MANA = vars.MANA_MAX;
		vars.markSyncDirty();

		setBase(player, Attributes.MAX_HEALTH, r.hp());
		setBase(player, Attributes.ATTACK_DAMAGE, r.att());
		setBase(player, Attributes.ATTACK_SPEED, r.attSpeed());
		setBase(player, Attributes.MOVEMENT_SPEED, r.speed());
		if (player.getHealth() > player.getMaxHealth())
			player.setHealth(player.getMaxHealth());

		attributes.appliedClass = vars.PLAYER_CLASS;
	}

	/**
	 * Server-side validated spending. Returns true if at least one point was spent.
	 * The client only sends a request: amount and attribute are clamped here.
	 */
	public static boolean spend(ServerPlayer player, StatAttribute attribute, int requested) {
		EldervaleModVariables.PlayerVariables vars = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		if (ClassProfile.get(vars.PLAYER_CLASS) == null)
			return false;
		PlayerAttributes attributes = player.getData(StatsAttachments.PLAYER_ATTRIBUTES);
		int amount = Mth.clamp(requested, 1, StatsConfig.MAX_SPEND_PER_REQUEST);
		amount = Math.min(amount, (int) Math.floor(vars.STAT_POINT));
		amount = Math.min(amount, StatsConfig.MAX_POINTS_PER_ATTRIBUTE - attributes.get(attribute));
		if (amount <= 0)
			return false;
		vars.STAT_POINT -= amount;
		attributes.add(attribute, amount);
		vars.markSyncDirty();
		recalculate(player);
		StatsNetwork.sync(player);
		return true;
	}

	private static void setBase(ServerPlayer player, Holder<Attribute> attribute, double value) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null && instance.getBaseValue() != value)
			instance.setBaseValue(value);
	}
}
