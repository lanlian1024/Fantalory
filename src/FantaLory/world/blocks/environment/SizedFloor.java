package FantaLory.world.blocks.environment;

import mindustry.Vars;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

/**
 * 支持多格尺寸的地板类型
 * 使用 floorsize= 指定尺寸（1~4）
 */
public class SizedFloor extends Floor {
    public int floorsize = 1;
    private static boolean applying;

    public SizedFloor(String name){
        super(name);
    }

    public SizedFloor(String name, int variants){
        super(name, variants);
    }

    @Override
    public void floorChanged(Tile tile){
        if(applying) return;

        int size = clampSize(floorsize);
        if(size <= 1) return;

        applying = true;
        try{
            int sizeOffset = -((size - 1) / 2);
            int originX = tile.x + sizeOffset;
            int originY = tile.y + sizeOffset;
            for(int dx = 0; dx < size; dx++){
                for(int dy = 0; dy < size; dy++){
                    Tile other = Vars.world.tile(originX + dx, originY + dy);
                    if(other != null && other.floor() != this){
                        other.setFloor(this);
                    }
                }
            }
        }finally{
            applying = false;
        }
    }

    private int clampSize(int size){
        if(size < 1) return 1;
        if(size > 4) return 4;
        return size;
    }
}
