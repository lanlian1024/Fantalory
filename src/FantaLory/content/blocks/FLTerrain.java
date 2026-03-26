package FantaLory.content.blocks;

import FantaLory.content.FLLiquids;
import FantaLory.content.items.FLItems;
import FantaLory.world.blocks.environment.SizedFloor;
import arc.graphics.Color;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.OreBlock;

public class FLTerrain{

    public static Floor wood_floor, flstone_floor;
    public static Floor magic_floor_2x2, magic_floor_3x3, magic_floor_4x4;
    public static OreBlock mithrilOre, magicStoneOre, redCopperOre, ironOre, aragoldOre;
    public static Floor lowpurity_magic_liquid;

    public static void load(){
        wood_floor = new Floor("wood_floor"){{
            localizedName = "木地板";
            variants = 3;
            albedo = 0.8f;
        }};

        flstone_floor = new Floor("flstone_floor"){{
            localizedName = "石砖地板";
            variants = 2;
            albedo = 0.8f;
        }};

        magic_floor_2x2 = new SizedFloor("magic_floor_2x2"){{
            localizedName = "魔纹地板-2x2";
            floorsize = 2;
            variants = 1;
        }};

        magic_floor_3x3 = new SizedFloor("magic_floor_3x3"){{
            localizedName = "魔纹地板-3x3";
            floorsize = 3;
            variants = 1;
        }};

        magic_floor_4x4 = new SizedFloor("magic_floor_4x4"){{
            localizedName = "魔纹地板-4x4";
            floorsize = 4;
            variants = 1;
        }};

        // 秘银矿地板。
        mithrilOre = new OreBlock("ore-mithril", FLItems.mithril){{
            localizedName = "秘银矿";
            variants = 2;
            oreScale = 1.2f;
        }};

        // 魔石矿地板。
        magicStoneOre = new OreBlock("ore-magic-stone", FLItems.magicStone){{
            localizedName = "魔石矿";
            variants = 3;
            oreScale = 1.15f;
            oreThreshold = 0.86f;
        }};

        // 赤铜矿地板。
        redCopperOre = new OreBlock("ore-red-copper", FLItems.redCopper){{
            localizedName = "赤铜矿";
            variants = 3;
            oreScale = 1.1f;
            oreThreshold = 0.84f;
        }};

        // 铁矿地板。
        ironOre = new OreBlock("ore-iron", FLItems.iron){{
            localizedName = "铁矿";
            variants = 3;
            oreScale = 1.05f;
            oreThreshold = 0.86f;
        }};

        // 霞金矿地板。
        aragoldOre = new OreBlock("ore-aragold", FLItems.aragold){{
            localizedName = "霞金矿";
            variants = 3;
            oreScale = 1.08f;
            oreThreshold = 0.87f;
        }};

        // 低纯魔液地板。
        lowpurity_magic_liquid = new Floor("lowpurity_magic_liquid"){{
            localizedName = "低纯魔液";
            isLiquid = true;
            liquidDrop = FLLiquids.mana;
            speedMultiplier = 0.6f;
            drownTime = 600f;
            emitLight = true;
            lightRadius = 3f;
            lightColor = Color.valueOf("8fe6ff").a(0.8f);
        }};
    }
}
