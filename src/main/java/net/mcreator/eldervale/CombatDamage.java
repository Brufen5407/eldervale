package net.mcreator.eldervale.gameplay;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Single entry point for skill damage dealt by a player.
 *
 * Why: damage with a bare DamageSource has no attacker, so it gets no XP on kill, no critical,
 * and PartyEvents cannot recognise it. Every hit made through skillHit() uses
 * damageSources().playerAttack(caster), so XP, critical hits, Party protection, Ambush and the
 * Berserk splash all work exactly like a normal melee hit.
 *
 * Server side only (never call from the client).
 */
public final class CombatDamage {
	private CombatDamage() {
	}

	/** Valid target of an area skill: a living hostile (Enemy), alive, and not the caster. */
	public static boolean isSkillTarget(ServerPlayer caster, Entity entity) {
		return entity instanceof LivingEntity living && living != caster && living.isAlive() && living instanceof Enemy;
	}

	/** Deals skill damage with the caster as attacker. Returns true if the hit was applied. */
	public static boolean skillHit(ServerPlayer caster, LivingEntity target, float amount) {
		return target.hurt(target.level().damageSources().playerAttack(caster), amount);
	}
}
