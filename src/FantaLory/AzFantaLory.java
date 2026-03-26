package FantaLory;

import FantaLory.content.blocks.FLBlocks;
import FantaLory.content.blocks.FLSandboxBlocks;
import FantaLory.content.blocks.FLTerrain;
import FantaLory.content.blocks.FLTurret;
import FantaLory.content.FLLiquids;
import FantaLory.content.FLTeams;
import FantaLory.content.FLTechTree;
import FantaLory.content.items.FLItems;
import FantaLory.content.planets.FLPlanets;
import FantaLory.content.sectors.FLSectors;
import FantaLory.content.units.FLUnits;
import FantaLory.ui.FLMenuEntry;
import FantaLory.ui.dialogs.FLResearchLayout;
import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.files.Fi;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Scaling;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Musics;
import mindustry.mod.Mod;
import mindustry.mod.Mods.LoadedMod;
import mindustry.ui.dialogs.BaseDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

public class AzFantaLory extends Mod{
    public static final String useModMenuMusicSetting = "fantalory-use-mod-menu-music";
    private static final String vanillaMenuMusicAsset = "menu";
    private static final String stableMenuMusicAsset = "carmen-cororo";
    private static final String menuMusicKeywordA = "carmen silv";
    private static final String menuMusicKeywordB = "cororo-accompanied";
    private static final float menuFadeStartSeconds = 5f;
    private static final float menuRestartSeconds = 3f;
    private static Music cachedVanillaMenuMusic;
    private static Music trackedMenuMusic;
    private static float trackedMenuMusicDuration = -1f;
    private static Field audioHandleField;
    private static Method streamLengthMethod;
    private static boolean musicLengthReflectionInit = false;

    public AzFantaLory(){
        Events.on(EventType.WorldLoadEvent.class, e -> fixFantaloryBuildContext());

        Events.on(EventType.ClientLoadEvent.class, e -> {
            if(cachedVanillaMenuMusic == null){
                cachedVanillaMenuMusic = Musics.menu;
            }
            applyMenuMusicPreference();
            FLResearchLayout.install();
            FLMenuEntry.install();
            showWelcomeDialog();
        });

        Events.run(EventType.Trigger.update, AzFantaLory::ensureMenuMusicLoop);
    }

    public static boolean useModMenuMusic(){
        return Core.settings.getBool(useModMenuMusicSetting, true);
    }

    public static void applyMenuMusicPreference(){
        if(!useModMenuMusic()){
            if(cachedVanillaMenuMusic != null){
                Musics.menu = cachedVanillaMenuMusic;
            }else{
                Musics.menu = Vars.tree.loadMusic(vanillaMenuMusicAsset);
            }
            if(Musics.menu != null){
                Musics.menu.setLooping(false);
            }
            return;
        }

        Fi stableOgg = Vars.tree.get("music/" + stableMenuMusicAsset + ".ogg");
        Fi stableMp3 = Vars.tree.get("music/" + stableMenuMusicAsset + ".mp3");
        if(stableOgg.exists() || stableMp3.exists()){
            Musics.menu = Vars.tree.loadMusic(stableMenuMusicAsset);
            if(Musics.menu != null){
                Musics.menu.setLooping(false);
            }
            return;
        }

        Fi preferred = findPreferredMenuMusicFile();
        if(preferred == null) return;

        String target = extractMusicAssetName(preferred);
        if(target != null){
            Musics.menu = Vars.tree.loadMusic(target);
            if(Musics.menu != null){
                Musics.menu.setLooping(false);
            }
        }
    }

