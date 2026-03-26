package FantaLory.content.units;

import FantaLory.api.CustomUnitRenderAPI;
import FantaLory.api.bullets.MagicBasicBulletType;
import FantaLory.api.MagicDamageAPI;
import FantaLory.api.units.FLHealerUnitType;
import FantaLory.api.units.FLMageUnitType;
import FantaLory.api.units.FLSpecialUnitType;
import FantaLory.api.units.FLTankUnitType;
import FantaLory.api.units.FLUnitType;
import FantaLory.api.units.FLWarriorUnitType;
import FantaLory.content.planets.FLPlanets;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.struct.Seq;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

import java.util.IdentityHashMap;

import static mindustry.content.TechTree.node;

public class FLUnits{
    // 模组单位引用。
    public static FLMageUnitType skyLaser;
    // 职业树显示节点（1个父节点 + 5个职业子节点）。
    public static FLUnitType careerRootNode;
    public static FLWarriorUnitType warriorCareerNode;
    public static FLTankUnitType tankCareerNode;
    public static FLMageUnitType mageCareerNode;
    public static FLHealerUnitType healerCareerNode;
    public static FLSpecialUnitType specialCareerNode;
    // 按职业维护单位注册列表，后续新增单位直接放入对应列表。
    private static final Seq<FLUnitType> warriorUnits = new Seq<>();
    private static final Seq<FLUnitType> tankUnits = new Seq<>();
    private static final Seq<FLUnitType> mageUnits = new Seq<>();
    private static final Seq<FLUnitType> healerUnits = new Seq<>();
    private static final Seq<FLUnitType> specialUnits = new Seq<>();
    // 单位通用属性里的法术抗性字段。
    private static final Stat magicResistStat = new Stat("magicresist");

    // 自定义渲染与朝向控制的单位实体。
    public static class NoOutlineUnit extends UnitEntity{
        // 记录武器原始X，用于左右镜像。
        private static final IdentityHashMap<Weapon, Float> weaponBaseX = new IdentityHashMap<>();
        private boolean lastXInited = false;
        private float lastX = 0f;
        private boolean facingLeft = false;

        // 创建单位实体。
        public static UnitEntity create(){
            return new NoOutlineUnit();
        }

        @Override
        // 加入战场时初始化位置追踪。
        public void add(){
            super.add();
            lastXInited = true;
            lastX = x;
        }

        @Override
        // 每帧更新渲染状态与武器镜像。
        public void update(){
            super.update();
            CustomUnitRenderAPI.update(this);
            updateFacingByPosition();
            mirrorWeaponX();
        }

        @Override
        // 走自定义渲染流程，非配置单位回退原版渲染。
        public void draw(){
            if(type == null) return;
            if(CustomUnitRenderAPI.hasConfig(type)){
                CustomUnitRenderAPI.draw(this);
                return;
            }
            super.draw();
        }

        // 根据坐标变化更新面向。
        private void updateFacingByPosition(){
            if(!isSkyLaser()) return;
            if(!lastXInited){
                lastXInited = true;
                lastX = x;
                return;
            }
            float dx = x - lastX;
            lastX = x;
            if(Math.abs(dx) > 0.02f){
                facingLeft = dx < 0f;
            }
        }

        // 依据面向镜像武器挂点位置。
        private void mirrorWeaponX(){
            if(!isSkyLaser() || type == null || type.weapons == null) return;
            for(int i = 0; i < type.weapons.size; i++){
                Weapon weapon = type.weapons.get(i);
                if(weapon == null) continue;

                Float baseX = weaponBaseX.get(weapon);
                if(baseX == null){
                    baseX = weapon.x;
                    weaponBaseX.put(weapon, baseX);
                }

                float absX = Math.abs(baseX);
                weapon.x = facingLeft ? -absX : absX;
            }
        }

        // 判断是否为 skyLaser 单位。
        private boolean isSkyLaser(){
            return type != null && "skyLaser".equals(type.name);
        }
    }

    // 注册单位与渲染配置。
    public static void load(){
        warriorUnits.clear();
        tankUnits.clear();
        mageUnits.clear();
        healerUnits.clear();
        specialUnits.clear();

        initCareerTreeNodes();

        CustomUnitRenderAPI.register("skyLaser",
            new CustomUnitRenderAPI.RenderConfig(
                "skyLaser",
                "skyLaser-left",
                "skyLaser-attack",
                "skyLaser-left-attack",
                "skyLaser-attack-left"
            )
        );

        skyLaser = new SkyLaserUnitType("skyLaser", createMagicCircleShoot());
        registerUnit(skyLaser);
    }

