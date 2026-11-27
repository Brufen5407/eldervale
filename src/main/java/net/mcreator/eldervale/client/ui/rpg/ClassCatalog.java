package net.mcreator.eldervale.client.ui.rpg;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

/**
 * Display data for the skills of every class (client only): names, mana cost, cooldown, description, icon.
 * Texts follow the CLASSI sheet; costs and cooldowns match SkillHud's table.
 * TODO: SkillHud.SKILLS and FancyClassScreen.CLASSES hold the same data; point them here when they are restyled.
 */
public final class ClassCatalog {

	private ClassCatalog() {
	}

	/** kind: "SKILL 1", "SKILL 2" or "PASSIVE". mana / cooldown (seconds) are 0 for passives. icon is a file in textures/screens, null for passives. */
	public record Skill(String kind, String name, int mana, int cooldown, String desc, String icon) {
		public boolean passive() {
			return "PASSIVE".equals(kind);
		}

		public ResourceLocation iconTexture() {
			return icon == null ? null : ResourceLocation.fromNamespaceAndPath("eldervale", "textures/screens/" + icon);
		}
	}

	public record Info(String id, String name, Skill[] skills) {
	}

	private static final Map<String, Info> BY_ID = new HashMap<>();

	private static void add(String id, Skill s1, Skill s2, Skill passive) {
		String name = id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
		BY_ID.put(id, new Info(id, name, new Skill[]{s1, s2, passive}));
	}

	private static Skill sk1(String name, int mana, int cd, String desc, String icon) {
		return new Skill("SKILL 1", name, mana, cd, desc, icon);
	}

	private static Skill sk2(String name, int mana, int cd, String desc, String icon) {
		return new Skill("SKILL 2", name, mana, cd, desc, icon);
	}

	private static Skill pas(String name, String desc) {
		return new Skill("PASSIVE", name, 0, 0, desc, null);
	}

	static {
		add("paladin",
				sk1("Holy Shields", 50, 30, "For 5 seconds the paladin takes no damage.", "paladin_skill1.png"),
				sk2("Holy Zone", 30, 20, "The paladin gains resistance and regeneration for 2s. In a team, allies receive the same effects for 5 seconds.", "paladin_skill2.png"),
				pas("Sacred Zone", "Monsters within a radius of 6 are slowed."));
		add("berserker",
				sk1("Berserk Slam", 30, 12, "A small leap that deals 5 damage on impact in an area of 7.", "berserker_skill1.png"),
				sk2("Berserk Charge", 20, 14, "The player dashes forward and knocks back all enemies in an area of 5.", "berserker_skill2.png"),
				pas("Berserk Mode", "Below 50% health the player gains a 50% boost to attack and speed. When attacking, nearby mobs take 50% of the damage dealt."));
		add("ranger",
				sk1("Decoy", 35, 25, "The player gains speed and invisibility for 5s, allowing escape, and summons a dummy that draws mob aggro for 8s, then disappears.", "ranger_skill1.png"),
				sk2("Magic Arrows", 40, 12, "The player leaps backward and fires 3 magic arrows at the enemy.", "ranger_skill2.png"),
				pas("Crazy Reflex", "When the player takes damage they gain Speed I for 3 seconds. Cooldown 8s."));
		add("mage",
				sk1("Arcane Nova", 40, 12, "The Mage releases an arcane explosion around himself, damaging and knocking back all enemies within a radius of 5.", "mageskill1.png"),
				sk2("Meteor Strike", 65, 20, "A meteor falls on a chosen point within 12 blocks, causing a powerful magic explosion that damages all enemies in the area.", "mageskill2.png"),
				pas("Arcane Mastery", "Each time the Mage uses a skill he gains +10% magic damage for 4s, stacking up to 3 times."));
		add("assassin",
				sk1("Shadow Strike", 25, 8, "The Assassin dashes to the nearest enemy and strikes from behind, dealing bonus damage if the target is not looking at him.", "assassin_skill1.png"),
				sk2("Smoke Bomb", 30, 18, "A smoke cloud in a radius of 4 slows nearby enemies and makes them lose sight of the Assassin, who gains invisibility for 5s and a burst of speed for 2s.", "assassin_skill2.png"),
				pas("Deadly Ambush", "+25% damage against enemies that are not targeting the Assassin. No bonus against enemies already fighting him."));
		add("monk",
				sk1("Thousand Fists", 25, 7, "The Monk teleports in front of a nearby enemy and throws 5 lightning-fast punches, each dealing 40% of his ATT. A landed combo adds a Flow State point.", "monk_skill1.png"),
				sk2("Divine Prayer", 35, 20, "The Monk prays and cannot move for 5s. A sacred aura pushes enemies back in a radius of 5 and heals him gradually.", "monk_skill2.png"),
				pas("Flow State", "Each consecutive hit raises attack speed: 2nd +5%, 3rd +10%, 4th and beyond +15%. The combo resets after 3s without hitting."));
		add("werewolf",
				sk1("Beast Transformation", 30, 25, "For 8s the Werewolf takes his beast form: +30% attack speed, +20% movement speed and 1 HP regenerated per second.", "werewolfskill1.png"),
				sk2("Feral Pounce", 20, 10, "The Werewolf leaps at the nearest enemy within 8 blocks, landing with an area attack that slows nearby enemies.", "werewolfskill2.png"),
				pas("Predator's Instinct", "Below 50% health the Werewolf gains +15% movement speed and +15% attack speed."));
		add("alchemist",
				sk1("Volatile Brew", 35, 12, "An unstable potion explodes on impact, creating a toxic cloud in a radius of 4. Enemies take damage over time and are slowed for 4s.", "alchemistskill1.png"),
				sk2("Reactive Compound", 30, 14, "A substance sticks to the first enemy hit and violently explodes after 3s.", "alchemistskill2.png"),
				pas("Contamination", "Enemies hit by a potion accumulate contamination. After 3 applications it explodes for extra damage."));
		add("spellblade",
				sk1("Arcane Slash", 25, 8, "The Spellblade charges his weapon with arcane energy and slashes, releasing a magic wave that damages every enemy in its path.", "spellbladeskill1.png"),
				sk2("Phantom Edge", 40, 16, "A magic blade strikes the nearest enemy within 8 blocks, dealing magic damage and briefly slowing it.", "spellbladeskill2.png"),
				pas("Arcane Conversion", "Each landed physical attack restores 2 mana. Can trigger once every 2s."));
	}

	/** classId is the value of PLAYER_CLASS (lower case); null when no class is chosen. */
	public static Info get(String classId) {
		return classId == null ? null : BY_ID.get(classId.toLowerCase(Locale.ROOT));
	}
}
