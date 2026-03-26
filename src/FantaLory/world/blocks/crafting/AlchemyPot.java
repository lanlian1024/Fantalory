package FantaLory.world.blocks.crafting;

import FantaLory.content.FLLiquids;
import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Strings;
import arc.util.Time;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.ui.Bar;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawLiquidTile;
import mindustry.world.draw.DrawMulti;
import mindustry.world.draw.DrawRegion;
import mindustry.world.meta.BlockStatus;

public class AlchemyPot extends GenericCrafter{
    // 输入液体类型（默认水），用于按实际输出量稳定扣除。
    public Liquid inputLiquid = Liquids.water;

    // 初始化炼药锅并绑定顶层建筑实体类。
    public AlchemyPot(String name){
        super(name);
        // 参考原版 electrolyzer：
        // - 底图
        // - 内部液体（10 像素区域 => 2x2 方块 16 像素宽，padding=3）
        // - 主体
        // - 工作输出层（位于主体上一层）
        // - 顶层 heat
        drawer = new DrawMulti(
            new DrawRegion("-bottom"),
            new DrawLiquidTile(Liquids.water, 3f),
            new DrawRegion(),
            new DrawAlchemyOutput(),
            new DrawAlchemyHeat()
        );
        buildType = () -> new AlchemyPotBuild(this);
    }

    @Override
    // 替换默认液体条：分别显示水库存与魔液库存。
    public void setBars(){
        super.setBars();
        removeBar("liquid");
        addBar("water", (AlchemyPotBuild b) -> new Bar(
            () -> "水 " + Strings.autoFixed(b == null || b.liquids == null ? 0f : b.liquids.get(inputLiquid), 1)
                + "/" + Strings.autoFixed(liquidCapacity, 1),
            () -> inputLiquid.color,
            () -> b == null || b.liquids == null ? 0f : Mathf.clamp(b.liquids.get(inputLiquid) / liquidCapacity)
        ));
        addBar("mana-liquid", (AlchemyPotBuild b) -> new Bar(
            () -> "魔液 " + Strings.autoFixed(b == null || b.liquids == null ? 0f : b.liquids.get(FLLiquids.mana), 1)
                + "/" + Strings.autoFixed(liquidCapacity, 1),
            () -> FLLiquids.mana.color,
            () -> b == null || b.liquids == null ? 0f : Mathf.clamp(b.liquids.get(FLLiquids.mana) / liquidCapacity)
        ));
    }
}

// 炼药锅工作输出层：仅工作时显示，绘制在主体之上。
class DrawAlchemyOutput extends DrawBlock{
    private TextureRegion region;
    private float layer = Layer.blockOver;

    @Override
    public void draw(Building build){
        float warm = build.warmup();
        if(warm <= 0.001f || region == null || !region.found()) return;

        float z = Draw.z();
        Draw.z(layer);
        Draw.alpha(Mathf.clamp(warm));
        Draw.rect(region, build.x, build.y);
        Draw.color();
        Draw.alpha(1f);
        Draw.z(z);
    }

    @Override
    public void load(mindustry.world.Block block){
        if(Core.atlas.has(block.name + "-output")){
            region = Core.atlas.find(block.name + "-output");
        }else if(Core.atlas.has(block.name + "-output1")){
            region = Core.atlas.find(block.name + "-output1");
        }else{
            region = Core.atlas.find(block.name + "-output");
        }
    }
}

// 炼药锅热层：仅工作时显示，且处于最顶层。
class DrawAlchemyHeat extends DrawBlock{
    private TextureRegion heatRegion;
    private Color heatColor = new Color(1f, 0.22f, 0.22f, 0.8f);
    private float heatPulse = 0.3f;
    private float heatPulseScl = 10f;
    private float layer = Layer.blockOver + 0.1f;

    @Override
    public void draw(Building build){
        float warm = build.warmup();
        if(warm <= 0.001f || heatRegion == null || !heatRegion.found()) return;

        float z = Draw.z();
        Draw.z(layer);
        Draw.blend(Blending.additive);
        float pulse = 1f - heatPulse + Mathf.absin(Time.time, heatPulseScl, heatPulse);
        float alpha = Mathf.clamp(warm * heatColor.a * pulse, 0f, 1f);
        Draw.color(heatColor, alpha);
        Draw.rect(heatRegion, build.x, build.y);
        Draw.blend();
        Draw.color();
        Draw.z(z);
    }

