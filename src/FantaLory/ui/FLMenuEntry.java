package FantaLory.ui;

import FantaLory.AzFantaLory;
import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.files.Fi;
import arc.graphics.Color;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Slider;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.gen.Musics;
import mindustry.mod.Mods.LoadedMod;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

public final class FLMenuEntry{
    public static final String welcomeIllustrationSetting = "fantalory-show-welcome-illustration";
    private static final String stableCarmenAsset = "carmen-cororo";
    private static final float modRoomFadeStartSeconds = 5f;
    private static final float modRoomRestartSeconds = 3f;
    private static boolean installed = false;
    private static BaseDialog mainDialog;
    private static String currentPage = "encyclopedia";
    private static Music selectedMusic;
    private static boolean selectedRoomIsModMusic = false;
    private static boolean roomMusicPaused = false;
    private static boolean wasInFantaloryDialog = false;
    private static final ObjectSet<Music> roomMusicPool = new ObjectSet<>();
    private static float roomMusicProgress = 0f;
    private static String roomMusicTimeText = "00:00 / 剩余 --:--";
    private static String roomMusicNowPlayingText = "当前播放: 未播放";
    private static float selectedMusicDuration = -1f;
    private static boolean roomVolumeSliderSyncing = false;
    private static Field audioHandleField;
    private static Method streamLengthMethod;
    private static boolean musicLengthReflectionInit = false;

    private FLMenuEntry(){
    }

    public static void install(){
        if(installed || Vars.ui == null || Vars.ui.menufrag == null) return;
        installed = true;

        mainDialog = new BaseDialog("FantaLory");
        mainDialog.addCloseButton();
        mainDialog.cont.pane(FLMenuEntry::buildDialog).grow().pad(8f);
        mainDialog.cont.row();
        mainDialog.shown(() -> {
            wasInFantaloryDialog = true;
            // Use built-in SoundControl fade-out by clearing menu track while this dialog is open.
            Musics.menu = null;
        });
        mainDialog.hidden(() -> {
            wasInFantaloryDialog = false;
            stopRoomMusic(null);
            selectedMusic = null;
            selectedRoomIsModMusic = false;
            roomMusicPaused = false;
            resetRoomMusicProgress();
            AzFantaLory.applyMenuMusicPreference();
        });

        Events.run(EventType.Trigger.update, FLMenuEntry::updateMusicRoomPlayback);

        Vars.ui.menufrag.addButton("FantaLory", Icon.book, mainDialog::show);
    }

    private static void buildDialog(Table root){
        root.defaults().growX().pad(4f);

        Table top = new Table();
        Table content = new Table();
        content.top().left();
        content.defaults().left().pad(4f).growX();

        TextButton encyclopedia = new TextButton("百科", Styles.defaultt);
        TextButton codex = new TextButton("图鉴", Styles.defaultt);
        TextButton story = new TextButton("剧情回顾", Styles.defaultt);
        TextButton credits = new TextButton("制作组", Styles.defaultt);
        TextButton settings = new TextButton("设置", Styles.defaultt);

        encyclopedia.clicked(() -> showPage(content, "encyclopedia"));
        codex.clicked(() -> showPage(content, "codex"));
        story.clicked(() -> showPage(content, "story"));
        credits.clicked(() -> showPage(content, "credits"));
        settings.clicked(() -> showPage(content, "settings"));

        top.add(encyclopedia).height(46f).pad(3f);
        top.add(codex).height(46f).pad(3f);
        top.add(story).height(46f).pad(3f);
        top.add(credits).height(46f).pad(3f);
        top.add(settings).height(46f).pad(3f);

        root.add(top).top();
        root.row();
        root.image().height(3f).color(Color.valueOf("59a6bf")).growX().padTop(2f).padBottom(6f);
        root.row();
        root.add(content).grow().minHeight(380f);

        showPage(content, "encyclopedia");
    }

    private static void showPage(Table content, String page){
        currentPage = page;

        content.clear();

        switch(page){
            case "encyclopedia":
                buildEncyclopedia(content);
                break;
            case "codex":
                content.add("[accent]图鉴[]").left();
                content.row();
                content.add("用于放置单位、建筑、物品与液体图鉴。").left().wrap().growX();
                break;
            case "story":
                content.add("[accent]剧情回顾[]").left();
                content.row();
                content.add("用于放置章节剧情、事件时间线与回顾文本。").left().wrap().growX();
                break;
            case "credits":
                buildCredits(content);
                break;
            case "settings":
                buildSettings(content);
                break;
            default:
                content.add("未定义页面。").left();
                break;
        }
    }

