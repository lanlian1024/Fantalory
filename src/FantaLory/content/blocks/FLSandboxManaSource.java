package FantaLory.content.blocks;

import FantaLory.world.blocks.mana.ManaGenerator;
import FantaLory.world.blocks.mana.ManaState;
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
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Env;

import static mindustry.type.ItemStack.with;

/**
 * 沙盒魔力源方块。
 */
public class FLSandboxManaSource extends Block{
    public static final Color manaColor = Color.valueOf("8fe6ff");
    public static final int range = 6;

    public FLSandboxManaSource(String name){
        super(name);
        requirements(Category.power, BuildVisibility.sandboxOnly, with());
        size = 2;
        update = true;
        solid = true;
        alwaysUnlocked = true;
        envEnabled = Env.any;
        lightRadius = 80f;
        emitLight = true;
        buildType = FLSandboxManaSourceBuild::new;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        float wx = x * Vars.tilesize + offset;
        float wy = y * Vars.tilesize + offset;
        float r = range * Vars.tilesize;
        Drawf.dashCircle(wx, wy, r, manaColor);
        Vars.indexer.eachBlock(Vars.player.team(), wx, wy, r,
            other -> ManaState.capacity(other.tile) > 0f,
            other -> Drawf.selected(other, Tmp.c1.set(manaColor).a(Mathf.absin(4f, 1f)))
        );
    }
}

/**
 * 沙盒魔力源建筑实体。
 */
class FLSandboxManaSourceBuild extends Building{
    float heat;

    @Override
    public void draw(){
        super.draw();
        float r = FLSandboxManaSource.range * Vars.tilesize;
        if(r <= 0.001f) return;

        heat = Mathf.lerpDelta(heat, 1f, 0.08f);
        float a = Mathf.clamp(0.28f + heat * 0.40f + Mathf.absin(Time.time, 50f, 0.16f), 0f, 0.9f);
        Draw.z(Layer.shields);
        Draw.color(FLSandboxManaSource.manaColor);
        Draw.alpha(a);
        Fill.circle(x, y, r);
        Draw.alpha(0.9f);
        Lines.stroke(1.2f);

        Seq<FLSandboxRangeCircle> others = collectOtherRanges(r);
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

    @Override
    public void drawSelect(){
        super.drawSelect();
        float r = FLSandboxManaSource.range * Vars.tilesize;
        Drawf.dashCircle(x, y, r, FLSandboxManaSource.manaColor);
        Vars.indexer.eachBlock(this, r,
            other -> ManaState.capacity(other.tile) > 0f,
            other -> Drawf.selected(other, Tmp.c1.set(FLSandboxManaSource.manaColor).a(Mathf.absin(4f, 1f)))
        );
    }

    @Override
    public void updateTile(){
        for(int dx = -FLSandboxManaSource.range; dx <= FLSandboxManaSource.range; dx++){
            for(int dy = -FLSandboxManaSource.range; dy <= FLSandboxManaSource.range; dy++){
                Tile other = tile.nearby(dx, dy);
                if(other == null) continue;
                float cap = ManaState.capacity(other);
                if(cap > 0f){
                    ManaState.add(other, cap);
                }
            }
        }
    }

    private Seq<FLSandboxRangeCircle> collectOtherRanges(float selfRange){
        Seq<FLSandboxRangeCircle> result = new Seq<>();
        float maxOtherRange = Math.max(18f, FLSandboxManaSource.range) * Vars.tilesize;
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
                    result.add(new FLSandboxRangeCircle(other.x, other.y, r));
                }
            }
        }
        return result;
    }

    private boolean isOverlapped(float px, float py, Seq<FLSandboxRangeCircle> others){
        for(int i = 0; i < others.size; i++){
            FLSandboxRangeCircle other = others.get(i);
            if(other == null) continue;
            if(Mathf.within(px, py, other.x, other.y, other.r - 0.1f)) return true;
        }
        return false;
    }

    private float otherRange(Building other){
        if(other.block instanceof ManaGenerator){
            return ((ManaGenerator)other.block).manaRange * Vars.tilesize;
        }
        if(other.block.name != null && other.block.name.endsWith("mana-source")){
            return FLSandboxManaSource.range * Vars.tilesize;
        }
        return 0f;
    }
}

/**
 * 范围圆数据。
 */
class FLSandboxRangeCircle{
    final float x;
    final float y;
    final float r;

    FLSandboxRangeCircle(float x, float y, float r){
        this.x = x;
        this.y = y;
        this.r = r;
    }
}