    @Override
    public void load(mindustry.world.Block block){
        heatRegion = arc.Core.atlas.find(block.name + "-heat");
    }
}

class AlchemyPotBuild extends GenericCrafter.GenericCrafterBuild{
    // 浮点比较阈值，避免极小数误差导致状态抖动。
    private static final float eps = 0.0001f;

    // 绑定外部方块。
    AlchemyPotBuild(AlchemyPot block){
        block.super();
    }

    @Override
    // 主更新：
    // 1) 有输出端时优先直推，并按“实际推送量”扣水；
    // 2) 水不足时用自身魔液库存补齐推送上限；
    // 3) 无输出端时转为向自身库存产液。
    public void updateTile(){
        AlchemyPot pot = (AlchemyPot)block;
        LiquidStack output = mainOutput(pot);
        float target = output == null ? 0f : Math.max(0f, output.amount * edelta());

        float movedTotal = 0f;
        float craftedFromWater = 0f;

        if(output != null && target > eps){
            int outputDir = getOutputDir(pot, 0);
            boolean hasPort = hasOutputPort(output.liquid, outputDir);

            if(hasPort){
                OutputResult result = pushPreferDirect(pot, output.liquid, target, outputDir);
                movedTotal = result.moved;
                craftedFromWater = result.waterUsed;

                // 外送不足或外送失败时，把剩余产能回落到自身库存，避免必须断开水源才自产。
                float remain = target - movedTotal;
                if(remain > eps){
                    OutputResult fallback = storeToSelf(pot, output.liquid, remain);
                    movedTotal += fallback.moved;
                    craftedFromWater += fallback.waterUsed;
                }
            }else{
                OutputResult result = storeToSelf(pot, output.liquid, target);
                movedTotal = result.moved;
                craftedFromWater = result.waterUsed;
            }
        }

        // 仅“由水转化出的魔液”推进配方进度，避免纯库存外放也消耗材料。
        if(craftedFromWater > eps && target > eps){
            float craftActivity = Mathf.clamp(craftedFromWater / target);
            progress += getProgressIncrease(pot.craftTime) * craftActivity;
        }

        // 只要有实际输出动作（直推或自产）就保持热机。
        if(movedTotal > eps){
            warmup = Mathf.approachDelta(warmup, 1f, pot.warmupSpeed);
            if(wasVisible && Mathf.chanceDelta(pot.updateEffectChance)){
                pot.updateEffect.at(
                    x + Mathf.range(pot.size * pot.updateEffectSpread),
                    y + Mathf.range(pot.size * pot.updateEffectSpread)
                );
            }
        }else{
            warmup = Mathf.approachDelta(warmup, 0f, pot.warmupSpeed);
        }

        totalProgress += warmup * Time.delta;

        if(progress >= 1f){
            craft();
        }

        // 即使没有原料，也会持续把库存中的低纯魔液输出。
        dumpOutputs();
    }

    @Override
    // 缺任意核心材料时显示红色状态。
    public BlockStatus status(){
        if(!enabled) return BlockStatus.logicDisable;
        if(isMissingCoreMaterial()) return BlockStatus.noInput;
        return BlockStatus.active;
    }

    @Override
    // 自定义 craft：仅消耗配方材料并触发特效，不重复添加液体产物。
    public void craft(){
        AlchemyPot pot = (AlchemyPot)block;
        consume();
        progress %= 1f;
        if(wasVisible){
            pot.craftEffect.at(x, y);
        }
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
        AlchemyPot pot = (AlchemyPot)block;
        return liquid == pot.inputLiquid && liquids.get(liquid) < block.liquidCapacity - eps;
    }

    // 处理“有输出端”场景：优先直推，优先用水，水不足再用自身库存补齐。
    private OutputResult pushPreferDirect(AlchemyPot pot, Liquid output, float target, int outputDir){
        float waterAvailable = canConsumeCatalyst() ? liquids.get(pot.inputLiquid) : 0f;
        float stockAvailable = liquids.get(output);
        float canSend = Math.min(target, waterAvailable + stockAvailable);
        float moved = canSend > eps ? tryDirectOutput(output, canSend, outputDir) : 0f;
        if(moved <= eps) return OutputResult.none();

        float waterUse = Math.min(moved, waterAvailable);
        if(waterUse > eps){
            liquids.remove(pot.inputLiquid, waterUse);
        }

        float stockUse = moved - waterUse;
        if(stockUse > eps){
            liquids.remove(output, stockUse);
        }

        return new OutputResult(moved, waterUse);
    }

