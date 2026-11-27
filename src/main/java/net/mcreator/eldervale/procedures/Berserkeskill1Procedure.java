package net.mcreator.eldervale.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.gameplay.CombatDamage;
import net.mcreator.eldervale.vfx.VfxIds;
import net.mcreator.eldervale.vfx.VfxServer;
import net.mcreator.eldervale.EldervaleMod;

/**
 * Berserker skill 1: BERSERK SLAM.
 * Damage is now ATTACK x SLAM_DAMAGE_MULT (2.0 x 2.5 base ATT = the old fixed 5), dealt with the caster as
 * attacker through CombatDamage: XP on kill, critical hits and Party protection work, and only hostile
 * living entities are hit (no items, villagers, pets or allies).
 */
public class Berserkeskill1Procedure {
	private static final double SLAM_RADIUS = 3.5D;
	private static final double SLAM_DAMAGE_MULT = 2.0D; // x ATTACK

	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("berserker") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL1_CD < 1 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 30) {
			entity.setDeltaMovement(new Vec3(0, 0.6, 0));
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.SKILL1_MAX_CD = 240;
				_vars.SKILL1_CD = 240;
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 30;
				_vars.markSyncDirty();
			}
			if (entity instanceof ServerPlayer sp)
				VfxServer.send(sp, VfxIds.BERSERKER_SLAM, VfxIds.START, 0, 3.5F);
			EldervaleMod.queueServerWork(20, () -> {
				if (!(entity instanceof ServerPlayer caster))
					return;
				final Vec3 center = new Vec3(caster.getX(), caster.getY(), caster.getZ());
				if (caster.isAlive()) {
					float damage = (float) (caster.getData(EldervaleModVariables.PLAYER_VARIABLES).ATTACK * SLAM_DAMAGE_MULT);
					for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(SLAM_RADIUS), e -> CombatDamage.isSkillTarget(caster, e)))
						CombatDamage.skillHit(caster, target, damage);
				}
				VfxServer.sendAt(caster, VfxIds.BERSERKER_SLAM, VfxIds.IMPACT, 0, caster.getX(), caster.getY(), caster.getZ(), 3.5F);
			});
		}
	}
}
