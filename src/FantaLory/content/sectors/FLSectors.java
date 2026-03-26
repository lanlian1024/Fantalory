package FantaLory.content.sectors;

import FantaLory.content.FLTeams;
import FantaLory.content.planets.FLPlanets;
import arc.files.Fi;
import arc.struct.Seq;
import mindustry.mod.Mods.LoadedMod;
import mindustry.type.SectorPreset;

public class FLSectors{
    private static final String planetMapFolder = "fantalory-planet";

    public static void load(LoadedMod mod){
        if(mod == null || mod.root == null || FLPlanets.fantaloryPlanet == null) return;

        Fi folder = mod.root.child("maps").child(planetMapFolder);
        if(!folder.exists() || !folder.isDirectory()) return;

        Seq<Fi> mapFiles = new Seq<>();
        for(Fi file : folder.list()){
            if(file != null && "msav".equalsIgnoreCase(file.extension())){
                mapFiles.add(file);
            }
        }
        mapFiles.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        int limit = Math.min(mapFiles.size, FLPlanets.fantaloryPlanet.sectors.size);
        for(int i = 0; i < limit; i++){
            Fi file = mapFiles.get(i);
            int index = i;
            String contentName = "fantalory-sector-" + (index + 1);
            String fileName = planetMapFolder + "/" + file.nameWithoutExtension();

            new SectorPreset(contentName, fileName, FLPlanets.fantaloryPlanet, index){{
                localizedName = "Fantalory " + (index + 1);
                alwaysUnlocked = true;
                requireUnlock = false;
                addStartingItems = true;
                showSectorLandInfo = true;
                showHidden = true;
                difficulty = 1f;
                rules = r -> {
                    r.defaultTeam = FLTeams.talory;
                    r.winWave = captureWave;
                    r.buildSpeedMultiplier = Math.max(r.buildSpeedMultiplier, 1f);
                    r.unitBuildSpeedMultiplier = Math.max(r.unitBuildSpeedMultiplier, 1f);
                };
            }};
        }
    }
}