    // 处理“无输出端”场景：把水等比转化为自身库存中的魔液。
    private OutputResult storeToSelf(AlchemyPot pot, Liquid output, float target){
        if(!canConsumeCatalyst()) return OutputResult.none();

        float waterAvailable = liquids.get(pot.inputLiquid);
        float space = block.liquidCapacity - liquids.get(output);
        if(waterAvailable <= eps || space <= eps) return OutputResult.none();

        float produced = Math.min(target, Math.min(waterAvailable, space));
        if(produced <= eps) return OutputResult.none();

        liquids.remove(pot.inputLiquid, produced);
        liquids.add(output, produced);
        return new OutputResult(produced, produced);
    }

    // 获取主输出液体定义。
    private LiquidStack mainOutput(AlchemyPot pot){
        if(pot.outputLiquids == null || pot.outputLiquids.length == 0){
            return null;
        }
        return pot.outputLiquids[0];
    }

    // 获取输出方向配置。
    private int getOutputDir(AlchemyPot pot, int index){
        if(pot.liquidOutputDirections == null || pot.liquidOutputDirections.length <= index){
            return -1;
        }
        return pot.liquidOutputDirections[index];
    }

    // 判断是否存在可作为输出端的邻接建筑。
    private boolean hasOutputPort(Liquid liquid, int outputDir){
        if(outputDir != -1){
            int real = Mathf.mod(rotation + outputDir, 4);
            return isValidOutputTarget(nearby(real), liquid);
        }

        if(proximity == null || proximity.isEmpty()) return false;
        for(int i = 0; i < proximity.size; i++){
            if(isValidOutputTarget(proximity.get(i), liquid)){
                return true;
            }
        }
        return false;
    }

    // 校验目标是否可作为液体输出端。
    private boolean isValidOutputTarget(Building next, Liquid liquid){
        if(next == null) return false;
        next = next.getLiquidDestination(this, liquid);
        if(next == null || next == this) return false;
        if(!next.block.hasLiquids || next.liquids == null) return false;
        return canDumpLiquid(next, liquid);
    }

    // 判断关键材料是否缺失（用于红色状态）。
    private boolean isMissingCoreMaterial(){
        AlchemyPot pot = (AlchemyPot)block;
        boolean noWater = liquids.get(pot.inputLiquid) <= eps;
        boolean noItem = !canConsumeCatalyst();
        return noWater || noItem;
    }

    // 本锅配方的催化材料（魔石）是否可用于转化。
    private boolean canConsumeCatalyst(){
        return items != null && items.total() > 0;
    }

    // 按指定方向或自动轮询方式输出液体。
    private float tryDirectOutput(Liquid liquid, float amount, int outputDir){
        if(amount <= eps) return 0f;

        if(outputDir != -1){
            int real = Mathf.mod(rotation + outputDir, 4);
            return moveToTarget(nearby(real), liquid, amount);
        }

        if(proximity == null || proximity.isEmpty()) return 0f;

        float moved = 0f;
        int dump = cdump;
        for(int i = 0; i < proximity.size; i++){
            int idx = (i + dump) % proximity.size;
            Building next = proximity.get(idx);
            moved += moveToTarget(next, liquid, amount - moved);
            if(moved + eps >= amount){
                cdump = idx;
                break;
            }
        }
        return moved;
    }

    // 向单个目标建筑转移液体。
    private float moveToTarget(Building next, Liquid liquid, float amount){
        if(next == null || amount <= eps) return 0f;

        next = next.getLiquidDestination(this, liquid);
        if(next == null || next == this) return 0f;
        if(!next.block.hasLiquids || next.liquids == null) return 0f;
        if(!canDumpLiquid(next, liquid)) return 0f;
        if(!next.acceptLiquid(this, liquid)) return 0f;

        float space = next.block.liquidCapacity - next.liquids.get(liquid);
        if(space <= eps) return 0f;

        float flow = Math.min(space, amount);
        next.handleLiquid(this, liquid, flow);
        return flow;
    }

    // 输出阶段的统计结果：moved 为总输出，waterUsed 为由水转化的份额。
    private static final class OutputResult{
        final float moved;
        final float waterUsed;

        OutputResult(float moved, float waterUsed){
            this.moved = moved;
            this.waterUsed = waterUsed;
        }

        static OutputResult none(){
            return new OutputResult(0f, 0f);
        }
    }
}