    private static void buildEncyclopedia(Table content){
        content.add("[accent]百科目录[]").left();
        content.row();

        Table tabs = new Table();
        TextButton damage = new TextButton("伤害机制", Styles.defaultt);
        TextButton building = new TextButton("建筑机制", Styles.defaultt);
        TextButton career = new TextButton("单位职业", Styles.defaultt);
        TextButton skill = new TextButton("技能机制", Styles.defaultt);

        Table body = new Table();
        body.top().left();
        body.defaults().left().pad(4f).growX();

        damage.clicked(() -> showEncyclopediaCategory(body, "damage"));
        building.clicked(() -> showEncyclopediaCategory(body, "building"));
        career.clicked(() -> showEncyclopediaCategory(body, "career"));
        skill.clicked(() -> showEncyclopediaCategory(body, "skill"));

        tabs.add(damage).height(42f).pad(3f);
        tabs.add(building).height(42f).pad(3f);
        tabs.add(career).height(42f).pad(3f);
        tabs.add(skill).height(42f).pad(3f);

        content.add(tabs).left();
        content.row();
        content.image().height(2f).color(Color.valueOf("4f6d78")).growX().padTop(2f).padBottom(6f);
        content.row();
        content.add(body).growX().left();

        showEncyclopediaCategory(body, "damage");
    }

    private static void showEncyclopediaCategory(Table body, String key){
        body.clear();
        switch(key){
            case "damage":
                buildDamageCategory(body);
                break;
            case "building":
                buildBuildingCategory(body);
                break;
            case "career":
                buildCareerCategory(body);
                break;
            case "skill":
                buildSkillCategory(body);
                break;
            default:
                body.add("未定义分类。").left();
                break;
        }
    }

    private static void buildDamageCategory(Table content){
        content.add("[accent]伤害机制[]").left();
        content.row();
        content.add("本分类包含：物理伤害、法术伤害、真实伤害、护盾交互与结算顺序。").left().wrap().growX();
        content.row();
        content.image().height(1f).color(Color.valueOf("3c5560")).growX().padTop(2f).padBottom(6f);
        content.row();

        addSection(
            content,
            "1) <物理伤害>（原版机制）",
            "本模组将原版默认伤害链定义为 <物理伤害>。\n" +
            "单位受到物理伤害时，核心可理解为：\n" +
            "物理最终伤害 = 护甲折算后伤害 / 生命系数 / 队伍伤害系数\n" +
            "其中“护甲折算后伤害”可理解为：基础伤害减去护甲带来的减免（并保留最小有效伤害）。\n" +
            "物理范围伤害（爆炸/溅射）会按“距离衰减”计算：离中心越近伤害越高，越远越低；超出半径后不再受伤。\n" +
            "单位拥有 <无敌> 状态时，物理伤害会被完全阻挡。"
        );

        addSection(
            content,
            "2) 法术伤害机制",
            "法术伤害是独立于原版物理链的新机制，按法术抗性线性减伤：\n" +
            "法术最终伤害 = 基础法术伤害 × (1 - 法术抗性/100)\n" +
            "法术抗性被限制在 0%~100%。\n" +
            "法术伤害不走原版护甲计算，并且不会被 <无敌> 状态阻挡。"
        );

        addSection(
            content,
            "3) 真实伤害机制",
            "真实伤害是独立机制，直接扣除生命值，不参与护甲与法术抗性。\n" +
            "当前附带真实伤害规则：按目标最大生命值 10% 计算，伤害上限 500；\n" +
            "当目标最大生命值超过 5000 时，不再按百分比继续增长，直接按 500 封顶。"
        );

        addSection(
            content,
            "4) 护盾与城墙交互",
            "法术子弹有 0.5% 概率附带真实伤害（按每发子弹判定一次）。\n" +
            "生效对象：\n" +
            "- 城墙：附带真实伤害作用于城墙生命值。\n" +
            "- 全向力场单位：附带真实伤害作用于护盾容量。\n" +
            "- 弧形护盾单位：附带真实伤害作用于护盾容量。\n" +
            "- 力墙投影建筑：附带真实伤害作用于投影护盾容量。"
        );

        addSection(
            content,
            "5) 机制叠加与结算顺序",
            "真实伤害是“附带”机制，不会替代法术伤害本体。\n" +
            "通常先结算法术伤害，再在触发条件成立时追加真实伤害或护盾扣减。\n" +
            "因此同一发子弹可同时产生法术伤害与真实伤害效果。"
        );
    }

