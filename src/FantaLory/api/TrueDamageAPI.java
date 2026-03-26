package FantaLory.api;

import mindustry.gen.Healthc;
import mindustry.gen.Unit;

public class TrueDamageAPI{
    private TrueDamageAPI(){
    }

    // 计算“按最大生命值百分比”的真实伤害，并限制上限。
    public static float calculateCappedMaxHealthTrueDamage(Healthc target, float maxHealthPercent, float percentHealthCap, float damageCap){
        if(target == null || maxHealthPercent <= 0f || damageCap <= 0f){
            return 0f;
        }

        float maxHealth = target.maxHealth();
        if(maxHealth <= 0f){
            return 0f;
        }

        // 超过阈值生命值时，不再按百分比继续增长，直接取伤害上限。
        if(maxHealth > percentHealthCap){
            return damageCap;
        }

        return Math.min(maxHealth * maxHealthPercent, damageCap);
    }

    // 施加真实伤害：独立机制，直接扣除生命值，不参与法术抗性与护甲计算。
    public static void applyTrueDamage(float trueDamage, Healthc target){
        if(target == null || trueDamage <= 0f || target.dead()){
            return;
        }

        if(target instanceof Unit unit && unit.type != null && !unit.type.killable){
            return;
        }

        float remainingHealth = target.health() - trueDamage;
        target.health(remainingHealth);
        target.hitTime(1f);

        if(remainingHealth <= 0f && !target.dead()){
            target.kill();
        }
    }

    // 便捷接口：按“最大生命值百分比+封顶”规则计算后立刻施加真实伤害。
    public static void applyCappedMaxHealthTrueDamage(Healthc target, float maxHealthPercent, float percentHealthCap, float damageCap){
        float trueDamage = calculateCappedMaxHealthTrueDamage(target, maxHealthPercent, percentHealthCap, damageCap);
        applyTrueDamage(trueDamage, target);
    }
}