    private static void ensureMenuMusicLoop(){
        if(Vars.state == null || !Vars.state.isMenu()) return;
        if(FLMenuEntry.isMainDialogShown()) return;
        if(Vars.ui != null){
            if(Vars.ui.planet != null && Vars.ui.planet.isShown()) return;
            if(Vars.ui.editor != null && Vars.ui.editor.isShown()) return;
        }

        Music menu = Musics.menu;
        if(menu == null){
            trackedMenuMusic = null;
            trackedMenuMusicDuration = -1f;
            return;
        }

        if(menu != trackedMenuMusic){
            trackedMenuMusic = menu;
            trackedMenuMusicDuration = resolveMusicDuration(menu);
        }

        menu.setLooping(false);
        if(Core.settings.getInt("musicvol", 100) <= 0) return;
        float baseVolume = Core.settings.getInt("musicvol", 100) / 100f;

        if(trackedMenuMusicDuration <= 0f || !Float.isFinite(trackedMenuMusicDuration)){
            trackedMenuMusicDuration = resolveMusicDuration(menu);
        }

        if(trackedMenuMusicDuration > 0f && Float.isFinite(trackedMenuMusicDuration)){
            float clampedPosition = Math.min(Math.max(0f, menu.getPosition()), trackedMenuMusicDuration);
            float remain = Math.max(0f, trackedMenuMusicDuration - clampedPosition);
            if(remain <= menuRestartSeconds){
                menu.setVolume(0f);
                menu.stop();
                menu.setVolume(baseVolume);
                menu.play();
                return;
            }else if(remain <= menuFadeStartSeconds){
                float fade = (remain - menuRestartSeconds) / (menuFadeStartSeconds - menuRestartSeconds);
                fade = Math.max(0f, Math.min(1f, fade));
                menu.setVolume(baseVolume * fade);
            }else{
                menu.setVolume(baseVolume);
            }
        }else{
            menu.setVolume(baseVolume);
        }
        if(!menu.isPlaying()){
            menu.play();
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

    private static Fi findPreferredMenuMusicFile(){
        LoadedMod mod = Vars.mods == null ? null : Vars.mods.getMod(AzFantaLory.class);
        if(mod == null || mod.root == null || !mod.root.exists()) return null;

        Seq<Fi> files = new Seq<>();
        mod.root.walk(file -> {
            if(file == null || file.isDirectory()) return;
            String ext = file.extension();
            if(ext == null) return;
            String lowerExt = ext.toLowerCase(Locale.ROOT);
            if(!"ogg".equals(lowerExt) && !"mp3".equals(lowerExt)) return;
            String assetName = extractMusicAssetName(file);
            if(assetName == null || assetName.isEmpty()) return;
            files.add(file);
        });
        files.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        for(Fi file : files){
            String base = extractMusicAssetName(file);
            if(base == null) continue;
            if(stableMenuMusicAsset.equalsIgnoreCase(base)){
                return file;
            }
        }

        for(Fi file : files){
            String base = extractMusicAssetName(file);
            if(base == null) continue;
            String lower = base.toLowerCase(Locale.ROOT);
            if(lower.contains(menuMusicKeywordA) && lower.contains(menuMusicKeywordB)){
                return file;
            }
        }

        for(Fi file : files){
            String base = extractMusicAssetName(file);
            if(base == null) continue;
            String lower = base.toLowerCase(Locale.ROOT);
            if(lower.contains("carmen")){
                return file;
            }
        }

        if(files.isEmpty()) return null;
        return files.first();
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

    private void showWelcomeDialog(){
        if(!FLMenuEntry.isWelcomeIllustrationEnabled()) return;

        Time.run(10f, () -> {
            BaseDialog dialog = new BaseDialog("HelloMod");
            TextureRegion bg = Core.atlas.find("fantalory-welcome-bg");
            Image bgImage = new Image(new TextureRegionDrawable(bg));
            bgImage.setScaling(Scaling.stretch);

            Table closeLayer = new Table();
            closeLayer.top().right();
            TextButton close = new TextButton("OK", mindustry.ui.Styles.defaultt);
            close.clicked(dialog::hide);
            closeLayer.add(close).pad(6f);

            dialog.cont.stack(bgImage, closeLayer).grow();
            dialog.show();
        });
    }

    private void fixFantaloryBuildContext(){
        if(Vars.state == null || Vars.state.rules == null) return;
        boolean fantalorySector = Vars.state.rules.sector != null &&
            FLPlanets.fantaloryPlanet != null &&
            Vars.state.rules.sector.planet == FLPlanets.fantaloryPlanet;

        boolean fantaloryModMap = Vars.state.map != null &&
            Vars.state.map.file != null &&
            Vars.state.map.file.path() != null &&
            Vars.state.map.file.path().replace('\\', '/').contains("/maps/fantalory-planet/");

        if(!fantalorySector && !fantaloryModMap) return;

        if(FLTeams.talory != null){
            Vars.state.rules.defaultTeam = FLTeams.talory;
        }
        Vars.state.rules.buildSpeedMultiplier = Math.max(Vars.state.rules.buildSpeedMultiplier, 1f);
        Vars.state.rules.unitBuildSpeedMultiplier = Math.max(Vars.state.rules.unitBuildSpeedMultiplier, 1f);
    }

    @Override
    public void loadContent(){
        super.loadContent();
        FLItems.load();
        FLLiquids.load();
        FLTeams.load();
        FLPlanets.load();
        FLSectors.load(Vars.mods.getMod(getClass()));
        FLTerrain.load();
        FLUnits.load();
        FLBlocks.load();
        FLSandboxBlocks.load();
        FLTurret.load();
        FLTechTree.load();
    }
}
