package FantaLory.content.blocks;

import mindustry.world.Block;

/**
 * 沙盒建筑注册入口。
 */
public class FLSandboxBlocks{
    public static Block manaSource;
    public static Block manaVoid;

    public static void load(){
        manaSource = new FLSandboxManaSource("mana-source");
        manaVoid = new FLSandboxManaVoid("mana-void");
    }
}
