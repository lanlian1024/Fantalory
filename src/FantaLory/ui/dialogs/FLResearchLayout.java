package FantaLory.ui.dialogs;

import FantaLory.content.planets.FLPlanets;
import arc.Events;
import arc.math.Mathf;
import arc.math.geom.Rect;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.ui.dialogs.ResearchDialog;
import mindustry.ui.layout.BranchTreeLayout;
import mindustry.ui.layout.BranchTreeLayout.TreeLocation;
import mindustry.ui.layout.TreeLayout.TreeNode;

public class FLResearchLayout{
    private static final float gapLevels = 150f;
    private static final float gapNodes = 110f;
    private static final float maxJitter = 72f;
    private static boolean installed;

    public static void install(){
        if(installed) return;
        installed = true;
        Events.run(EventType.Trigger.update, FLResearchLayout::update);
    }

    private static void update(){
        if(Vars.ui == null || Vars.ui.research == null) return;
        if(FLPlanets.fantaloryPlanet == null || FLPlanets.fantaloryPlanet.techTree == null) return;

        ResearchDialog dialog = Vars.ui.research;
        if(!dialog.isShown() || dialog.root == null) return;
        if(dialog.root.node != FLPlanets.fantaloryPlanet.techTree) return;
        if(!needsRelayout(dialog.root)) return;

        LayoutNode root = new LayoutNode(dialog.root, null);
        new BranchTreeLayout(){{
            gapBetweenLevels = gapLevels;
            gapBetweenNodes = gapNodes;
            rootLocation = TreeLocation.right;
        }}.layout(root);

        copyLayout(root);
        applyVerticalJitter(dialog.root, 0);
        rebuildBounds(dialog);

        dialog.view.hoverNode = null;
        dialog.view.infoTable.remove();
        dialog.view.infoTable.clear();
    }

    private static boolean needsRelayout(ResearchDialog.TechTreeNode root){
        if(root.children == null || root.children.length == 0) return false;

        ResearchDialog.TechTreeNode sample = null;
        for(ResearchDialog.TechTreeNode child : root.children){
            if(child != null && child.visible){
                sample = child;
                break;
            }
        }
        if(sample == null) sample = root.children[0];
        if(sample == null) return false;

        float dx = Math.abs(sample.x - root.x);
        float dy = Math.abs(sample.y - root.y);
        // 竖向布局或左到右布局都需要重排为右到左
        return dy >= dx || sample.x > root.x;
    }

    private static void copyLayout(LayoutNode node){
        node.node.x = node.x;
        node.node.y = node.y;
        if(node.children == null) return;
        for(LayoutNode child : node.children){
            copyLayout(child);
        }
    }

    private static void applyVerticalJitter(ResearchDialog.TechTreeNode node, int depth){
        if(node == null) return;
        if(node.parent != null){
            node.y += jitterFor(node.node.content.id, depth);
        }
        if(node.children == null) return;
        for(ResearchDialog.TechTreeNode child : node.children){
            applyVerticalJitter(child, depth + 1);
        }
    }

    private static float jitterFor(int id, int depth){
        int hash = id * 1103515245 + depth * 12345 + 0x3f4a2b1;
        float n = ((hash >>> 8) & 1023) / 1023f;
        float signed = n * 2f - 1f;
        float scale = Mathf.clamp(0.65f + depth * 0.08f, 0.65f, 1f);
        return signed * maxJitter * scale;
    }

    private static void rebuildBounds(ResearchDialog dialog){
        float minx = 0f, miny = 0f, maxx = 0f, maxy = 0f;
        for(ResearchDialog.TechTreeNode n : dialog.nodes){
            if(n == null || !n.visible) continue;
            minx = Math.min(n.x - n.width / 2f, minx);
            maxx = Math.max(n.x + n.width / 2f, maxx);
            miny = Math.min(n.y - n.height / 2f, miny);
            maxy = Math.max(n.y + n.height / 2f, maxy);
        }
        dialog.bounds = new Rect(minx, miny, maxx - minx, maxy - miny);
        dialog.bounds.x -= dialog.nodeSize * 1.5f;
    }

    private static class LayoutNode extends TreeNode<LayoutNode>{
        private final ResearchDialog.TechTreeNode node;

        private LayoutNode(ResearchDialog.TechTreeNode node, LayoutNode parent){
            this.node = node;
            this.parent = parent;
            this.width = this.height = node.width;
            if(node.children == null){
                children = new LayoutNode[0];
            }else{
                children = new LayoutNode[node.children.length];
                for(int i = 0; i < node.children.length; i++){
                    children[i] = new LayoutNode(node.children[i], this);
                }
            }
        }
    }
}
