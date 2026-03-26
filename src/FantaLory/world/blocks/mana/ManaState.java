package FantaLory.world.blocks.mana;

import arc.math.Mathf;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.Tile;

/**
 * 魔力状态管理器（按 Tile 存储）。
 *
 * 设计要点：
 * 1) 每个 Tile 对应一个 ManaNode，记录当前魔力与容量。
 * 2) 提供统一的初始化、增减、转化、连线与均衡接口。
 * 3) 魔液转魔力使用固定比例 liquidToManaRatio。
 */
public final class ManaState{
    // 最小容量下限，防止出现 0 容量导致除零与异常状态。
    private static final float minCapacity = 0.0001f;
    // 魔液 -> 魔力换算比例：1 液体 = 20 魔力。
    private static final float liquidToManaRatio = 20f;
    // 全局节点表：key 为 tile.pos()，value 为该格子的魔力节点。
    private static final IntMap<ManaNode> nodes = new IntMap<>();

    private ManaState(){
    }

    /**
     * 初始化或重置某 Tile 的魔力节点。
     */
    public static void init(Tile tile, float capacity, float initial){
        if(tile == null) return;
        int pos = tile.pos();
        ManaNode node = nodes.get(pos);
        if(node == null){
            node = new ManaNode();
            nodes.put(pos, node);
        }
        node.capacity = Math.max(minCapacity, capacity);
        node.mana = Mathf.clamp(initial, 0f, node.capacity);
    }

    /**
     * 删除某 Tile 的魔力节点，并同步断开双向连接。
     */
    public static void remove(Tile tile){
        if(tile == null) return;
        int pos = tile.pos();
        ManaNode node = nodes.remove(pos);
        if(node == null) return;
        for(int i = 0; i < node.links.size; i++){
            int other = node.links.get(i);
            ManaNode otherNode = nodes.get(other);
            if(otherNode != null){
                otherNode.links.removeValue(pos);
            }
        }
    }

    // 读取当前魔力值。
    public static float get(Tile tile){
        ManaNode node = getNode(tile);
        return node == null ? 0f : node.mana;
    }

    // 读取容量上限。
    public static float capacity(Tile tile){
        ManaNode node = getNode(tile);
        return node == null ? 0f : node.capacity;
    }

    // 读取魔力填充比例（0~1）。
    public static float fraction(Tile tile){
        ManaNode node = getNode(tile);
        if(node == null || node.capacity <= 0f) return 0f;
        return node.mana / node.capacity;
    }

    // 增加魔力（自动钳制到容量上限）。
    public static void add(Tile tile, float amount){
        if(amount <= 0f) return;
        ManaNode node = getNode(tile);
        if(node == null) return;
        node.mana = Mathf.clamp(node.mana + amount, 0f, node.capacity);
    }

    // 暴露当前液体换算比例。
    public static float liquidToManaRatio(){
        return liquidToManaRatio;
    }

    /**
     * 将建筑液体仓中的指定液体按比例转换为魔力。
     * 返回值：本次成功转入的魔力量。
     */
    public static float fillFromLiquid(Building build, Liquid liquid){
        if(build == null || build.tile == null || build.liquids == null || liquid == null) return 0f;

        ManaNode node = nodes.get(build.tile.pos());
        if(node == null) return 0f;

        float space = node.capacity - node.mana;
        if(space <= 0.0001f) return 0f;

        float available = build.liquids.get(liquid);
        if(available <= 0.0001f) return 0f;

        // 先按容量空位计算可消耗液体上限，再与实际库存取最小。
        float maxLiquidBySpace = space / liquidToManaRatio;
        float usedLiquid = Math.min(maxLiquidBySpace, available);
        if(usedLiquid <= 0.0001f) return 0f;

        float convertedMana = usedLiquid * liquidToManaRatio;
        build.liquids.remove(liquid, usedLiquid);
        node.mana = Mathf.clamp(node.mana + convertedMana, 0f, node.capacity);
        return convertedMana;
    }

    /**
     * 消耗魔力。
     * 返回 true 表示消耗成功，false 表示库存不足。
     */
    public static boolean consume(Tile tile, float amount){
        if(amount <= 0f) return true;
        ManaNode node = getNode(tile);
        if(node == null) return false;
        if(node.mana + 0.0001f < amount) return false;
        node.mana -= amount;
        return true;
    }

    /**
     * 建立两节点双向连接，用于后续魔力均衡。
     */
    public static void link(Tile a, Tile b){
        if(a == null || b == null) return;
        int pa = a.pos(), pb = b.pos();
        ManaNode na = nodes.get(pa);
        ManaNode nb = nodes.get(pb);
        if(na == null || nb == null) return;
        if(!na.links.contains(pb)) na.links.add(pb);
        if(!nb.links.contains(pa)) nb.links.add(pa);
    }

    /**
     * 在当前 Tile 与其连接节点之间做均衡。
     * strength 范围 0~1，数值越大，趋近平均值越快。
     */
    public static void balance(Tile tile, float strength){
        ManaNode node = getNode(tile);
        if(node == null || node.links.isEmpty()) return;
        strength = Mathf.clamp(strength, 0f, 1f);

        float total = node.mana;
        int count = 1;
        for(int i = 0; i < node.links.size; i++){
            ManaNode other = nodes.get(node.links.get(i));
            if(other != null){
                total += other.mana;
                count++;
            }
        }
        float avg = total / count;
        node.mana = Mathf.lerp(node.mana, avg, strength);
        for(int i = 0; i < node.links.size; i++){
            ManaNode other = nodes.get(node.links.get(i));
            if(other != null){
                other.mana = Mathf.lerp(other.mana, avg, strength);
            }
        }
    }

    // 读取指定 Tile 对应节点。
    private static ManaNode getNode(Tile tile){
        if(tile == null) return null;
        return nodes.get(tile.pos());
    }

    // 魔力节点结构：魔力值、容量、连接表。
    private static final class ManaNode{
        float mana = 0f;
        float capacity = 0f;
        IntSeq links = new IntSeq();
    }
}
