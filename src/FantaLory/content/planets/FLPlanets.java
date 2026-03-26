package FantaLory.content.planets;

import FantaLory.content.FLTeams;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;
import arc.struct.Seq;
import arc.util.noise.Simplex;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.game.Team;
import mindustry.graphics.g3d.GenericMesh;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.HexSkyMesh;
import mindustry.graphics.g3d.MatMesh;
import mindustry.graphics.g3d.MultiMesh;
import mindustry.graphics.g3d.NoiseMesh;
import mindustry.maps.planet.SerpuloPlanetGenerator;
import mindustry.type.Planet;
import mindustry.world.meta.Env;

public class FLPlanets{
    public static Planet fantaloryPlanet;

    public static void load(){
        fantaloryPlanet = new Planet("fantalory-planet", Planets.sun, 1f, 2){{
            localizedName = "塔洛莉";
            generator = new FantaloryPlanetGenerator();
            meshLoader = () -> new HexMesh(this, 7);
            cloudMeshLoader = () -> new MultiMesh(
                new HexSkyMesh(this, 8, 0.12f, 0.12f, 6, Color.valueOf("b7efcc").a(0.332f), 2, 0.45f, 0.95f, 0.40f),
                new HexSkyMesh(this, 27, 0.18f, 0.30f, 6, Color.valueOf("8fe6ff").a(0.243f), 2, 0.52f, 1.15f, 0.60f),
                createAsteroidBelt(this),
                createOuterLightRing(this)
            );
            sectorSeed = 17;
            startSector = 0;
            alwaysUnlocked = true;
            allowLaunchToNumbered = true;
            allowSelfSectorLaunch = true;
            allowLaunchSchematics = true;
            allowLaunchLoadout = true;
            iconColor = Color.valueOf("b9efc8");
            atmosphereColor = Color.valueOf("8dd8ad");
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.33f;
            landCloudColor = Color.valueOf("aef4cf").cpy().a(0.398f);
            defaultEnv = Env.terrestrial;
            ruleSetter = r -> {
                r.defaultTeam = FLTeams.talory;
                r.waveTeam = Team.crux;
                r.placeRangeCheck = false;
                r.coreDestroyClear = true;
            };
        }};
    }

    private static GenericMesh createAsteroidBelt(Planet planet){
        Seq<GenericMesh> pieces = new Seq<>();
        Rand rand = new Rand(planet.id * 991L + 73L);
        Color c1 = Color.valueOf("8fe6ff");
        Color c2 = Color.valueOf("d7f7ff");

        int count = 240;
        float minOrbit = planet.radius * 2.10f;
        float maxOrbit = planet.radius * 2.55f;
        float bandHeight = planet.radius * 0.08f;
        float tilt = -10f;
        float cos = Mathf.cosDeg(tilt);
        float sin = Mathf.sinDeg(tilt);

        for(int i = 0; i < count; i++){
            float angle = rand.random(360f);
            float orbit = rand.random(minOrbit, maxOrbit);
            float y = rand.range(bandHeight);
            float size = rand.random(0.008f, 0.023f);
            int seed = 500 + i * 13;

            GenericMesh rock = new NoiseMesh(
                planet, seed, 1,
                size, 2, 0.58f, 0.42f, 0.17f,
                c1, c2, 2, 0.60f, 0.65f, 0.18f
            );

            float x = Mathf.cosDeg(angle) * orbit;
            float z = Mathf.sinDeg(angle) * orbit;
            float ty = y * cos - z * sin;
            float tz = y * sin + z * cos;
            Vec3 pos = new Vec3(x, ty, tz);
            pieces.add(new MatMesh(rock, new Mat3D().setToTranslation(pos)));
        }

        return new MultiMesh(pieces.toArray(GenericMesh.class));
    }

    private static GenericMesh createOuterLightRing(Planet planet){
        Seq<GenericMesh> pieces = new Seq<>();
        Rand rand = new Rand(planet.id * 997L + 311L);
        Color c1 = Color.valueOf("b8efff");
        Color c2 = Color.valueOf("e5fbff");

        int count = 420;
        float minOrbit = planet.radius * 2.95f;
        float maxOrbit = planet.radius * 3.40f;
        float bandHeight = planet.radius * 0.02f;
        float tilt = 45f;
        float cos = Mathf.cosDeg(tilt);
        float sin = Mathf.sinDeg(tilt);

        for(int i = 0; i < count; i++){
            float angle = rand.random(360f);
            float orbit = rand.random(minOrbit, maxOrbit);
            float y = rand.range(bandHeight);
            float size = rand.random(0.0048f, 0.010f);
            int seed = 2100 + i * 17;

            GenericMesh glow = new NoiseMesh(
                planet, seed, 1,
                size, 1, 0.6f, 0.35f, 0.05f,
                c1, c2, 1, 0.55f, 0.55f, 0.0f
            );

            float x = Mathf.cosDeg(angle) * orbit;
            float z = Mathf.sinDeg(angle) * orbit;

            float ty = y * cos - z * sin;
            float tz = y * sin + z * cos;

            pieces.add(new MatMesh(glow, new Mat3D().setToTranslation(new Vec3(x, ty, tz))));
        }

        return new MultiMesh(pieces.toArray(GenericMesh.class));
    }

