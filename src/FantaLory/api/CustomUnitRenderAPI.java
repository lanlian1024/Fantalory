package FantaLory.api;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.graphics.Layer;
import mindustry.type.UnitType;
import mindustry.type.Weapon;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.WeakHashMap;

public final class CustomUnitRenderAPI{
    private static final ObjectMap<String, RenderConfig> configs = new ObjectMap<>();
    private static final WeakHashMap<UnitEntity, RenderState> states = new WeakHashMap<>();

    private CustomUnitRenderAPI(){
    }

    public static void register(String unitName, RenderConfig config){
        if(unitName == null || unitName.isEmpty() || config == null) return;
        configs.put(unitName, config);
    }

    public static boolean hasConfig(UnitType type){
        return type != null && findConfigByName(type.name) != null;
    }

    public static void update(UnitEntity unit){
        RenderConfig config = config(unit);
        if(config == null) return;

        RenderState state = state(unit);
        updateFacing(unit, state, config);
        mirrorWeaponX(unit, state, config);
        unit.rotation = config.upRotation;
    }

    public static boolean draw(UnitEntity unit){
        RenderConfig config = config(unit);
        if(config == null) return false;

        RenderState state = state(unit);
        unit.rotation = config.upRotation;
        drawShadow(unit, state);

        boolean firing = isFiring(unit);
        TextureRegion region = pickRegion(unit, state, config, firing);
        if(region != null){
            Draw.z(Layer.flyingUnit + 1f);
            Draw.rect(region, unit.x, unit.y, unit.rotation - 90f);
            Draw.reset();
        }

        if(config.drawExtras){
            drawExtras(unit);
        }
        Draw.reset();
        return true;
    }

    private static RenderConfig config(UnitEntity unit){
        if(unit == null || unit.type == null) return null;
        return findConfigByName(unit.type.name);
    }

    private static RenderConfig findConfigByName(String unitName){
        if(unitName == null) return null;
        RenderConfig exact = configs.get(unitName);
        if(exact != null) return exact;

        for(ObjectMap.Entry<String, RenderConfig> entry : configs){
            if(entry == null || entry.key == null) continue;
            String key = entry.key;
            if(unitName.equals(key) || unitName.endsWith("-" + key) || unitName.endsWith(key)){
                return entry.value;
            }
        }
        return null;
    }

    private static RenderState state(UnitEntity unit){
        RenderState state = states.get(unit);
        if(state == null){
            state = new RenderState();
            states.put(unit, state);
        }
        return state;
    }

    private static void updateFacing(UnitEntity unit, RenderState state, RenderConfig config){
        if(!updateFacingByAim(unit, state)){
            if(!state.lastXInited){
                state.lastXInited = true;
                state.lastX = unit.x;
                return;
            }
            float dx = unit.x - state.lastX;
            state.lastX = unit.x;
            if(Math.abs(dx) > config.positionThreshold){
                state.facingLeft = dx < 0f;
            }
        }
    }

    private static boolean updateFacingByAim(UnitEntity unit, RenderState state){
        try{
            Field mountsField = getFieldIfExists(unit.getClass(), "mounts");
            if(mountsField == null) mountsField = getFieldIfExists(UnitEntity.class, "mounts");
            if(mountsField != null){
                mountsField.setAccessible(true);
                Object mountsObj = mountsField.get(unit);
                if(mountsObj != null && mountsObj.getClass().isArray()){
                    int len = Array.getLength(mountsObj);
                    for(int i = 0; i < len; i++){
                        Object mount = Array.get(mountsObj, i);
                        if(mount == null) continue;

                        Float aimX = readFloatField(mount, "aimX");
                        if(aimX == null) continue;

                        Boolean shoot = readBoolField(mount, "shoot", "shooting", "shot");
                        boolean aiming = Boolean.TRUE.equals(shoot);
                        if(!aiming) continue;

                        if(Math.abs(aimX - unit.x) > 0.001f){
                            state.facingLeft = aimX < unit.x;
                            return true;
                        }
                    }
                }
            }
        }catch(Throwable ignored){
        }

        Float aimX = readFloatField(unit, "aimX");
        Float aimY = readFloatField(unit, "aimY");
        if(aimX != null && aimY != null){
            float dst2 = (aimX - unit.x) * (aimX - unit.x) + (aimY - unit.y) * (aimY - unit.y);
            if(dst2 > 0.01f && Math.abs(aimX - unit.x) > 0.001f){
                state.facingLeft = aimX < unit.x;
                return true;
            }
        }

        return false;
    }

    private static void mirrorWeaponX(UnitEntity unit, RenderState state, RenderConfig config){
        if(!config.mirrorWeaponX || unit.type == null || unit.type.weapons == null) return;
        for(int i = 0; i < unit.type.weapons.size; i++){
            Weapon weapon = unit.type.weapons.get(i);
            if(weapon == null) continue;

            Float baseX = state.weaponBaseX.get(weapon);
            if(baseX == null){
                baseX = weapon.x;
                state.weaponBaseX.put(weapon, baseX);
            }
            float absX = Math.abs(baseX);
            weapon.x = state.facingLeft ? -absX : absX;
        }
    }

