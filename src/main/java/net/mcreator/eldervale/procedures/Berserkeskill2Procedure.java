package net.mcreator.eldervale.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.gameplay.CombatDamage;

/**
 * Berserker skill 2: BERSERK CHARGE.
 * Damage is now ATTACK x CHARGE_DAMAGE_MULT (0.8 x 2.5 base ATT = the old fixed 2), dealt with the caster as
 * attacker through CombatDamage (XP, critical, Party protection). Damage and knockback only affect hostile
 * living entities (no items, villagers, pets or allies).
 */
public class Berserkeskill2Procedure {
	private static final double KNOCKBACK_RADIUS = 5.0D;
	private static final double KNOCKBACK_POWER = 1.5D;
	private static final double DAMAGE_RADIUS = 3.5D;
	private static final double CHARGE_DAMAGE_MULT = 0.8D; // x ATTACK

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return;
		EldervaleModVariables.PlayerVariables vars = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		if (vars.PLAYER_CLASS.equals("berserker") && vars.SKILL2_CD < 1 && vars.MANA >= 20) {
			player.setDeltaMovement(new Vec3(1.7 * player.getLookAngle().x, 1 * player.getLookAngle().y, 1.7 * player.getLookAngle().z));
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY(), player.getZ(), 10, 0, 0.3, 0, 0.02);

			final Vec3 center = new Vec3(player.getX(), player.getY(), player.getZ());
			float damage = (float) (vars.ATTACK * CHARGE_DAMAGE_MULT);
			for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(KNOCKBACK_RADIUS), e -> CombatDamage.isSkillTarget(player, e))) {
				double distSqr = target.position().distanceToSqr(center);
				if (distSqr > KNOCKBACK_RADIUS * KNOCKBACK_RADIUS)
					continue;
				// damage first, so the knockback below is the last word on the target's motion
				if (distSqr <= DAMAGE_RADIUS * DAMAGE_RADIUS)
					CombatDamage.skillHit(player, target, damage);
				Vec3 away = target.position().subtract(center);
				if (away.lengthSqr() > 1.0E-4D) {
					target.setDeltaMovement(away.normalize().scale(KNOCKBACK_POWER));
					target.hurtMarked = true;
				}
			}

			vars.SKILL2_CD = 280;
			vars.SKILL2_MAX_CD = 280;
			vars.MANA = vars.MANA - 20;
			vars.markSyncDirty();
		}
	}
}