    // 初始化职业树的展示节点，不参与战斗，仅用于科技树分类显示。
    private static void initCareerTreeNodes(){
        careerRootNode = new FLUnitType("career-root", FLUnitType.careerSpecial, "职业总览");
        careerRootNode.localizedName = "职业总览";
        careerRootNode.description = "塔洛莉单位按职业划分的独立研究分支。";
        careerRootNode.hideDatabase = true;

        warriorCareerNode = new FLWarriorUnitType("career-warrior");
        warriorCareerNode.localizedName = "战士";
        warriorCareerNode.description = "近战与突击定位单位。";
        warriorCareerNode.hideDatabase = true;

        tankCareerNode = new FLTankUnitType("career-tank");
        tankCareerNode.localizedName = "坦克";
        tankCareerNode.description = "高生存与前排承伤单位。";
        tankCareerNode.hideDatabase = true;

        mageCareerNode = new FLMageUnitType("career-mage");
        mageCareerNode.localizedName = "魔法师";
        mageCareerNode.description = "法术输出与远程压制单位。";
        // 在核心数据库中保留“魔法师”分类入口。
        mageCareerNode.hideDatabase = false;
        mageCareerNode.alwaysUnlocked = true;

        healerCareerNode = new FLHealerUnitType("career-healer");
        healerCareerNode.localizedName = "治疗术士";
        healerCareerNode.description = "治疗、增益与续航支援单位。";
        // 在核心数据库中保留“治疗术士”分类入口。
        healerCareerNode.hideDatabase = false;
        healerCareerNode.alwaysUnlocked = true;

        specialCareerNode = new FLSpecialUnitType("career-special");
        specialCareerNode.localizedName = "特殊职业";
        specialCareerNode.description = "机制独特的专精单位。";
        specialCareerNode.hideDatabase = true;

        // 职业占位节点也绑定到塔洛莉数据库页签。
        bindToFantaloryDatabase(mageCareerNode);
        bindToFantaloryDatabase(healerCareerNode);
    }

    // 按职业注册单位，供科技树与后续扩展复用。
    private static void registerUnit(FLUnitType type){
        if(type == null) return;
        // 模组单位显示在塔洛莉星球数据库页签中。
        bindToFantaloryDatabase(type);

        switch(type.careerId){
            case FLUnitType.careerWarrior:
                warriorUnits.add(type);
                break;
            case FLUnitType.careerTank:
                tankUnits.add(type);
                break;
            case FLUnitType.careerMage:
                mageUnits.add(type);
                break;
            case FLUnitType.careerHealer:
                healerUnits.add(type);
                break;
            case FLUnitType.careerSpecial:
                specialUnits.add(type);
                break;
            default:
                specialUnits.add(type);
                break;
        }
    }

    // 绑定内容到塔洛莉数据库页签。
    private static void bindToFantaloryDatabase(FLUnitType type){
        if(type == null || FLPlanets.fantaloryPlanet == null) return;
        type.databaseTabs.add(FLPlanets.fantaloryPlanet);
    }

    // 按职业生成塔洛莉科技树的单位分支。
    public static void loadTechTreeByCareer(){
        node(careerRootNode, () -> {
            // 五个职业固定作为职业总览的子节点，后续单位各自挂在对应职业下。
            node(warriorCareerNode, () -> addCareerUnitNodes(warriorUnits));
            node(tankCareerNode, () -> addCareerUnitNodes(tankUnits));
            node(mageCareerNode, () -> addCareerUnitNodes(mageUnits));
            node(healerCareerNode, () -> addCareerUnitNodes(healerUnits));
            node(specialCareerNode, () -> addCareerUnitNodes(specialUnits));
        });
    }

    // 把职业下单位依次挂载为子节点；无单位时仅显示职业节点本身。
    private static void addCareerUnitNodes(Seq<FLUnitType> units){
        if(units == null || units.isEmpty()) return;
        for(int i = 0; i < units.size; i++){
            node(units.get(i));
        }
    }

    // 创建开火魔法阵特效。
    private static Effect createMagicCircleShoot(){
        return new Effect(30f, e -> {
            float size = Vars.tilesize * 5f;
            float radius = size * 0.5f;
            float fin = e.fin();
            float fout = e.fout();
            float rot = e.rotation + fin * 120f;

            Draw.color(Color.valueOf("7fffd4"), Color.valueOf("ffb347"), fin);
            Lines.stroke(1.6f * fout);
            Lines.circle(e.x, e.y, radius * (0.85f + 0.15f * fin));
            Lines.circle(e.x, e.y, radius * 0.55f);
            Lines.circle(e.x, e.y, radius * 0.2f);
            Lines.poly(e.x, e.y, 3, radius * 0.6f, rot);
            Lines.poly(e.x, e.y, 4, radius * 0.38f, -rot);

            Draw.color(Color.white, fout);
            Fill.square(e.x, e.y, radius * 0.06f, rot);
            Draw.reset();
        });
    }