    private static void buildBuildingCategory(Table content){
        content.add("[accent]建筑机制[]").left();
        content.row();
        content.add("本分类包含：魔石用途、魔液转化与魔力传输。").left().wrap().growX();
        content.row();
        content.image().height(1f).color(Color.valueOf("3c5560")).growX().padTop(2f).padBottom(6f);
        content.row();

        addSection(
            content,
            "1) 魔石用途",
            "魔石是魔法工业链的基础材料：\n" +
            "- 炼药锅固定消耗魔石作为催化材料。\n" +
            "- 幻想炉与部分建筑配方会使用魔石参与建造或生产。\n" +
            "可理解为：魔石是把普通资源接入魔法系统的关键材料。"
        );

        addSection(
            content,
            "2) 魔液用途与转化",
            "当前基础魔液为“低纯魔液”。\n" +
            "- 炼药锅将水转化为低纯魔液，输出速率受下游可接收量限制，按实际输出稳定吃水。\n" +
            "- 魔力设备将魔液转成魔力时，固定换算比为：1 液体 = 20 魔力。\n" +
            "- 魔液既可供发生器使用，也可为部分魔法建筑提供补给。"
        );

        addSection(
            content,
            "3) 魔力传输机制",
            "魔力以“建筑缓存值/容量”方式存储。每个魔力建筑都有当前值和上限。\n" +
            "- 魔力发生器在范围内把可分配魔力平均分发给缺魔目标。\n" +
            "- 秘银增幅可扩大发生器传输范围，并按时间消耗秘银。\n" +
            "- 沙盒魔力源会在范围内直接充满魔力；沙盒魔力虚空会清空范围内魔力。\n" +
            "- 发生器不会回灌到其他发生器或沙盒源/虚空。"
        );
    }

    private static void buildCareerCategory(Table content){
        content.add("[accent]单位职业[]").left();
        content.row();
        content.add(
            "塔洛莉单位采用职业分类树，分为五类：\n" +
            "1. 战士\n" +
            "2. 坦克\n" +
            "3. 魔法师\n" +
            "4. 治疗术士\n" +
            "5. 特殊职业\n" +
            "当前已实装单位：skyLaser，归属 [#8fe6ff]魔法师[]。"
        ).left().wrap().growX();
        content.row();
        content.image().height(1f).color(Color.valueOf("3c5560")).growX().padTop(2f).padBottom(6f);
        content.row();
        content.add(
            "后续新增单位会按职业分别放入对应分支，\n" +
            "科技树与核心数据库都会按职业归类显示。"
        ).left().wrap().growX();
    }

    private static void buildSkillCategory(Table content){
        content.add("[accent]技能机制[]").left();
        content.row();
        content.add(
            "当前单位技能主要通过武器机制表现：\n" +
            "- 主弹与分裂弹组合\n" +
            "- 命中触发状态与特效\n" +
            "- 法术伤害与附带真实伤害共同结算\n" +
            "- 与护盾系统联动（力墙、弧形护盾、投影盾）\n" +
            "后续新增单位技能会继续在本分类补充。"
        ).left().wrap().growX();
    }

    private static void addSection(Table content, String title, String body){
        content.add("[accent]" + title + "[]").left();
        content.row();
        content.add(body).left().wrap().growX();
        content.row();
        content.image().height(1f).color(Color.valueOf("3c5560")).growX().padTop(2f).padBottom(6f);
        content.row();
    }

