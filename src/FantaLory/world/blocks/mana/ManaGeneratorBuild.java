package FantaLory.world.blocks.mana;

import FantaLory.content.FLLiquids;
import FantaLory.content.items.FLItems;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import mindustry.world.blocks.production.GenericCrafter;

class ManaGeneratorBuild extends GenericCrafter.GenericCrafterBuild{
    // 工作态中心光效颜色（淡蓝）。
    private static final Color coreFxColor = Color.valueOf("bfefff");

    // 投影热度，用于插值控制亮度。
    float heat;
    // 秘银消耗进度。
    float boostMithrilProgress;
    // 是否正在应用秘银增强。
    boolean boostActive;
    // 魔力净流速（每秒）。
    float manaFlowPerSecond;
    // 中心光效热度（0~1），用于平滑显隐。
    float coreFxHeat;

    // 绑定外部方块实例。
    ManaGeneratorBuild(ManaGenerator block){
        block.super();
    }

    // 便捷获取外部方块配置。
    private ManaGenerator gen(){
        return (ManaGenerator)block;
    }

    @Override
    // 仅接收魔力系液体。
    public boolean acceptLiquid(Building source, Liquid liquid){
        if(!FLLiquids.isManaSeries(liquid)) return false;
        return liquids != null && liquids.get(liquid) < block.liquidCapacity - 0.0001f;
    }

    @Override
    // 仅接收秘银作为范围增强材料。
    public boolean acceptItem(Building source, Item item){
        if(item == FLItems.mithril){
            return items != null && items.get(item) < block.itemCapacity;
        }
        return super.acceptItem(source, item);
    }

    @Override
    // 放置后初始化魔力缓存。
    public void placed(){
        super.placed();
        ManaGenerator generator = gen();
        ManaState.init(tile, generator.manaCapacity, generator.initialMana);
    }

    @Override
    // 拆除时清理魔力缓存。
    public void onRemoved(){
        ManaState.remove(tile);
        super.onRemoved();
    }

    @Override
    // 绘制本体后叠加投影填充和边框。
    public void draw(){
        super.draw();
        drawCoreWorkEffect();
        drawRangeFill();
        drawRangeBorder();
    }

    // 工作态中心特效：
    // - 中心 9 像素半透明淡蓝圆（半径 4.5）
    // - 单圆环从中心向当前最大投影范围扩散（线宽 4 像素）
    private void drawCoreWorkEffect(){
        if(coreFxHeat <= 0.001f) return;

        float maxRadius = getVisualRange() * Vars.tilesize;
        if(maxRadius <= 5f) return;

        float centerRadius = 4.5f;
        float travel = Math.max(1f, maxRadius - centerRadius);
        float ringSpeed = 4f; // 像素/刻，偏快扩散
        float ringRadius = centerRadius + Mathf.mod(Time.time * ringSpeed, travel);

        float pulse = Mathf.absin(Time.time, 8f, 0.08f) * coreFxHeat;
        // 在现有效果基础上整体提高 20% 亮度/可见度。
        float visibilityScale = 1.2f;
        float centerAlpha = Mathf.clamp((0.16f + pulse) * coreFxHeat * 0.85f * visibilityScale, 0f, 0.54f);
        float ringAlpha = Mathf.clamp((0.2f + pulse * 0.7f) * coreFxHeat * 0.85f * visibilityScale, 0f, 0.6f);

        Draw.z(Layer.effect + 0.1f);
        Draw.color(coreFxColor);

        Draw.alpha(centerAlpha);
        Fill.circle(x, y, centerRadius);

        Draw.alpha(ringAlpha);
        Lines.stroke(4f);
        Lines.circle(x, y, ringRadius);

        Draw.reset();
    }

    // 计算当前投影亮度目标值。
    private float projectedWarmup(){
        if(!hasPowerConnection()) return 0f;
        return isProjectionBright() ? 1f : 0.16f;
    }

