package FantaLory.api.bullets;

import arc.Events;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Intersector;
import arc.math.geom.Vec2;
import arc.util.Log;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.entities.Fires;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.entities.abilities.ShieldArcAbility;
import mindustry.entities.bullet.BulletType;
import mindustry.game.EventType.UnitBulletDestroyEvent;
import mindustry.game.EventType.UnitDamageEvent;
import mindustry.gen.Bullet;
import mindustry.gen.Building;
import mindustry.gen.Healthc;
import mindustry.gen.Hitboxc;
import mindustry.gen.Unit;
import mindustry.logic.LAccess;
import mindustry.logic.Ranged;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.BlockFlag;
import FantaLory.api.MagicDamageAPI;
import FantaLory.api.TrueDamageAPI;

final class MagicBulletSupport{
    // 保留原版单位受击事件对象，避免丢失联动。
    private static final UnitDamageEvent bulletDamageEvent = new UnitDamageEvent();

    // 真实伤害触发配置：0.5% 几率，10% 最大生命值，封顶 500（对应 5000 生命值）。
    private static final float trueDamageChance = 0.005f;
    private static final float trueDamagePercent = 0.10f;
    private static final float trueDamageHealthCap = 5000f;
    private static final float trueDamageCap = 500f;

    // 单位护盾吸收判定范围（用于回溯被护盾吸收的子弹）。
    private static final float shieldAbilitySearchRange = 260f;
    private static final float forceFieldMinShield = 0.0001f;

    private MagicBulletSupport(){
    }

    // 法术子弹默认穿透规则：默认仅建筑穿透 1 层，已有更高层数时保持不变。
    static void setupDefaults(BulletType type){
        if(type == null) return;
        type.buildingDamageMultiplier = 0f;
        type.pierce = false;
        type.pierceBuilding = true;
        if(type.pierceCap < 1){
            type.pierceCap = 1;
        }
    }

    static void onRemoved(BulletType type, Bullet b){
        if(type == null || b == null) return;

        // 子弹被护盾吸收时，附带真实伤害改为作用在对应盾容上。
        if(b.absorbed()){
            try{
                tryApplyBonusTrueDamageOnAbsorbedShield(b);
            }catch(Throwable t){
                Log.err("FantaLory: true-damage shield bonus failed for absorbed bullet @ (@, @)", b.id, b.x(), b.y());
                Log.err(t);
            }
        }
    }

    static void onHitEntity(BulletType type, Bullet b, Hitboxc entity, float health){
        if(type == null || b == null || entity == null) return;

        boolean wasDead = entity instanceof Unit u && u.dead;

        if(entity instanceof Healthc h){
            float damage = b.damage;
            float shield = entity instanceof Unit u ? Math.max(u.shield(), 0f) : 0f;

            // 保留原版 maxDamageFraction 上限逻辑。
            if(type.maxDamageFraction > 0f){
                float cap = h.maxHealth() * type.maxDamageFraction + shield;
                damage = Math.min(damage, cap);
                health = Math.min(health, cap);
            }else{
                health += shield;
            }

            // 保留吸血逻辑，吸血按实际可造成伤害部分计算。
            if(type.lifesteal > 0f && b.owner instanceof Healthc o){
                float result = Math.max(Math.min(h.health(), damage), 0f);
                o.heal(result * type.lifesteal);
            }

            // 主伤害仍为法术伤害。
            MagicDamageAPI.applyMagicDamage(damage, h);
        }

        // 保留原版击退、状态效果、事件派发。
        if(entity instanceof Unit unit){
            Tmp.v3.set(unit).sub(b).nor().scl(type.knockback * 80f);
            if(type.impact) Tmp.v3.setAngle(b.rotation() + (type.knockback < 0f ? 180f : 0f));
            unit.impulse(Tmp.v3);
            unit.apply(type.status, type.statusDuration);

            Events.fire(bulletDamageEvent.set(unit, b));
        }

        // 保留单位被子弹击杀事件。
        if(!wasDead && entity instanceof Unit unit && unit.dead){
            Events.fire(new UnitBulletDestroyEvent(unit, b));
        }

        // 保留穿透计数处理。
        type.handlePierce(b, health, entity.x(), entity.y());
    }