    private static void buildCredits(Table content){
        content.add("[accent]制作组[]").left();
        content.row();

        Table split = new Table();
        split.top().left();

        Table left = new Table();
        left.top().left();
        left.defaults().left().pad(6f).growX();
        left.add("代码:vibe coding").left().row();
        left.add("音乐:Fooler").left().row();
        left.add("贴图:姬宮華戀").left().row();
        left.add("美术:姬宮華戀").left().row();
        left.add("饭饭:姬宮華戀").left().row();
        left.add("甜品:姬宮華戀").left().row();
        left.add("睡觉:姬宮華戀").left().row();

        Table right = new Table();
        right.top().left();
        right.defaults().left().pad(4f).growX();
        Table roomHeader = new Table();
        roomHeader.defaults().pad(2f);
        roomHeader.add("[accent]music room[]").left().growX();

        Table volumeInfo = new Table();
        volumeInfo.defaults().padLeft(6f);

        TextButton pauseToggle = new TextButton("暂停", Styles.defaultt);
        pauseToggle.clicked(() -> {
            if(selectedMusic == null) return;
            if(roomMusicPaused){
                roomMusicPaused = false;
                float baseVolume = Core.settings.getInt("musicvol", 100) / 100f;
                syncSelectedMusic(baseVolume);
            }else{
                roomMusicPaused = true;
                selectedMusic.pause(true);
            }
        });
        pauseToggle.update(() -> {
            if(selectedMusic == null){
                pauseToggle.setDisabled(true);
                pauseToggle.setText("暂停");
            }else{
                pauseToggle.setDisabled(false);
                pauseToggle.setText(roomMusicPaused ? "继续" : "暂停");
            }
        });
        volumeInfo.add(pauseToggle).width(86f).right();

        volumeInfo.add("[lightgray]音量[]").right();

        Slider volumeSlider = new Slider(0f, 100f, 1f, false);
        volumeSlider.setStyle(Styles.defaultSlider);
        volumeSlider.setValue(Core.settings.getInt("musicvol", 100));
        volumeSlider.moved(value -> {
            if(roomVolumeSliderSyncing) return;
            int volume = Math.max(0, Math.min(100, Math.round(value)));
            Core.settings.put("musicvol", volume);
        });
        volumeSlider.update(() -> {
            int settingVolume = Core.settings.getInt("musicvol", 100);
            int sliderVolume = Math.round(volumeSlider.getValue());
            if(sliderVolume == settingVolume) return;
            roomVolumeSliderSyncing = true;
            volumeSlider.setValue(settingVolume);
            roomVolumeSliderSyncing = false;
        });

        volumeInfo.add(volumeSlider).width(120f);
        volumeInfo.label(() -> Math.round(volumeSlider.getValue()) + "%").minWidth(42f).right();

        roomHeader.add(volumeInfo).right();

        Table progressInfo = new Table();
        progressInfo.defaults().padLeft(8f);
        progressInfo.label(() -> roomMusicNowPlayingText).left().growX().colspan(3).padBottom(2f).row();
        Bar progressBar = new Bar(() -> "", () -> Color.valueOf("8fcfff"), () -> roomMusicProgress);
        progressInfo.add(progressBar).width(250f).height(18f).right();
        progressInfo.add().width(18f);
        progressInfo.label(() -> roomMusicTimeText).left().width(190f);

        roomHeader.add(progressInfo).padLeft(18f).right();
        right.add(roomHeader).padBottom(6f).growX().row();
        right.image().height(2f).color(Color.valueOf("59a6bf")).growX().padBottom(6f).row();

        Table rightSplit = new Table();
        rightSplit.top().left();

        Table mindustrySide = new Table();
        mindustrySide.top().left();
        mindustrySide.defaults().left().pad(3f).growX();
        mindustrySide.add("[accent]Mindustry-原声带[]").center().growX().padBottom(4f).row();
        mindustrySide.image().height(1.5f).color(Color.valueOf("4f6d78")).growX().padBottom(4f).row();

        Table mindustryList = new Table();
        mindustryList.top().left();
        mindustryList.defaults().growX().height(44f).pad(2f);
        fillMusicButtons(mindustryList, collectMindustryMusicEntries(), "没有发现原版音乐。");
        mindustrySide.pane(mindustryList).grow().scrollX(false).height(330f).row();

        Table fantalorySide = new Table();
        fantalorySide.top().left();
        fantalorySide.defaults().left().pad(3f).growX();
        fantalorySide.add("[accent]Lanota-精选集[]").center().growX().padBottom(4f).row();
        fantalorySide.image().height(1.5f).color(Color.valueOf("4f6d78")).growX().padBottom(4f).row();

        Table fantaloryList = new Table();
        fantaloryList.top().left();
        fantaloryList.defaults().growX().height(44f).pad(2f);
        fillMusicButtons(fantaloryList, collectFantaloryMusicEntries(), "music 文件夹内没有 .mp3/.ogg。");
        fantalorySide.pane(fantaloryList).grow().scrollX(false).height(330f).row();

        rightSplit.add(mindustrySide).grow().padRight(8f);
        rightSplit.image().width(2f).growY().color(Color.valueOf("59a6bf")).padRight(8f);
        rightSplit.add(fantalorySide).grow();
        right.add(rightSplit).grow();

        Cell<Table> leftCell = split.add(left).top().growY().padRight(8f);
        split.image().width(2f).growY().color(Color.valueOf("59a6bf")).padRight(10f);
        split.add(right).grow().top();

        split.update(() -> {
            float target = split.getWidth() * 0.15f;
            leftCell.width(Math.max(140f, target));
        });

        content.add(split).grow().minHeight(380f);
    }

