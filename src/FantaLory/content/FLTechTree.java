package FantaLory.content;

import FantaLory.content.blocks.FLBlocks;
import FantaLory.content.blocks.FLSandboxBlocks;
import FantaLory.content.blocks.FLTerrain;
import FantaLory.content.blocks.FLTurret;
import FantaLory.content.items.FLItems;
import FantaLory.content.planets.FLPlanets;
import FantaLory.content.units.FLUnits;
import arc.struct.ObjectFloatMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.TechTree.TechNode;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Objectives.SectorComplete;
import mindustry.type.Item;
import mindustry.type.SectorPreset;

import static mindustry.content.TechTree.context;
import static mindustry.content.TechTree.node;
import static mindustry.content.TechTree.nodeProduce;
import static mindustry.content.TechTree.nodeRoot;

public class FLTechTree{
    // 统一构建塔洛莉星球科技树，把模组内容全部挂在该树下。
    public static void load(){
        if(FLPlanets.fantaloryPlanet == null || FLBlocks.fantaCore == null) return;

        // 统一研究倍率，避免不同条目成本差异过大。
        ObjectFloatMap<Item> costMultipliers = new ObjectFloatMap<>();
        for(Item item : Vars.content.items()){
            costMultipliers.put(item, 0.08f);
        }

        // 根节点：塔洛莉核心。
        TechNode root = nodeRoot("fantalory", FLBlocks.fantaCore, () -> {
            context().researchCostMultipliers = costMultipliers;

            // 资源分支：赤铜/铁/霞金/魔石/秘银及其矿层。
            nodeProduce(FLItems.redCopper, () -> {
                node(FLTerrain.redCopperOre);
            });

            nodeProduce(FLItems.iron, () -> {
                node(FLTerrain.ironOre);
            });

            nodeProduce(FLItems.aragold, () -> {
                node(FLTerrain.aragoldOre);
            });

            nodeProduce(FLItems.magicStone, () -> {
                node(FLTerrain.magicStoneOre);

                // 炼药线：炼药锅 -> 魔液 -> 沙盒魔力源/虚空。
                node(FLBlocks.alchemyPot, () -> {
                    nodeProduce(FLLiquids.mana, () -> {
                        node(FLSandboxBlocks.manaSource, () -> {
                            node(FLSandboxBlocks.manaVoid);
                        });
                    });
                });
            });

            nodeProduce(FLItems.mithril, () -> {
                node(FLTerrain.mithrilOre);

                // 工业与战斗线：幻想炉 -> 魔力发生器 -> 炮塔。
                node(FLBlocks.fantaFurnace, () -> {
                    node(FLBlocks.manaGenerator, () -> {
                        node(FLTurret.pixelTurret, () -> {
                            node(FLTurret.starcrystal);
                        });
                    });
                });
            });

            // 独立单位职业树：一个父节点 + 五个职业子节点，不连接炮塔/原分支。
            FLUnits.loadTechTreeByCareer();

            // 地形/地板分支：把模组地表内容也挂入科技树。
            node(FLTerrain.wood_floor);
            node(FLTerrain.flstone_floor);
            node(FLTerrain.magic_floor_2x2, () -> {
                node(FLTerrain.magic_floor_3x3, () -> {
                    node(FLTerrain.magic_floor_4x4);
                });
            });
            node(FLTerrain.lowpurity_magic_liquid);
        });

        // 绑定根节点到塔洛莉星球，并递归绑定子节点。
        root.planet = FLPlanets.fantaloryPlanet;
        root.addPlanet(FLPlanets.fantaloryPlanet);
        FLPlanets.fantaloryPlanet.techTree = root;
        bindPlanetRecursive(root);

        // 把塔洛莉区块线性追加到同一棵科技树下。
        appendSectorNodes(root);
    }

    // 递归设置节点所属星球，确保所有条目显示在塔洛莉科技树上下文中。
    private static void bindPlanetRecursive(TechNode node){
        if(node == null) return;
        node.planet = FLPlanets.fantaloryPlanet;
        node.addPlanet(FLPlanets.fantaloryPlanet);
        for(TechNode child : node.children){
            bindPlanetRecursive(child);
        }
    }

    // 追加塔洛莉扇区节点，按扇区ID顺序串联解锁。
    private static void appendSectorNodes(TechNode root){
        Seq<SectorPreset> presets = Vars.content.sectors().select(s ->
            s != null &&
            s.planet == FLPlanets.fantaloryPlanet &&
            s.sector != null
        );
        presets.sort((a, b) -> Integer.compare(a.sector.id, b.sector.id));

        TechNode parent = root;
        SectorPreset previous = null;
        for(SectorPreset preset : presets){
            TechNode node = new TechNode(parent, (UnlockableContent)preset, preset.researchRequirements());
            node.planet = FLPlanets.fantaloryPlanet;
            node.addPlanet(FLPlanets.fantaloryPlanet);
            if(previous != null){
                node.objectives.add(new SectorComplete(previous));
            }
            parent = node;
            previous = preset;
        }
    }
}
