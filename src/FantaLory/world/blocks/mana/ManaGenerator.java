package FantaLory.world.blocks.mana;

import FantaLory.content.items.FLItems;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Strings;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.ItemStack;
import mindustry.ui.Bar;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValues;

public class ManaGenerator extends GenericCrafter{
    // 每帧基础魔力产出（内部按帧计算）。
    public float manaProduction = 50f;
    // 每帧可转化的魔液消耗量（内部按帧计算）。
    public float manaLiquidUse = 50f;
    // 建筑魔力容量上限。
    public float manaCapacity = 2000f;
    // 建筑初始魔力。
    public float initialMana = 0f;
    // 基础投影范围（格）。
    public int manaRange = 18;
    // 秘银增强附加范围（格）。
    public int boostRangeBonus = 8;
    // 秘银每秒消耗量。
    public float boostMithrilUsePerSecond = 0.2f;
    // 魔力条颜色。
    public Color manaBarColor = Color.valueOf("8fe6ff");
    // 投影范围颜色。
    public Color rangeColor = Color.valueOf("8fe6ff");

    // 初始化方块并绑定对应建筑实体。
    public ManaGenerator(String name){
        super(name);
        buildType = () -> new ManaGeneratorBuild(this);
    }

    @Override
    // 注册魔力流速条与魔力库存条。
    public void setBars(){
        super.setBars();
        addBar("mana-flow", (ManaGeneratorBuild b) -> new Bar(
            () -> {
                float flow = b.manaFlowPerSecond;
                String sign = flow >= 0f ? "+" : "-";
                return "魔力" + sign + Strings.autoFixed(Math.abs(flow), 0) + " 能量/秒";
            },
            () -> b.manaFlowPerSecond >= 0f ? manaBarColor : Pal.remove,
            b::manaFlowFrac
        ));
        addBar("mana", (ManaGeneratorBuild b) -> new Bar(
            () -> "魔力 " + Strings.autoFixed(b.tile == null ? 0f : ManaState.get(b.tile), 1)
                + "/" + Strings.autoFixed(b.tile == null ? manaCapacity : ManaState.capacity(b.tile), 1),
            () -> manaBarColor,
            () -> ManaState.fraction(b.tile)
        ));
    }

    @Override
    // 注册方块统计信息（产出与增强）。
    public void setStats(){
        float boosterPeriod = boostMithrilUsePerSecond > 0.0001f ? 60f / boostMithrilUsePerSecond : -1f;
        if(boosterPeriod > 0f){
            stats.timePeriod = boosterPeriod;
        }
        super.setStats();
        stats.add(Stat.output, "基础[#8fe6ff]魔力[]输出 "
            + Strings.autoFixed(manaProduction * 60f, 2) + "[#8fe6ff]魔力[]/秒");
        if(boostRangeBonus > 0 && boosterPeriod > 0f){
            stats.remove(Stat.booster);
            stats.add(Stat.booster, StatValues.itemBoosters(
                "",
                boosterPeriod,
                0f,
                boostRangeBonus * Vars.tilesize,
                ItemStack.with(FLItems.mithril, 1)
            ));
        }
    }

    @Override
    // 放置预览时绘制范围虚线圈与可供能建筑高亮。
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        float wx = x * Vars.tilesize + offset;
        float wy = y * Vars.tilesize + offset;
        float r = manaRange * Vars.tilesize;
        Drawf.dashCircle(wx, wy, r, rangeColor);
        Vars.indexer.eachBlock(Vars.player.team(), wx, wy, r,
            other -> ManaState.capacity(other.tile) > 0f,
            other -> Drawf.selected(other, Tmp.c1.set(rangeColor).a(Mathf.absin(4f, 1f)))
        );
    }
}
