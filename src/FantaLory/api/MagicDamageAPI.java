package FantaLory.api;

import arc.struct.ObjectMap;
import mindustry.gen.Building;
import mindustry.gen.Healthc;
import mindustry.gen.Unit;

public class MagicDamageAPI{
    /*
     * 法术抗性注册表。
     * key: 任意可受伤目标（单位/建筑，统一按 Healthc 管理）
     * value: 法术抗性百分比，10 表示减伤 10%
     */
    private static final ObjectMap<Healthc, Float> spellResistanceMap = new ObjectMap<>();

    /*
     * 计算最终法术伤害。
     * 规则：
     * 1) 不参与原版护甲与默认伤害公式,穿透原版无敌buff扣血
     * 2) 仅按法术抗性做线性减伤
     * 3) 抗性会被钳制到 [0, 100],10对应10%伤害减免
     *
     * 公式：
     * finalDamage = baseDamage * (1 - resistance/100)
     */
    public static float calculateMagicDamage(float baseDamage, Healthc target){
        if(baseDamage <= 0f){
            return 0f;
        }
        if(target == null){
            return baseDamage;
        }

        float resistance = spellResistanceMap.get(target, 0f);
        resistance = clampResistance(resistance);

        float factor = 1f - resistance / 100f;
        if(factor < 0f) factor = 0f;
        return baseDamage * factor;
    }

    /*
     * 施加法术伤害。
     * - 单位使用 rawDamage，尽量绕开原版常规伤害处理链
     * - 建筑使用 damage
     */
    public static void applyMagicDamage(float baseDamage, Healthc target){
        if(target == null) return;

        float finalDamage = calculateMagicDamage(baseDamage, target);
        if(finalDamage <= 0f) return;

        if(target instanceof Unit unit){
            unit.rawDamage(finalDamage);
        }else{
            target.damage(finalDamage);
        }
    }

    // 兼容旧接口：单位法术伤害计算
    public static float calculateMagicDamage(float baseDamage, Unit target){
        return calculateMagicDamage(baseDamage, (Healthc)target);
    }

    // 兼容旧接口：单位法术伤害应用
    public static void applyMagicDamage(float baseDamage, Unit target){
        applyMagicDamage(baseDamage, (Healthc)target);
    }

    // 读取单位法术抗性
    public static float getUnitResistance(Unit unit){
        if(unit == null) return 0f;
        return spellResistanceMap.get(unit, 0f);
    }

    // 设置单位法术抗性（百分比）
    public static void setUnitResistance(Unit unit, float resistance){
        if(unit == null) return;
        spellResistanceMap.put(unit, resistance);
    }

    // 清理单位法术抗性
    public static void clearUnitResistance(Unit unit){
        if(unit == null) return;
        spellResistanceMap.remove(unit);
    }

    // 读取建筑法术抗性
    public static float getBuildingResistance(Building building){
        if(building == null) return 0f;
        return spellResistanceMap.get(building, 0f);
    }

    // 设置建筑法术抗性（百分比）
    public static void setBuildingResistance(Building building, float resistance){
        if(building == null) return;
        spellResistanceMap.put(building, resistance);
    }

    // 清理建筑法术抗性
    public static void clearBuildingResistance(Building building){
        if(building == null) return;
        spellResistanceMap.remove(building);
    }

    // 抗性钳制到 0~100，防止越界导致负伤害或反向治疗。
    private static float clampResistance(float resistance){
        if(resistance < 0f) return 0f;
        if(resistance > 100f) return 100f;
        return resistance;
    }
}