    private static TextureRegion pickRegion(UnitEntity unit, RenderState state, RenderConfig config, boolean firing){
        cacheRegions(unit, config);

        if(firing){
            if(state.facingLeft){
                if(config.attackLeftRegionObj != null) return config.attackLeftRegionObj;
                if(config.leftRegionObj != null) return config.leftRegionObj;
            }
            if(config.attackRightRegionObj != null) return config.attackRightRegionObj;
        }

        if(state.facingLeft && config.leftRegionObj != null) return config.leftRegionObj;
        if(config.rightRegionObj != null) return config.rightRegionObj;
        return null;
    }

    private static void cacheRegions(UnitEntity unit, RenderConfig config){
        if(config.rightRegionObj == null) config.rightRegionObj = forceFindRegion(unit, config.rightRegion);
        if(config.leftRegionObj == null) config.leftRegionObj = forceFindRegion(unit, config.leftRegion);
        if(config.attackRightRegionObj == null) config.attackRightRegionObj = forceFindRegion(unit, config.attackRightRegion);
        if(config.attackLeftRegionObj == null) config.attackLeftRegionObj = forceFindRegion(unit, config.attackLeftRegion);
        if(config.attackLeftRegionObj == null && config.attackLeftAltRegion != null){
            config.attackLeftRegionObj = forceFindRegion(unit, config.attackLeftAltRegion);
        }
    }

    private static TextureRegion forceFindRegion(UnitEntity unit, String key){
        if(key == null || key.isEmpty()) return null;

        TextureRegion region = findRegion(key);
        if(region != null) return region;

        String low = key.toLowerCase();
        if(!low.equals(key)){
            region = findRegion(low);
            if(region != null) return region;
        }

        if(unit != null && unit.type != null && unit.type.name != null){
            String typeName = unit.type.name;
            String prefix = typeName;
            int dash = typeName.lastIndexOf('-');
            if(dash > 0) prefix = typeName.substring(0, dash);

            region = findRegion(prefix + "-" + key);
            if(region != null) return region;
            region = findRegion(prefix + "-" + low);
            if(region != null) return region;
        }

        return findRegionBySuffix(key);
    }

    private static TextureRegion findRegion(String name){
        TextureRegion region = Core.atlas.find(name);
        return region != null && region.found() ? region : null;
    }

    private static TextureRegion findRegionBySuffix(String suffix){
        String low = suffix.toLowerCase();
        try{
            Method getRegions = Core.atlas.getClass().getMethod("getRegions");
            Object regions = getRegions.invoke(Core.atlas);
            if(regions instanceof Iterable){
                for(Object obj : (Iterable<?>)regions){
                    if(!(obj instanceof TextureRegion)) continue;
                    Field nameField = getFieldIfExists(obj.getClass(), "name");
                    if(nameField == null) continue;
                    nameField.setAccessible(true);
                    Object nameObj = nameField.get(obj);
                    if(!(nameObj instanceof String)) continue;
                    String name = ((String)nameObj).toLowerCase();
                    if(name.equals(low) || name.endsWith(low)){
                        return (TextureRegion)obj;
                    }
                }
            }
        }catch(Throwable ignored){
        }
        return null;
    }

    private static boolean isFiring(UnitEntity unit){
        try{
            Field mountsField = getFieldIfExists(unit.getClass(), "mounts");
            if(mountsField == null) mountsField = getFieldIfExists(UnitEntity.class, "mounts");
            if(mountsField != null){
                mountsField.setAccessible(true);
                Object mountsObj = mountsField.get(unit);
                if(mountsObj != null && mountsObj.getClass().isArray()){
                    int len = Array.getLength(mountsObj);
                    for(int i = 0; i < len; i++){
                        Object mount = Array.get(mountsObj, i);
                        if(mount == null) continue;

                        Boolean charging = readBoolField(mount, "charging");
                        if(Boolean.TRUE.equals(charging)) return true;

                        Float heat = readFloatField(mount, "heat");
                        if(heat != null && heat > 0.35f) return true;

                        Boolean shoot = readBoolField(mount, "shoot");
                        Float reload = readFloatField(mount, "reload");
                        if(Boolean.TRUE.equals(shoot) && reload != null && reload <= 0.001f) return true;

                        Object weapon = readObjectField(mount, "weapon");
                        boolean continuous = Boolean.TRUE.equals(readBoolField(weapon, "continuous", "alwaysContinuous"));
                        if(continuous){
                            Object bullet = readObjectField(mount, "bullet");
                            if(bullet != null) return true;
                        }
                    }
                }
            }
        }catch(Throwable ignored){
        }
        return false;
    }