    private static void buildSettings(Table content){
        content.add("[accent]设置[]").left();
        content.row();
        content.image().height(2f).color(Color.valueOf("59a6bf")).growX().padTop(2f).padBottom(8f);
        content.row();

        Table row = new Table();
        row.defaults().pad(6f);
        row.add("每次进入游戏显示欢迎插图").left().growX();

        CheckBox toggle = new CheckBox("");
        toggle.setChecked(isWelcomeIllustrationEnabled());
        toggle.changed(() -> Core.settings.put(welcomeIllustrationSetting, toggle.isChecked()));
        row.add(toggle).right().width(46f);

        content.add(row).growX();
        content.row();

        Table menuMusicRow = new Table();
        menuMusicRow.defaults().pad(6f);
        menuMusicRow.add("菜单音乐使用模组曲目").left().growX();

        CheckBox menuMusicToggle = new CheckBox("");
        menuMusicToggle.setChecked(AzFantaLory.useModMenuMusic());
        menuMusicToggle.changed(() -> {
            Core.settings.put(AzFantaLory.useModMenuMusicSetting, menuMusicToggle.isChecked());
            AzFantaLory.applyMenuMusicPreference();
        });
        menuMusicRow.add(menuMusicToggle).right().width(46f);

        content.add(menuMusicRow).growX();
    }

    public static boolean isWelcomeIllustrationEnabled(){
        return Core.settings.getBool(welcomeIllustrationSetting, true);
    }

    public static boolean isMainDialogShown(){
        return mainDialog != null && mainDialog.isShown();
    }

    private static void fillMusicButtons(Table table, Seq<MusicEntry> entries, String emptyText){
        if(entries.isEmpty()){
            table.add(emptyText).left().row();
            return;
        }

        for(MusicEntry entry : entries){
            TextButton button = new TextButton(entry.displayName, Styles.defaultt);
            button.clicked(() -> playSelectedMusic(entry));
            table.add(button).growX().row();
        }
    }

