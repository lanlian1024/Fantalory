package FantaLory.content.blocks;

import FantaLory.world.blocks.mana.ManaState;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Env;

import static mindustry.type.ItemStack.with;

/**
 * 沙盒魔力黑洞方块。
 */
public class FLSandboxManaVoid extends Block{
    public static final int range = 6;

    public FLSandboxManaVoid(String name){
        super(name);
        requirements(Category.power, BuildVisibility.sandboxOnly, with());
        size = 2;
        update = true;
        solid = true;
        alwaysUnlocked = true;
        envEnabled = Env.any;
        buildType = FLSandboxManaVoidBuild::new;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        float wx = x * Vars.tilesize + offset;
        float wy = y * Vars.tilesize + offset;
        Drawf.dashSquare(Pal.accent, wx, wy, range * Vars.tilesize);
    }
}

/**
 * 沙盒魔力黑洞建筑实体。
 */
class FLSandboxManaVoidBuild extends Building{
    @Override
    public void drawSelect(){
        super.drawSelect();
        Drawf.dashSquare(Pal.accent, x, y, FLSandboxManaVoid.range * Vars.tilesize);
    }

    @Override
    public void updateTile(){
        for(int dx = -FLSandboxManaVoid.range; dx <= FLSandboxManaVoid.range; dx++){
            for(int dy = -FLSandboxManaVoid.range; dy <= FLSandboxManaVoid.range; dy++){
                Tile other = tile.nearby(dx, dy);
                if(other == null) continue;
                float cur = ManaState.get(other);
                if(cur > 0f){
                    ManaState.consume(other, cur);
                }
            }
        }
    }
}
