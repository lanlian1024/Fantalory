package FantaLory.content;

import arc.graphics.Color;
import arc.struct.ObjectSet;
import mindustry.type.Liquid;

public class FLLiquids{
    // 低纯魔液主液体对象（也是当前魔力系统输入液体）。
    public static Liquid mana;
    // 魔力系液体集合：用于发生器/工厂判断“可转化液体”。
    private static final ObjectSet<Liquid> manaSeries = new ObjectSet<>();

    // 注册液体内容。
    public static void load(){
        mana = new Liquid("mana"){{
            localizedName = "低纯魔液";
            color = Color.valueOf("8fe6ff");
            barColor = color;
            lightColor = Color.valueOf("8fe6ff").a(0.45f);
            heatCapacity = 0.8f;
            viscosity = 0.5f;
            explosiveness = 0f;
            flammability = 0f;
            coolant = false;
        }};
        registerManaSeries(mana);
    }

    // 手动注册“魔力系液体”（给后续高浓度液体扩展预留）。
    public static void registerManaSeries(Liquid liquid){
        if(liquid != null){
            manaSeries.add(liquid);
        }
    }

    /*
     * 判断某液体是否属于魔力系。
     * 优先判定注册表，其次按名称规则兜底，避免漏判：
     * - mana
     * - mana-xxx
     * - xxx-mana
     */
    public static boolean isManaSeries(Liquid liquid){
        if(liquid == null) return false;
        if(manaSeries.contains(liquid)) return true;
        if(liquid.name == null) return false;
        return liquid.name.equals("mana")
            || liquid.name.startsWith("mana-")
            || liquid.name.endsWith("-mana");
    }
}