    private static Seq<MusicEntry> collectMindustryMusicEntries(){
        Seq<MusicEntry> out = new Seq<>();
        ObjectSet<Music> seenMusic = new ObjectSet<>();

        try{
            for(Field field : Musics.class.getFields()){
                if(field.getType() != Music.class) continue;
                Music music = (Music)field.get(null);
                if(music == null || !seenMusic.add(music)) continue;
                out.add(new MusicEntry(field.getName() + "-Mindustry", music));
            }
        }catch(Exception ignored){
        }

        out.sort((a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
        return out;
    }

    private static Seq<MusicEntry> collectFantaloryMusicEntries(){
        Seq<MusicEntry> out = new Seq<>();
        LoadedMod mod = getFantaloryLoadedMod();
        if(mod == null || mod.root == null || !mod.root.exists()){
            return out;
        }

        ObjectSet<String> seen = new ObjectSet<>();
        mod.root.walk(file -> {
            if(file == null || file.isDirectory()) return;
            if(!isMusicFile(file)) return;

            String assetName = extractMusicAssetName(file);
            if(assetName == null || assetName.isEmpty()) return;
            if(stableCarmenAsset.equalsIgnoreCase(assetName)) return;

            String key = assetName.toLowerCase(Locale.ROOT);
            if(!seen.add(key)) return;

            String display = file.nameWithoutExtension();
            out.add(new MusicEntry(display, assetName));
        });

        out.sort((a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
        return out;
    }

    private static LoadedMod getFantaloryLoadedMod(){
        if(Vars.mods == null) return null;
        return Vars.mods.getMod(AzFantaLory.class);
    }

    private static boolean isMusicFile(Fi file){
        String ext = file.extension();
        if(ext == null) return false;
        String lower = ext.toLowerCase(Locale.ROOT);
        return "ogg".equals(lower) || "mp3".equals(lower);
    }

    private static String extractMusicAssetName(Fi file){
        String path = file.path();
        if(path == null) return null;

        String normalized = path.replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);

        int index = lower.lastIndexOf("/music/");
        String relative;
        if(index >= 0){
            relative = normalized.substring(index + 7);
        }else if(lower.startsWith("music/")){
            relative = normalized.substring(6);
        }else{
            return null;
        }

        if(relative.isEmpty()) return null;
        int dot = relative.lastIndexOf('.');
        if(dot <= 0) return null;
        return relative.substring(0, dot);
    }

    private static void playSelectedMusic(MusicEntry entry){
        if(entry == null) return;
        Music music = entry.resolveMusic();
        if(music == null){
            if(Vars.ui != null){
                Vars.ui.showInfoFade("[scarlet]音乐加载失败，请检查 music 文件夹与打包内容。[]");
            }
            return;
        }
        stopRoomMusic(music);
        selectedMusic = music;
        selectedRoomIsModMusic = entry.isFantaloryMusic();
        roomMusicPaused = false;
        roomMusicNowPlayingText = "当前播放: " + entry.displayName;
        selectedMusicDuration = resolveMusicDuration(selectedMusic);
        float baseVolume = Core.settings.getInt("musicvol", 100) / 100f;
        syncSelectedMusic(baseVolume);
        updateRoomMusicProgress();
    }

    private static void updateMusicRoomPlayback(){
        boolean inFantaloryDialog = mainDialog != null && mainDialog.isShown();
        if(inFantaloryDialog){
            // Keep menu track silenced inside this dialog; SoundControl will fade it out.
            Musics.menu = null;
            float baseVolume = Core.settings.getInt("musicvol", 100) / 100f;
            if(!roomMusicPaused){
                syncSelectedMusic(baseVolume);
            }
            updateRoomMusicProgress();
        }else if(wasInFantaloryDialog){
            stopRoomMusic(null);
            selectedMusic = null;
            selectedRoomIsModMusic = false;
            roomMusicPaused = false;
            resetRoomMusicProgress();
            AzFantaLory.applyMenuMusicPreference();
        }

        wasInFantaloryDialog = inFantaloryDialog;
    }

    private static void syncSelectedMusic(float volume){
        if(selectedMusic == null) return;

        selectedMusic.setLooping(!shouldUseManualRoomLoop());
        selectedMusic.setVolume(volume);
        if(!selectedMusic.isPlaying()){
            selectedMusic.play();
        }
    }

    private static void stopRoomMusic(Music keep){
        for(Music music : roomMusicPool){
            if(music != null && music != keep){
                music.stop();
            }
        }
        if(keep == null){
            roomMusicPool.clear();
        }
    }

    private static void resetRoomMusicProgress(){
        roomMusicProgress = 0f;
        roomMusicTimeText = "00:00 / 剩余 --:--";
        roomMusicNowPlayingText = "当前播放: 未播放";
        selectedMusicDuration = -1f;
    }

    private static void updateRoomMusicProgress(){
        if(selectedMusic == null){
            resetRoomMusicProgress();
            return;
        }

        float position = Math.max(0f, selectedMusic.getPosition());

        if(selectedMusicDuration <= 0f || !Float.isFinite(selectedMusicDuration)){
            selectedMusicDuration = resolveMusicDuration(selectedMusic);
        }

        if(selectedMusicDuration > 0f && Float.isFinite(selectedMusicDuration)){
            float clampedPosition = Math.min(position, selectedMusicDuration);
            float remain = Math.max(0f, selectedMusicDuration - clampedPosition);
            float baseVolume = Core.settings.getInt("musicvol", 100) / 100f;

            if(shouldUseManualRoomLoop() && !roomMusicPaused){
                if(remain <= modRoomRestartSeconds){
                    selectedMusic.setVolume(0f);
                    restartSelectedRoomMusic(baseVolume);
                    clampedPosition = 0f;
                    remain = selectedMusicDuration;
                }else if(remain <= modRoomFadeStartSeconds){
                    float fade = (remain - modRoomRestartSeconds) / (modRoomFadeStartSeconds - modRoomRestartSeconds);
                    fade = Math.max(0f, Math.min(1f, fade));
                    selectedMusic.setVolume(baseVolume * fade);
                }else{
                    selectedMusic.setVolume(baseVolume);
                }
            }

            roomMusicProgress = Math.max(0f, Math.min(1f, clampedPosition / selectedMusicDuration));
            roomMusicTimeText = formatTime(clampedPosition) + " / 剩余 " + formatTime(remain);
        }else{
            roomMusicProgress = 0f;
            roomMusicTimeText = formatTime(position) + " / 剩余 --:--";
        }
    }

    private static float resolveMusicDuration(Music music){
        if(music == null) return -1f;

        try{
            if(!musicLengthReflectionInit){
                audioHandleField = Class.forName("arc.audio.AudioSource").getDeclaredField("handle");
                audioHandleField.setAccessible(true);

                Class<?> soloudClass = Class.forName("arc.audio.Soloud");
                streamLengthMethod = soloudClass.getDeclaredMethod("streamLength", long.class);
                streamLengthMethod.setAccessible(true);
                musicLengthReflectionInit = true;
            }

            if(audioHandleField == null || streamLengthMethod == null) return -1f;

            long handle = audioHandleField.getLong(music);
            if(handle <= 0L) return -1f;

            Object raw = streamLengthMethod.invoke(null, handle);
            if(raw instanceof Number){
                float duration = ((Number)raw).floatValue();
                if(Float.isFinite(duration) && duration > 0f){
                    return duration;
                }
            }
        }catch(Throwable ignored){
            musicLengthReflectionInit = true;
            audioHandleField = null;
            streamLengthMethod = null;
        }

        return -1f;
    }

    private static boolean shouldUseManualRoomLoop(){
        return selectedMusic != null &&
            selectedRoomIsModMusic &&
            selectedMusicDuration > 0f &&
            Float.isFinite(selectedMusicDuration);
    }

    private static void restartSelectedRoomMusic(float volume){
        if(selectedMusic == null) return;
        selectedMusic.stop();
        selectedMusic.setLooping(false);
        selectedMusic.setVolume(volume);
        selectedMusic.play();
    }

    private static String formatTime(float secondsFloat){
        int total = Math.max(0, (int)secondsFloat);
        int seconds = total % 60;
        int minutes = (total / 60) % 60;
        int hours = total / 3600;

        if(hours > 0){
            return hours + ":" + twoDigits(minutes) + ":" + twoDigits(seconds);
        }
        return twoDigits(minutes) + ":" + twoDigits(seconds);
    }

    private static String twoDigits(int value){
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static class MusicEntry{
        final String displayName;
        private Music music;
        private final String assetName;

        MusicEntry(String displayName, Music music){
            this.displayName = displayName;
            this.music = music;
            this.assetName = null;
        }

        MusicEntry(String displayName, String assetName){
            this.displayName = displayName;
            this.assetName = assetName;
        }

        boolean isFantaloryMusic(){
            return assetName != null;
        }

        Music resolveMusic(){
            if(music != null){
                roomMusicPool.add(music);
                return music;
            }

            if(assetName != null){
                String lowerAsset = assetName.toLowerCase(Locale.ROOT);
                if(lowerAsset.contains("carmen")){
                    Fi stableOgg = Vars.tree.get("music/" + stableCarmenAsset + ".ogg");
                    Fi stableMp3 = Vars.tree.get("music/" + stableCarmenAsset + ".mp3");
                    if(stableOgg.exists() || stableMp3.exists()){
                        music = Vars.tree.loadMusic(stableCarmenAsset);
                        roomMusicPool.add(music);
                        return music;
                    }
                }
            }

            if(assetName != null){
                Fi ogg = Vars.tree.get("music/" + assetName + ".ogg");
                Fi mp3 = Vars.tree.get("music/" + assetName + ".mp3");
                if(ogg.exists() || mp3.exists()){
                    music = Vars.tree.loadMusic(assetName);
                    roomMusicPool.add(music);
                    return music;
                }
            }
            return null;
        }
    }
}