    static void onHitTile(BulletType type, Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
        if(type == null || b == null || build == null) return;

        if(type.makeFire && build.team != b.team){
            Fires.create(build.tile);
        }

        // 友方治疗逻辑保留原版行为。
        if(type.heals() && build.team == b.team && !(build.block instanceof ConstructBlock)){
            type.healEffect.at(build.x, build.y, 0f, type.healColor, build.block);
            build.heal(type.healPercent / 100f * build.maxHealth + type.healAmount);
            type.healSound.at(build, 1f + Mathf.range(0.1f), type.healSoundVolume);
            type.hit(b);
        }else if(build.team != b.team && direct){
            // 敌方建筑命中改为法术伤害。
            type.hit(b);

            float damage = b.damage;
            if(type.maxDamageFraction > 0f){
                damage = Math.min(damage, build.maxHealth * type.maxDamageFraction);
            }

            if(type.lifesteal > 0f && b.owner instanceof Healthc o){
                float result = Math.max(Math.min(build.health, damage), 0f);
                o.heal(result * type.lifesteal);
            }

            // 主伤害仍为法术伤害。
            MagicDamageAPI.applyMagicDamage(damage, build);

            // 城墙附带真实伤害。
            tryApplyBonusTrueDamageToWall(b, build);
        }

        // 保留建筑命中后的穿透处理。
        type.handlePierce(b, initialHealth, x, y);
    }

    private static boolean hasTrueDamageProc(Bullet b){
        return b != null && Mathf.randomSeed(b.id) < trueDamageChance;
    }

    private static float calcBonusTrueDamage(Healthc target){
        return TrueDamageAPI.calculateCappedMaxHealthTrueDamage(target, trueDamagePercent, trueDamageHealthCap, trueDamageCap);
    }

    // 子弹被吸收后，优先尝试命中 force-projector 护盾容量。
    private static boolean tryApplyBonusTrueDamageToProjectorShield(Bullet b){
        float bx = b.x(), by = b.y();
        Building target = null;
        float bestDst2 = Float.MAX_VALUE;

        var shields = Vars.indexer.getEnemy(b.team(), BlockFlag.shield);
        if(shields == null || shields.isEmpty()){
            return false;
        }

        for(Building build : shields){
            if(build == null || build.team == b.team() || build.dead()){
                continue;
            }
            if(!(build.block instanceof ForceProjector projector)){
                continue;
            }
            if(!(build instanceof Ranged ranged)){
                continue;
            }

            float shield = (float)build.sense(LAccess.shield);
            if(shield <= 0f){
                continue;
            }

            float radius = ranged.range();
            if(radius <= 0f){
                continue;
            }

            if(!Intersector.isInRegularPolygon(projector.sides, build.x, build.y, radius, projector.shieldRotation, bx, by)){
                continue;
            }

            float dst2 = Mathf.dst2(build.x, build.y, bx, by);
            if(dst2 < bestDst2){
                bestDst2 = dst2;
                target = build;
            }
        }

        if(target == null){
            return false;
        }

        float shield = (float)target.sense(LAccess.shield);
        if(shield <= 0f){
            return false;
        }

        float trueDamage = calcBonusTrueDamage(target);
        if(trueDamage <= 0f){
            return false;
        }

        target.setProp(LAccess.shield, Math.max(0f, shield - trueDamage));
        return true;
    }