    // 绘制投影范围填充。
    private void drawRangeFill(){
        float r = getVisualRange() * Vars.tilesize;
        if(r <= 0.001f) return;
        float activeWarmup = projectedWarmup();
        heat = Mathf.lerpDelta(heat, activeWarmup, 0.08f);
        float pulse = activeWarmup > 0.001f ? Mathf.absin(Time.time, 50f, 0.16f) : 0f;
        float a = Mathf.clamp(0.08f + heat * 0.44f + pulse, 0f, 0.9f);
        Draw.z(Layer.shields);
        Draw.color(gen().rangeColor);
        Draw.alpha(a);
        Fill.circle(x, y, r);
        Draw.reset();
    }

    // 绘制投影范围边框，并在重叠区域隐藏边线。
    private void drawRangeBorder(){
        float r = getVisualRange() * Vars.tilesize;
        if(r <= 0.001f) return;
        Seq<ManaRangeCircle> others = collectOtherRanges(r);

        Draw.z(Layer.shields);
        Draw.color(gen().rangeColor);
        Draw.alpha(0.16f + heat * 0.74f);
        Lines.stroke(0.9f + heat * 0.3f);
        int segments = 72;
        float step = 360f / segments;
        for(int i = 0; i < segments; i++){
            float a1 = i * step;
            float a2 = (i + 1) * step;
            float mid = (a1 + a2) * 0.5f;
            float px = x + Angles.trnsx(mid, r);
            float py = y + Angles.trnsy(mid, r);
            if(isOverlapped(px, py, others)) continue;
            float x1 = x + Angles.trnsx(a1, r);
            float y1 = y + Angles.trnsy(a1, r);
            float x2 = x + Angles.trnsx(a2, r);
            float y2 = y + Angles.trnsy(a2, r);
            Lines.line(x1, y1, x2, y2);
        }
        Draw.reset();
    }

    // 收集附近可与当前范围重叠的其他投影圈。
    private Seq<ManaRangeCircle> collectOtherRanges(float selfRange){
        Seq<ManaRangeCircle> result = new Seq<>();
        float maxOtherRange = Math.max(getVisualRange(), 6f) * Vars.tilesize;
        int tileRange = Mathf.ceil((selfRange + maxOtherRange) / Vars.tilesize) + 1;
        IntSet seen = new IntSet();
        for(int dx = -tileRange; dx <= tileRange; dx++){
            for(int dy = -tileRange; dy <= tileRange; dy++){
                Tile otherTile = tile.nearby(dx, dy);
                if(otherTile == null) continue;
                Building other = otherTile.build;
                if(other == null || other == this) continue;
                if(other.tile == null) continue;
                if(!seen.add(other.tile.pos())) continue;
                float r = otherRange(other);
                if(r <= 0f) continue;
                if(Mathf.within(x, y, other.x, other.y, selfRange + r + Vars.tilesize)){
                    result.add(new ManaRangeCircle(other.x, other.y, r));
                }
            }
        }
        return result;
    }

    // 判断边框采样点是否落在其他投影圈内。
    private boolean isOverlapped(float px, float py, Seq<ManaRangeCircle> others){
        for(int i = 0; i < others.size; i++){
            ManaRangeCircle other = others.get(i);
            if(other == null) continue;
            if(Mathf.within(px, py, other.x, other.y, other.r - 0.1f)) return true;
        }
        return false;
    }

    // 计算其他建筑对应的有效投影半径。
    private float otherRange(Building other){
        if(other instanceof ManaGeneratorBuild){
            return ((ManaGeneratorBuild)other).getVisualRange() * Vars.tilesize;
        }
        if(other.block instanceof ManaGenerator){
            return ((ManaGenerator)other.block).manaRange * Vars.tilesize;
        }
        if(other.block.name != null && other.block.name.endsWith("mana-source")){
            return 6f * Vars.tilesize;
        }
        return 0f;
    }

    @Override
    // 选中建筑时绘制范围与可供能目标高亮。
    public void drawSelect(){
        super.drawSelect();
        float r = getVisualRange() * Vars.tilesize;
        Drawf.dashCircle(x, y, r, gen().rangeColor);
        Vars.indexer.eachBlock(this, r,
            other -> ManaState.capacity(other.tile) > 0f,
            other -> Drawf.selected(other, Tmp.c1.set(gen().rangeColor).a(Mathf.absin(4f, 1f)))
        );
    }