    private static class FantaloryPlanetGenerator extends SerpuloPlanetGenerator{
        private final Color lightGreen = Color.valueOf("b9efc8");
        private final Color lightBlue = Color.valueOf("8fe6ff");
        private final Color deepGreen = Color.valueOf("2f6f3e");
        private final Color darksandGreen = Color.valueOf("5f9f6c");
        private final Color darksandWaterGreen = Color.valueOf("4fa67a");
        private final Color darksandTaintedWaterGreen = Color.valueOf("9fd9b0");
        private final Color soilYellow = Color.valueOf("c9ad73");
        private final Color paleYellow = Color.valueOf("f0e2a7");

        @Override
        public float getHeight(Vec3 position){
            float base = super.getHeight(position);
            float ridge = Math.abs(Simplex.noise3d(
                seed + 91, 4, 0.52f, 1.25f,
                position.x * 2.2f, position.y * 2.2f, position.z * 2.2f
            ));
            float detail = Simplex.noise3d(
                seed + 37, 5, 0.58f, 2.1f,
                position.x * 2.8f, position.y * 2.8f, position.z * 2.8f
            );
            return Mathf.clamp(base + ridge * 0.08f + detail * 0.06f, 0f, 1.35f);
        }

        @Override
        public void getColor(Vec3 position, Color out){
            super.getColor(position, out);

            Color darksandMap = Blocks.darksand.mapColor;
            Color darksandWaterMap = Blocks.darksandWater.mapColor;
            Color darksandTaintedWaterMap = Blocks.darksandTaintedWater.mapColor;
            boolean darksandTone =
                Math.abs(out.r - darksandMap.r) < 0.035f &&
                Math.abs(out.g - darksandMap.g) < 0.035f &&
                Math.abs(out.b - darksandMap.b) < 0.035f;
            boolean darksandWaterTone =
                Math.abs(out.r - darksandWaterMap.r) < 0.045f &&
                Math.abs(out.g - darksandWaterMap.g) < 0.045f &&
                Math.abs(out.b - darksandWaterMap.b) < 0.045f;
            boolean darksandTaintedWaterTone =
                Math.abs(out.r - darksandTaintedWaterMap.r) < 0.045f &&
                Math.abs(out.g - darksandTaintedWaterMap.g) < 0.045f &&
                Math.abs(out.b - darksandTaintedWaterMap.b) < 0.045f;
            boolean blackTone = out.r < 0.18f && out.g < 0.18f && out.b < 0.18f;
            boolean darkBrownTone =
                out.r > 0.20f && out.r < 0.48f &&
                out.g > 0.10f && out.g < 0.33f &&
                out.b < 0.18f;
            boolean deepPurpleTone =
                out.b > 0.42f &&
                out.r > 0.16f && out.r < 0.42f &&
                out.g < 0.26f;
            boolean purpleTone = out.b > 0.35f && out.r > 0.28f && out.g < 0.35f;
            boolean blueTone = out.b > 0.40f && out.b > out.r + 0.08f && out.b > out.g + 0.04f;

            if(darksandTaintedWaterTone){
                out.lerp(darksandTaintedWaterGreen, 0.92f);
            }else if(darksandWaterTone){
                out.lerp(darksandWaterGreen, 0.92f);
            }else if(darksandTone){
                out.lerp(darksandGreen, 0.92f);
            }else if(blackTone){
                out.lerp(deepGreen, 0.9f);
            }else if(darkBrownTone){
                float stripe = Math.abs(Mathf.sin(position.x * 26f) * Mathf.cos(position.z * 26f));
                if(stripe > 0.52f){
                    out.lerp(soilYellow, 0.92f);
                }else{
                    out.lerp(paleYellow, 0.92f);
                }
            }else if(deepPurpleTone){
                out.lerp(deepGreen, 0.9f);
            }else if(purpleTone){
                out.lerp(lightGreen, 0.82f);
            }else if(blueTone){
                out.lerp(lightBlue, 0.75f);
            }
        }
    }
}
