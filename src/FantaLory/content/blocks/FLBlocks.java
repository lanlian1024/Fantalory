package FantaLory.content.blocks;

import FantaLory.content.items.FLItems;
import FantaLory.world.blocks.crafting.AlchemyPot;
import FantaLory.world.blocks.mana.ManaCrafter;
import FantaLory.world.blocks.mana.ManaGenerator;
import FantaLory.content.planets.FLPlanets;
import arc.graphics.Color;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.UnitTypes;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import FantaLory.content.FLLiquids;

/**
 * 方块注册
 */
public class FLBlocks{
    public static Block fantaFurnace;
    public static Block alchemyPot;
    public static Block manaGenerator;
    public static Block fantaCore;

    public static void load(){
        // 幻想炉：消耗原料并消耗魔力进行冶炼。
        fantaFurnace = new ManaCrafter("fantafurnace"){{
            localizedName = "幻想炉";
            description = "用于冶炼秘银的特殊熔炉。";
            requirements(Category.crafting, ItemStack.with(
                Items.copper, 80,
                Items.lead, 60
            ));
            category = Category.crafting;
            size = 2;
            health = 200;
            ambientSoundVolume = 0.04f;

            craftTime = 10f;
            consumeItems(new ItemStack(Items.copper, 2), new ItemStack(Items.lead, 1));
            outputItem = new ItemStack(FLItems.mithril, 1);

            hasPower = false;
            hasItems = true;
            hasLiquids = true;
            liquidCapacity = 120f;
            emitLight = true;
            lightRadius = 6f;
            lightColor = Color.valueOf("B0C4DE");

            // 工作特效：发热贴图 + 中心白烟。
            showWorkingHeat = true;
            heatRegionSuffix = "-heat";
            heatColor = new Color(1f, 0.22f, 0.22f, 0.8f);
            showWorkingSmoke = true;
            smokeSizePx = 5f;
            smokeRisePx = 10f;
            smokeAlpha = 0.42f;

            // 50 魔力/秒（内部按帧计算）。
            manaUse = 50f / 60f;
            // 炉体最大可储存魔力。
            manaCapacity = 500f;
            initialMana = 0f;
            manaBarColor = Color.valueOf("8fe6ff");
        }};

        // 炼药锅：把水转化为低纯魔液（不耗电）。
        alchemyPot = new AlchemyPot("alchemy-pot"){{
            localizedName = "炼药锅";
            description = "消耗魔石，将水等比转化为低纯魔液。";
            requirements(Category.crafting, ItemStack.with(
                Items.copper, 70,
                Items.lead, 60,
                FLItems.magicStone, 25
            ));
            category = Category.crafting;
            size = 2;
            health = 240;

            craftTime = 60f;
            hasPower = false;
            consumesPower = false;
            hasItems = true;
            hasLiquids = true;
            liquidCapacity = 120f;
            itemCapacity = 10;

            consumeItem(FLItems.magicStone, 1);
            // 水的消耗改为由炼药锅内部按“实际输出量”动态扣除，避免固定吃水导致跳变。
            outputLiquid = new LiquidStack(FLLiquids.mana, 10f / 60f);
        }};

        // 魔力发生器：消耗魔液和电力，产出并分发魔力。
        manaGenerator = new ManaGenerator("mana-generator"){{
            localizedName = "魔力发生器";
            requirements(Category.power, ItemStack.with(
                Items.copper, 140,
                Items.lead, 120,
                FLItems.mithril, 40
            ));
            category = Category.power;
            size = 3;
            health = 320;

            craftTime = 60f;
            hasItems = true;
            itemCapacity = 20;
            hasPower = true;
            consumesPower = true;
            hasLiquids = true;
            liquidCapacity = 240f;
            // 电力消耗：100/秒（1f=60/秒）。
            consumePower(100f / 60f);

            // 魔力产出：500/秒（内部按帧）。
            manaProduction = 500f / 60f;
            // 魔液消耗上限：25/秒。
            manaLiquidUse = 25f / 60f;
            // 本体魔力容量。
            manaCapacity = 2000f;
            initialMana = 0f;
            // 基础范围 18 格，秘银增强 +8 格，秘银耗量 0.2/秒。
            manaRange = 18;
            boostRangeBonus = 8;
            boostMithrilUsePerSecond = 0.2f;
            manaBarColor = Color.valueOf("8fe6ff");
            rangeColor = Color.valueOf("8fe6ff");

            // 核心数据库：归类为 power 标签，并显示在塔洛莉页签。
            databaseTag = "power";
            if(FLPlanets.fantaloryPlanet != null){
                databaseTabs.add(FLPlanets.fantaloryPlanet);
            }
        }};

        fantaCore = new CoreBlock("core-fantalory"){{
            requirements(Category.effect, ItemStack.with(
                Items.copper, 2000,
                Items.lead, 1500,
                Items.silicon, 1200,
                FLItems.mithril, 800
            ));
            size = 4;
            health = 6000;
            itemCapacity = 12000;
            unitCapModifier = 20;
            unitType = UnitTypes.evoke;
            alwaysUnlocked = true;
            incinerateNonBuildable = true;
        }};
    }
}
