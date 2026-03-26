package FantaLory.world.blocks.mana;

import FantaLory.content.FLLiquids;
import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Strings;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValues;

public class ManaCrafter extends GenericCrafter{
    // 每帧魔力消耗量。
    public float manaUse = 50f;
    // 魔力容量上限。
    public float manaCapacity = 200f;
    // 初始魔力（负数表示按容量初始化）。
    public float initialMana = -1f;
    // 魔力条颜色。
    public Color manaBarColor = Color.valueOf("8fe6ff");
    // 工作时是否绘制发热贴图。
    public boolean showWorkingHeat = false;
    // 发热贴图后缀，默认读取 blockName-heat。
    public String heatRegionSuffix = "-heat";
    // 发热贴图颜色与脉冲参数（参考原版热输入视觉）。
    public Color heatColor = new Color(1f, 0.22f, 0.22f, 0.8f);
    public float heatPulse = 0.3f;
    public float heatPulseScl = 10f;
    // 工作时是否绘制中心白烟。
    public boolean showWorkingSmoke = false;
    // 白烟直径（像素）。
    public float smokeSizePx = 5f;
    // 白烟上升高度（像素）。
    public float smokeRisePx = 10f;
    // 白烟透明度上限。
    public float smokeAlpha = 0.45f;

    // 发热贴图缓存。
    public TextureRegion heatRegion;

    // 初始化方块并绑定顶层建筑实体类。
    public ManaCrafter(String name){
        super(name);
        hasPower = false;
        buildType = () -> new ManaCrafterBuilding(this);
    }

    @Override
    // 预载可选发热贴图。
    public void load(){
        super.load();
        if(showWorkingHeat && Core.atlas.has(name + heatRegionSuffix)){
            heatRegion = Core.atlas.find(name + heatRegionSuffix);
        }else{
            heatRegion = null;
        }
    }

    @Override
    // 注册魔力库存条。
    public void setBars(){
        super.setBars();
        addBar("mana", (ManaCrafterBuilding b) -> new Bar(
            () -> "魔力 " + Strings.autoFixed(b.tile == null ? 0f : ManaState.get(b.tile), 1)
                + "/" + Strings.autoFixed(b.tile == null ? manaCapacity : ManaState.capacity(b.tile), 1),
            () -> manaBarColor,
            () -> ManaState.fraction(b.tile)
        ));
    }

    @Override
    // 注册耗魔信息：工厂按每秒显示。
    public void setStats(){
        super.setStats();
        if(manaUse > 0.0001f){
            stats.add(Stat.input, StatValues.string("[#8fe6ff]魔力[]消耗：@/秒", Strings.autoFixed(manaUse * 60f, 2)));
        }
    }
}

class ManaCrafterBuilding extends GenericCrafter.GenericCrafterBuild{
    // 绑定外部方块。
    ManaCrafterBuilding(ManaCrafter block){
        block.super();
    }

    @Override
    // 在原版绘制后叠加“发热贴图 + 中心白烟”工作特效。
    public void draw(){
        super.draw();
        ManaCrafter crafter = (ManaCrafter)block;
        float active = warmup();
        if(active <= 0.001f) return;

        drawWorkingHeat(crafter, active);
        drawWorkingSmoke(crafter, active);
    }

    // 发热贴图：工作越充分越明显，带轻微脉冲。
    private void drawWorkingHeat(ManaCrafter crafter, float active){
        if(!crafter.showWorkingHeat || crafter.heatRegion == null) return;

        Draw.z(Layer.blockAdditive);
        Draw.blend(Blending.additive);
        float pulse = 1f - crafter.heatPulse + Mathf.absin(Time.time, crafter.heatPulseScl, crafter.heatPulse);
        float alpha = Mathf.clamp(active * crafter.heatColor.a * pulse, 0f, 1f);
        Draw.color(crafter.heatColor, alpha);
        Draw.rect(crafter.heatRegion, x, y);
        Draw.blend();
        Draw.color();
        Draw.z(Layer.block);
    }

    // 中心白烟：以 5 像素直径为基准，持续向上漂移。
    private void drawWorkingSmoke(ManaCrafter crafter, float active){
        if(!crafter.showWorkingSmoke) return;

        Draw.z(Layer.effect);
        float baseRadius = Math.max(0.5f, crafter.smokeSizePx * 0.5f);

        // 两团错相白烟，提升连续感。
        for(int i = 0; i < 2; i++){
            float t = (Time.time / 36f + i * 0.5f + (id % 7) * 0.03f) % 1f;
            float yOff = t * crafter.smokeRisePx;
            float xOff = Mathf.sin((Time.time + i * 17f + id) / 20f) * 0.8f;
            float radius = baseRadius * (0.85f + 0.25f * t);
            float alpha = Mathf.clamp((1f - t) * crafter.smokeAlpha * active, 0f, 1f);

            Draw.color(Color.white, alpha);
            Fill.circle(x + xOff, y + yOff, radius);
        }
        Draw.reset();
    }

    @Override
    // 仅接收低纯魔液。
    public boolean acceptLiquid(Building source, Liquid liquid){
        if(liquid == FLLiquids.mana){
            return liquids != null && liquids.get(liquid) < block.liquidCapacity - 0.0001f;
        }
        return super.acceptLiquid(source, liquid);
    }

    @Override
    // 放置时初始化魔力缓存。
    public void placed(){
        super.placed();
        ManaCrafter crafter = (ManaCrafter)block;
        float init = crafter.initialMana < 0f ? crafter.manaCapacity : crafter.initialMana;
        ManaState.init(tile, crafter.manaCapacity, init);
    }

    @Override
    // 拆除时清理魔力缓存。
    public void onRemoved(){
        ManaState.remove(tile);
        super.onRemoved();
    }

    @Override
    // 主更新：液体转魔力并按需求消耗魔力。
    public void updateTile(){
        ManaCrafter crafter = (ManaCrafter)block;
        ManaState.fillFromLiquid(this, FLLiquids.mana);
        if(efficiency > 0f && shouldConsume()){
            float amount = crafter.manaUse * Time.delta;
            if(!ManaState.consume(tile, amount)){
                efficiency = 0f;
            }
        }
        super.updateTile();
    }
}
