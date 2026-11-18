package net.mcreator.eldervale.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import net.mcreator.eldervale.network.EldervaleModVariables;

import java.util.Comparator;

public class AssassinSkill2Procedure {
public static void execute(
LevelAccessor world, double x, double y, double z, Entity entity) {
if (
entity == null ) return ;
if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("assassin")&&entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL2_CD<=0&&entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA>=30) {{
final Vec3 _center = new Vec3(x, y, z);
for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(8 / 2d), e -> true)
.stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
if (!(entity == entityiterator)&&entityiterator instanceof Monster) {if(entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, true));if(entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
_entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 5, false, false));if(entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false));if (entityiterator instanceof Mob _entity && null; instanceof LivingEntity _ent) _entity.setTarget(_ent);}
}
}{EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA-30;
_vars.SKILL2_CD = 360;
_vars.SKILL2_MAX_CD = 360;
_vars.markSyncDirty();}}
}
}