    @Override
    // 主更新：转化魔液、分发魔力、更新流速显示。
    public void updateTile(){
        float beforeMana = tile == null ? 0f : ManaState.get(tile);
        convertLiquidToMana();
        boolean running = efficiency > 0f && canRunByManaState();
        // 工作态判定：有供电、可运行、且当前存在充能/分发需求。
        boolean coreWorking = running && hasPowerSupply() && needsPowerConsumption();
        coreFxHeat = Mathf.approachDelta(coreFxHeat, coreWorking ? 1f : 0f, 0.05f);
        boostActive = updateBoostState(running);
        if(running){
            float send = Math.min(ManaState.get(tile), gen().manaProduction * edelta());
            if(send > 0f){
                float sent = distributeMana(send, getCurrentRange());
                if(sent > 0f){
                    ManaState.consume(tile, sent);
                }
            }
        }
        super.updateTile();

        float afterMana = tile == null ? 0f : ManaState.get(tile);
        float dt = Math.max(Time.delta, 0.0001f);
        float targetFlow = (afterMana - beforeMana) * 60f / dt;
        manaFlowPerSecond = Mathf.lerpDelta(manaFlowPerSecond, targetFlow, 0.2f);
    }

    @Override
    // 仅在可运行且有需求时才执行原版消耗流程。
    public boolean shouldConsume(){
        return super.shouldConsume() && canRunByManaState() && needsPowerConsumption();
    }

    // 将魔力系液体按比例转化为魔力。
    private void convertLiquidToMana(){
        if(liquids == null) return;
        Liquid sourceLiquid = pickConvertibleLiquid();
        if(sourceLiquid == null) return;

        float available = liquids.get(sourceLiquid);
        if(available <= 0.0001f) return;

        float space = Math.max(0f, ManaState.capacity(tile) - ManaState.get(tile));
        if(space <= 0.0001f) return;

        float ratio = ManaState.liquidToManaRatio();
        float maxLiquid = gen().manaLiquidUse * edelta();
        float maxLiquidBySpace = space / ratio;
        float usedLiquid = Math.min(Math.min(available, maxLiquid), maxLiquidBySpace);
        if(usedLiquid <= 0.0001f) return;

        float convertedMana = usedLiquid * ratio;
        liquids.remove(sourceLiquid, usedLiquid);
        ManaState.add(tile, convertedMana);
    }

    // 是否存在可转化的魔力系液体。
    private boolean hasConvertibleLiquid(){
        return pickConvertibleLiquid() != null;
    }

    // 当前状态下是否可运行。
    private boolean canRunByManaState(){
        return tile != null && (ManaState.get(tile) > 0.0001f || hasConvertibleLiquid());
    }

    // 是否需要耗电（充能或对外供能）。
    private boolean needsPowerConsumption(){
        if(tile == null) return false;
        if(needsChargeByLiquid()) return true;
        return hasManaDemandTargets(getDemandCheckRange()) && ManaState.get(tile) > 0.0001f;
    }

    // 是否需要通过魔液继续充满自身缓存。
    private boolean needsChargeByLiquid(){
        if(tile == null) return false;
        if(!hasConvertibleLiquid()) return false;
        return ManaState.get(tile) + 0.0001f < ManaState.capacity(tile);
    }

    // 计算需求扫描范围（包含增强范围）。
    private int getDemandCheckRange(){
        int extra = items != null && items.get(FLItems.mithril) > 0 ? gen().boostRangeBonus : 0;
        return gen().manaRange + Math.max(extra, 0);
    }

    // 检查范围内是否存在缺魔力目标。
    private boolean hasManaDemandTargets(int range){
        if(tile == null || range <= 0) return false;
        for(int dx = -range; dx <= range; dx++){
            for(int dy = -range; dy <= range; dy++){
                Tile other = tile.nearby(dx, dy);
                if(!isManaTransferTarget(other)) continue;
                float cap = ManaState.capacity(other);
                if(cap <= 0.0001f) continue;
                if(ManaState.get(other) + 0.0001f < cap){
                    return true;
                }
            }
        }
        return false;
    }

