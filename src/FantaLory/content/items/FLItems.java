package FantaLory.content.items;

import arc.graphics.Color;
import mindustry.type.Item;

public class FLItems{

    public static Item mithril;
    public static Item magicStone;
    public static Item redCopper;
    public static Item iron;
    public static Item aragold;

    public static void load(){
        // 秘银：中期核心金属。
        mithril = new Item("mithril", Color.valueOf("f5f5f5")){{
            hardness = 4;
            cost = 0.5f;
            alwaysUnlocked = true;
            localizedName = "秘银";
            description = "一种稀有的银白色金属，质地轻盈且坚硬。";
        }
            @Override
            public void loadIcon(){
                super.loadIcon();

                // 读取 5 张秘银帧图（mithril1..5），并按“快速两轮->停2秒->快速两轮->停2秒”循环。
                arc.graphics.g2d.TextureRegion baseFull = new arc.graphics.g2d.TextureRegion(fullIcon);
                arc.graphics.g2d.TextureRegion baseUi = new arc.graphics.g2d.TextureRegion(uiIcon);
                arc.graphics.g2d.TextureRegion[] regions = new arc.graphics.g2d.TextureRegion[5];
                for(int i = 1; i <= 5; i++){
                    regions[i - 1] = arc.Core.atlas.find(name + i, baseFull);
                }

                fullIcon = new arc.graphics.g2d.TextureRegion(baseFull);
                uiIcon = new arc.graphics.g2d.TextureRegion(baseUi);

                final float fastFrameTime = 3f;  // 快速闪烁
                final float waitDuration = 120f; // 2 秒（60 tick/s）
                final float burstDuration = regions.length * 2f * fastFrameTime; // 5 帧播放两轮
                final float cycleDuration = burstDuration + waitDuration + burstDuration + waitDuration;

                arc.Events.run(mindustry.game.EventType.Trigger.update, () -> {
                    float phase = arc.util.Time.globalTime % cycleDuration;
                    int frame = -1;

                    if(phase < burstDuration){
                        frame = ((int)(phase / fastFrameTime)) % regions.length;
                    }else if(phase >= burstDuration + waitDuration && phase < burstDuration + waitDuration + burstDuration){
                        float secondPhase = phase - burstDuration - waitDuration;
                        frame = ((int)(secondPhase / fastFrameTime)) % regions.length;
                    }

                    if(frame >= 0){
                        fullIcon.set(regions[frame]);
                        uiIcon.set(regions[frame]);
                    }else{
                        fullIcon.set(baseFull);
                        uiIcon.set(baseUi);
                    }
                });
            }
        };

        // 魔石：魔力相关建筑的基础原料。
        magicStone = new Item("magic-stone", Color.valueOf("8fe6ff")){{
            hardness = 3;
            cost = 0.6f;
            alwaysUnlocked = true;
            // 启用图标帧动画：读取 magic-stone1..5，并自动插值过渡帧实现闪烁感。
            // 说明：当前为 5 帧 + 1 过渡帧配置，整轮周期延长为原先的 2 倍。
            frames = 5;
            transitionFrames = 1;
            frameTime = 12f;
            localizedName = "魔石";
            description = "蕴含魔力的矿石。";
        }};

        // 赤铜：偏早期的基础金属。
        redCopper = new Item("red-copper", Color.valueOf("c7684b")){{
            hardness = 2;
            cost = 0.45f;
            alwaysUnlocked = true;
            localizedName = "赤铜";
            description = "带有赤色光泽的铜质金属，可用于基础制造,不是巧克力!";
        }};

        // 铁：通用结构材料。
        iron = new Item("iron", Color.valueOf("9aa3ad")){{
            hardness = 3;
            cost = 0.7f;
            alwaysUnlocked = true;
            localizedName = "铁";
            description = "常见且坚韧的金属材料，适合承重与结构用途。";
        }};

        // 霞金：带霞色辉光的稀有金属。
        aragold = new Item("aragold", Color.valueOf("f3a96a")){{
            hardness = 5;
            cost = 0.85f;
            alwaysUnlocked = true;
            localizedName = "霞金";
            description = "带有晚霞色泽的贵金属，兼具延展性与魔力亲和。";
        }
            @Override
            public void loadIcon(){
                super.loadIcon();

                // 读取 5 张霞金帧图（aragold1..5），循环规则与秘银一致：
                // 快速两轮 -> 停2秒 -> 快速两轮 -> 停2秒。
                arc.graphics.g2d.TextureRegion baseFull = new arc.graphics.g2d.TextureRegion(fullIcon);
                arc.graphics.g2d.TextureRegion baseUi = new arc.graphics.g2d.TextureRegion(uiIcon);
                arc.graphics.g2d.TextureRegion[] regions = new arc.graphics.g2d.TextureRegion[5];
                for(int i = 1; i <= 5; i++){
                    regions[i - 1] = arc.Core.atlas.find(name + i, baseFull);
                }

                fullIcon = new arc.graphics.g2d.TextureRegion(baseFull);
                uiIcon = new arc.graphics.g2d.TextureRegion(baseUi);

                final float fastFrameTime = 3f;  // 快速闪烁
                final float waitDuration = 120f; // 2 秒（60 tick/s）
                final float burstDuration = regions.length * 2f * fastFrameTime; // 5 帧播放两轮
                final float cycleDuration = burstDuration + waitDuration + burstDuration + waitDuration;

                arc.Events.run(mindustry.game.EventType.Trigger.update, () -> {
                    float phase = arc.util.Time.globalTime % cycleDuration;
                    int frame = -1;

                    if(phase < burstDuration){
                        frame = ((int)(phase / fastFrameTime)) % regions.length;
                    }else if(phase >= burstDuration + waitDuration && phase < burstDuration + waitDuration + burstDuration){
                        float secondPhase = phase - burstDuration - waitDuration;
                        frame = ((int)(secondPhase / fastFrameTime)) % regions.length;
                    }

                    if(frame >= 0){
                        fullIcon.set(regions[frame]);
                        uiIcon.set(regions[frame]);
                    }else{
                        fullIcon.set(baseFull);
                        uiIcon.set(baseUi);
                    }
                });
            }
        };
    }
}