    // 创建 skyLaser 主子弹与两级分裂子弹。
    private static MagicBasicBulletType createSkyLaserBullet(Effect shootEffect){
        MagicBasicBulletType secondFrag = new MagicBasicBulletType(3f, 12f);
        secondFrag.lifetime = 22f;
        secondFrag.width = 3f;
        secondFrag.height = 3f;
        secondFrag.trailLength = 6;
        secondFrag.trailWidth = 1.6f;
        secondFrag.frontColor = Color.white;
        secondFrag.backColor = Color.valueOf("ffe082");
        secondFrag.trailColor = secondFrag.backColor;
        secondFrag.hitEffect = Fx.hitBulletSmall;
        secondFrag.despawnEffect = Fx.hitBulletSmall;

        MagicBasicBulletType firstFrag = new MagicBasicBulletType(4.5f, 28f);
        firstFrag.lifetime = 36f;
        firstFrag.width = 4f;
        firstFrag.height = 4f;
        firstFrag.homingPower = 0.18f;
        firstFrag.homingRange = 140f;
        firstFrag.trailLength = 10;
        firstFrag.trailWidth = 2.2f;
        firstFrag.frontColor = Color.valueOf("b3e5ff");
        firstFrag.backColor = Color.valueOf("4fc3f7");
        firstFrag.trailColor = firstFrag.backColor;
        firstFrag.hitEffect = Fx.hitBulletColor;
        firstFrag.despawnEffect = Fx.hitBulletSmall;
        firstFrag.fragBullets = 1;
        firstFrag.fragBullet = secondFrag;

        MagicBasicBulletType main = new MagicBasicBulletType(6f, 95f);
        main.lifetime = 60f;
        main.width = 6f;
        main.height = 6f;
        main.trailLength = 15;
        main.trailWidth = 3f;
        main.frontColor = Color.valueOf("ffb347");
        main.backColor = Color.valueOf("ff4500");
        main.trailColor = main.backColor;
        main.lightColor = Color.valueOf("ff8c00");
        main.lightRadius = 24f;
        main.lightOpacity = 0.9f;
        main.shootEffect = shootEffect;
        main.smokeEffect = Fx.none;
        main.hitEffect = Fx.hitBulletColor;
        main.despawnEffect = Fx.hitBulletColor;
        main.fragBullets = 5;
        main.fragBullet = firstFrag;
        return main;
    }

    // skyLaser 武器定义。
    private static class SkyLaserWeapon extends Weapon{
        // 初始化武器参数并绑定法术子弹。
        SkyLaserWeapon(Effect shootEffect){
            x = 30f;
            y = 30f;
            mirror = false;
            rotate = true;
            reload = 13f;
            inaccuracy = 0f;
            bullet = createSkyLaserBullet(shootEffect);
        }

        @Override
        // 重写武器数据库描述，显示法术伤害与分裂层级。
        public void addStats(UnitType u, Table t){
            if(inaccuracy > 0f){
                t.row();
                t.add("[lightgray]" + Stat.inaccuracy.localized() + ": [white]"
                    + (int)inaccuracy + " " + StatUnit.degrees.localized());
            }
            if(!alwaysContinuous && reload > 0f && bullet != null && !bullet.killShooter){
                t.row();
                t.add("[lightgray]" + Stat.reload.localized() + ": " + (mirror ? "2x " : "")
                    + "[white]" + Strings.autoFixed(60f / reload * shoot.shots, 2)
                    + " " + StatUnit.perSecond.localized());
            }
            if(bullet == null) return;

            t.row();
            t.add("[#8fe6ff]法术伤害[lightgray]: [white]" + Strings.autoFixed(bullet.damage, 2));

            if(bullet.fragBullet != null && bullet.fragBullets > 0){
                t.row();
                t.add("[lightgray]分裂子弹: [white]" + bullet.fragBullets);
                t.row();
                t.add("[#8fe6ff]法术伤害[lightgray]: [white]"
                    + Strings.autoFixed(bullet.fragBullet.damage, 2));

                if(bullet.fragBullet.fragBullet != null && bullet.fragBullet.fragBullets > 0){
                    t.row();
                    t.add("[lightgray]二级分裂子弹: [white]" + bullet.fragBullet.fragBullets);
                    t.row();
                    t.add("[#8fe6ff]法术伤害[lightgray]: [white]"
                        + Strings.autoFixed(bullet.fragBullet.fragBullet.damage, 2));
                }
            }
        }
    }

    // 固定单位朝向为正上方的AI。
    private static class SkyLaserCommandAI extends CommandAI{
        @Override
        // 更新AI时强制单位朝向。
        public void updateUnit(){
            if(unit != null) unit.rotation = 90f;
            super.updateUnit();
        }
    }

    // skyLaser 单位类型定义。
    private static class SkyLaserUnitType extends FLMageUnitType{
        // 初始化单位参数与武器。
        SkyLaserUnitType(String name, Effect shootEffect){
            super(name);
            constructor = NoOutlineUnit::create;

            outlines = false;
            outlineRadius = 0;
            outlineColor = Color.clear;
            flying = true;
            range = 300f;
            hitSize = 48f;
            health = 12000f;
            armor = 10f;
            speed = 3f;
            rotateSpeed = 0f;
            accel = 0.12f;
            drag = 0.04f;
            faceTarget = false;

            weapons.add(new SkyLaserWeapon(shootEffect));
            controller = unit -> new SkyLaserCommandAI();
        }

        @Override
        // 创建单位时写入法术抗性。
        public Unit create(Team team){
            Unit unit = super.create(team);
            MagicDamageAPI.setUnitResistance(unit, 20f);
            return unit;
        }

        @Override
        // 在单位通用属性中显示法术抗性。
        public void setStats(){
            super.setStats();
            stats.add(magicResistStat, 20f, StatUnit.percent);
        }
    }
}