    // 过滤可接收魔力分发的建筑。
    private boolean isManaTransferTarget(Tile other){
        if(other == null || other == tile) return false;
        Building build = other.build;
        if(build == null || build.block == null) return false;
        if(ManaState.capacity(other) <= 0.0001f) return false;
        if(build.block instanceof ManaGenerator) return false;

        String name = build.block.name;
        if(name != null){
            if(name.endsWith("mana-source") || name.endsWith("mana-void")){
                return false;
            }
        }
        return true;
    }

    // 选择当前可用的魔力系液体。
    private Liquid pickConvertibleLiquid(){
        if(liquids == null) return null;

        Liquid current = liquids.current();
        if(current != null && FLLiquids.isManaSeries(current) && liquids.get(current) > 0.0001f){
            return current;
        }

        final Liquid[] found = new Liquid[1];
        liquids.each((liquid, amount) -> {
            if(found[0] == null && amount > 0.0001f && FLLiquids.isManaSeries(liquid)){
                found[0] = liquid;
            }
        });
        return found[0];
    }

    // 更新秘银增强消耗状态。
    private boolean updateBoostState(boolean running){
        if(!running || items == null || items.get(FLItems.mithril) <= 0){
            boostMithrilProgress = 0f;
            return false;
        }

        boostMithrilProgress += gen().boostMithrilUsePerSecond / 60f * edelta();
        if(boostMithrilProgress >= 1f){
            int use = Math.min((int)boostMithrilProgress, items.get(FLItems.mithril));
            if(use > 0){
                items.remove(FLItems.mithril, use);
                boostMithrilProgress -= use;
            }
        }
        return true;
    }

    // 当前真实分发范围（受增强影响）。
    private int getCurrentRange(){
        return gen().manaRange + (boostActive ? gen().boostRangeBonus : 0);
    }

    // 当前可视投影范围（有秘银库存也显示增强圈）。
    int getVisualRange(){
        return gen().manaRange + (hasBoostVisual() ? gen().boostRangeBonus : 0);
    }

    // 是否显示增强视觉。
    private boolean hasBoostVisual(){
        return boostActive || (items != null && items.get(FLItems.mithril) > 0);
    }

    // 是否接入电网。
    private boolean hasPowerConnection(){
        return power != null && power.links != null && power.links.size > 0;
    }

    // 是否有可用电力。
    private boolean hasPowerSupply(){
        return power != null && power.status > 0.0001f;
    }

    // 投影是否处于高亮状态。
    private boolean isProjectionBright(){
        if(tile == null) return false;
        return hasPowerSupply() && ManaState.get(tile) > 0.0001f;
    }

    // 将当前可用魔力平均分发给范围内目标。
    private float distributeMana(float amount, int range){
        if(amount <= 0f) return 0f;
        Seq<Tile> targets = new Seq<>();
        int count = 0;
        for(int dx = -range; dx <= range; dx++){
            for(int dy = -range; dy <= range; dy++){
                Tile other = tile.nearby(dx, dy);
                if(!isManaTransferTarget(other)) continue;
                float cap = ManaState.capacity(other);
                if(cap <= 0.0001f) continue;
                if(ManaState.get(other) + 0.0001f >= cap) continue;
                targets.add(other);
                count++;
            }
        }
        if(count <= 0) return 0f;
        float per = amount / count;
        float sent = 0f;
        for(int i = 0; i < targets.size; i++){
            Tile other = targets.get(i);
            float before = ManaState.get(other);
            ManaState.add(other, per);
            sent += Math.max(0f, ManaState.get(other) - before);
        }
        return sent;
    }

    // 将魔力流速换算为条形图比例。
    float manaFlowFrac(){
        float max = Math.max(gen().manaProduction * 60f, 1f);
        return Mathf.clamp(Math.abs(manaFlowPerSecond) / max);
    }
}

// 投影圈采样数据结构。
final class ManaRangeCircle{
    final float x;
    final float y;
    final float r;

    ManaRangeCircle(float x, float y, float r){
        this.x = x;
        this.y = y;
        this.r = r;
    }
}