    private static void drawShadow(UnitEntity unit, RenderState state){
        boolean needShadow = false;
        try{
            needShadow = unit.isFlying() || (unit.type != null && unit.type.shadowElevation > 0f);
        }catch(Throwable ignored){
        }
        if(!needShadow || unit.type == null) return;

        try{
            float z = unit.elevation > 0.5f || (unit.type.flying && unit.dead)
                ? unit.type.flyingLayer
                : unit.type.groundLayer + Mathf.clamp(unit.type.hitSize / 4000f, 0f, 0.01f);
            Draw.z(Math.min(Layer.darkness, z - 1f));

            // 按当前朝向实时镜像投影，保证左向贴图对应左向投影。
            float prevX = Draw.xscl;
            if(state != null){
                Draw.xscl = state.facingLeft ? -1f : 1f;
            }
            unit.type.drawShadow(unit);
            Draw.xscl = prevX;
        }catch(Throwable ignored){
        }finally{
            Draw.reset();
        }
    }

    private static void drawExtras(UnitEntity unit){
        if(unit == null || unit.type == null) return;
        String[] extraMethods = new String[]{
            "drawEngines", "drawEngine", "drawThrusters", "drawMounts", "drawWeapons", "drawTurrets", "drawLegs", "drawLight"
        };
        for(String method : extraMethods){
            invokeIfExists(unit.type, method, unit);
        }
    }

    private static boolean invokeIfExists(Object target, String methodName, UnitEntity param){
        if(target == null) return false;
        try{
            Method method = null;
            Class<?> cls = target.getClass();

            try{
                method = cls.getMethod(methodName, UnitEntity.class);
            }catch(NoSuchMethodException ignored){
            }
            if(method == null){
                try{
                    method = cls.getMethod(methodName, Unit.class);
                }catch(NoSuchMethodException ignored){
                }
            }
            if(method == null){
                try{
                    method = cls.getMethod(methodName);
                }catch(NoSuchMethodException ignored){
                }
            }
            if(method == null) return false;

            method.setAccessible(true);
            if(method.getParameterCount() == 0){
                method.invoke(target);
            }else{
                method.invoke(target, param);
            }
            return true;
        }catch(Throwable ignored){
            return false;
        }
    }

    private static Float readFloatField(Object obj, String... names){
        if(obj == null || names == null) return null;
        for(String name : names){
            try{
                Field field = getFieldIfExists(obj.getClass(), name);
                if(field == null) continue;
                field.setAccessible(true);
                Object value = field.get(obj);
                if(value instanceof Number) return ((Number)value).floatValue();
            }catch(Throwable ignored){
            }
        }
        return null;
    }

    private static Boolean readBoolField(Object obj, String... names){
        if(obj == null || names == null) return null;
        for(String name : names){
            try{
                Field field = getFieldIfExists(obj.getClass(), name);
                if(field == null) continue;
                field.setAccessible(true);
                Object value = field.get(obj);
                if(value instanceof Boolean) return (Boolean)value;
                if(value instanceof Number) return ((Number)value).floatValue() > 0f;
            }catch(Throwable ignored){
            }
        }
        return null;
    }

    private static Object readObjectField(Object obj, String... names){
        if(obj == null || names == null) return null;
        for(String name : names){
            try{
                Field field = getFieldIfExists(obj.getClass(), name);
                if(field == null) continue;
                field.setAccessible(true);
                return field.get(obj);
            }catch(Throwable ignored){
            }
        }
        return null;
    }

    private static Field getFieldIfExists(Class<?> cls, String name){
        Class<?> cur = cls;
        while(cur != null){
            try{
                return cur.getField(name);
            }catch(NoSuchFieldException ignored){
            }
            try{
                return cur.getDeclaredField(name);
            }catch(NoSuchFieldException ignored){
            }
            cur = cur.getSuperclass();
        }
        return null;
    }

    private static final class RenderState{
        boolean facingLeft = false;
        boolean lastXInited = false;
        float lastX = 0f;
        final IdentityHashMap<Weapon, Float> weaponBaseX = new IdentityHashMap<>();
    }

    public static class RenderConfig{
        public final String rightRegion;
        public final String leftRegion;
        public final String attackRightRegion;
        public final String attackLeftRegion;
        public final String attackLeftAltRegion;

        public float upRotation = 90f;
        public float positionThreshold = 0.02f;
        public boolean mirrorWeaponX = true;
        public boolean drawExtras = false;

        transient TextureRegion rightRegionObj;
        transient TextureRegion leftRegionObj;
        transient TextureRegion attackRightRegionObj;
        transient TextureRegion attackLeftRegionObj;

        public RenderConfig(String rightRegion, String leftRegion, String attackRightRegion, String attackLeftRegion){
            this(rightRegion, leftRegion, attackRightRegion, attackLeftRegion, null);
        }

        public RenderConfig(String rightRegion, String leftRegion, String attackRightRegion, String attackLeftRegion, String attackLeftAltRegion){
            this.rightRegion = rightRegion;
            this.leftRegion = leftRegion;
            this.attackRightRegion = attackRightRegion;
            this.attackLeftRegion = attackLeftRegion;
            this.attackLeftAltRegion = attackLeftAltRegion;
        }
    }
}