    private static ShieldArcAbility findAbsorbingShieldArcAbility(Unit unit, float bulletX, float bulletY, float prevX, float prevY){
        Ability[] abilities = unit.abilities;
        if(abilities == null){
            return null;
        }

        for(Ability ability : abilities){
            if(ability == null){
                continue;
            }
            if(!(ability instanceof ShieldArcAbility arc)){
                continue;
            }

            if(arc.data <= 0f){
                continue;
            }
            if(arc.whenShooting && !unit.isShooting()){
                continue;
            }

            Vec2 center = new Vec2(arc.x, arc.y).rotate(unit.rotation() - 90f).add(unit.x(), unit.y());
            float inner = Math.max(arc.radius - arc.width, 0f);
            float outer = arc.radius + arc.width;

            boolean outsideInner = !(Mathf.within(bulletX, bulletY, center.x, center.y, inner) &&
                Mathf.within(prevX, prevY, center.x, center.y, inner));
            boolean withinOuter = Mathf.within(bulletX, bulletY, center.x, center.y, outer) ||
                Mathf.within(prevX, prevY, center.x, center.y, outer);

            if(!outsideInner || !withinOuter){
                continue;
            }

            float facing = unit.rotation() + arc.angleOffset;
            boolean withinArc = Angles.within(Angles.angle(center.x, center.y, bulletX, bulletY), facing, arc.angle / 2f) ||
                Angles.within(Angles.angle(center.x, center.y, prevX, prevY), facing, arc.angle / 2f);

            if(withinArc){
                return arc;
            }
        }

        return null;
    }

    private static boolean isInsideForceFieldAbilityShield(Unit unit, float bulletX, float bulletY){
        if(unit.shield() <= forceFieldMinShield){
            return false;
        }

        Ability[] abilities = unit.abilities;
        if(abilities == null){
            return false;
        }

        for(Ability ability : abilities){
            if(ability == null){
                continue;
            }
            if(!(ability instanceof ForceFieldAbility field)){
                continue;
            }

            if(Intersector.isInRegularPolygon(field.sides, unit.x(), unit.y(), field.radius, field.rotation, bulletX, bulletY)){
                return true;
            }
        }

        return false;
    }

    // 子弹被吸收后，识别“力墙场/弧形护盾能力”单位，并扣其盾容（不扣血）。
    private static void tryApplyBonusTrueDamageToShieldAbilityUnit(Bullet b){
        float bx = b.x(), by = b.y();
        float px = b.lastX(), py = b.lastY();

        Unit unit = Units.closestEnemy(b.team(), bx, by, shieldAbilitySearchRange,
            u -> findAbsorbingShieldArcAbility(u, bx, by, px, py) != null || isInsideForceFieldAbilityShield(u, bx, by));
        if(unit == null){
            return;
        }

        float trueDamage = calcBonusTrueDamage(unit);
        if(trueDamage <= 0f){
            return;
        }

        ShieldArcAbility arc = findAbsorbingShieldArcAbility(unit, bx, by, px, py);
        if(arc != null){
            arc.data = Math.max(0f, arc.data - trueDamage);
            return;
        }

        if(isInsideForceFieldAbilityShield(unit, bx, by)){
            unit.shield(Math.max(0f, unit.shield() - trueDamage));
        }
    }

    private static void tryApplyBonusTrueDamageOnAbsorbedShield(Bullet b){
        if(!hasTrueDamageProc(b)){
            return;
        }

        // 优先 force-projector 护盾；未命中再尝试单位护盾能力。
        if(tryApplyBonusTrueDamageToProjectorShield(b)){
            return;
        }
        tryApplyBonusTrueDamageToShieldAbilityUnit(b);
    }

    // 城墙保持附带真实伤害（扣血）。
    private static void tryApplyBonusTrueDamageToWall(Bullet b, Building build){
        if(build == null || build.team == b.team || build.dead()){
            return;
        }
        if(!(build.block instanceof Wall)){
            return;
        }
        if(!hasTrueDamageProc(b)){
            return;
        }

        TrueDamageAPI.applyCappedMaxHealthTrueDamage(build, trueDamagePercent, trueDamageHealthCap, trueDamageCap);
    }
}
