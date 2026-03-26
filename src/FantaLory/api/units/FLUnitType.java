package FantaLory.api.units;

import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;

public class FLUnitType extends UnitType{
    // 职业 ID 常量，便于科技树和后续注册统一判断。
    public static final String careerWarrior = "warrior";
    public static final String careerTank = "tank";
    public static final String careerMage = "mage";
    public static final String careerHealer = "healer";
    public static final String careerSpecial = "special";

    // 当前单位职业信息。
    public final String careerId;
    public final String careerName;

    // 基础构造：创建带职业标签的模组单位类型。
    public FLUnitType(String name, String careerId, String careerName){
        super(name);
        this.careerId = careerId;
        this.careerName = careerName;
        // 核心数据库按职业分组显示，替代原版 air/ground/naval 标签。
        databaseTag = "fantalory-career-" + careerId;
        // 给职业展示节点提供默认实体构造器，避免 155.1 在生成图标时崩溃。
        constructor = UnitEntity::create;
    }
}
