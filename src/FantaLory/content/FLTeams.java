package FantaLory.content;

import arc.graphics.Color;
import mindustry.game.Team;

public class FLTeams{
    // 使用独立队伍槽位（不占用原版 6 个基础队伍）。
    public static final int taloryTeamId = 77;
    public static Team talory;

    public static void load(){
        talory = Team.get(taloryTeamId);
        // 强制命名为 talory，显示文本走 team.talory.name 本地化。
        talory.name = "talory";
        talory.setPalette(
            Color.valueOf("b9efc8"),
            Color.valueOf("9fe6bc"),
            Color.valueOf("7bcf9d")
        );
    }
}
