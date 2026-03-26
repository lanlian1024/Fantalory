package FantaLory.content.blocks;

import FantaLory.api.bullets.MagicBasicBulletType;
import FantaLory.content.FLLiquids;
import FantaLory.content.items.FLItems;
import FantaLory.world.blocks.mana.ManaState;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.ui.Label;
import arc.util.Strings;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.part.DrawPart.PartProgress;
import mindustry.entities.part.RegionPart;
import mindustry.entities.pattern.ShootAlternate;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.draw.DrawTurret;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValues;

public class FLTurret{
    // 星晶炮台的魔力容量与每发消耗。
    static final float starcrystalManaCapacity = 30f;
    static final float starcrystalManaPerShot = 2f;
    static final Color starcrystalManaColor = Color.valueOf("8fe6ff");

    private static final String damageKey = "伤害";
    private static final String buildingText = "建筑";
    private static final String buildingObjectText = "建筑物";
    private static final String blockText = "方块";
    private static final String magicDamageText = "[#8fe6ff]法术伤害[]";
    static final String manaText = "魔力";

    public static ItemTurret starcrystal, pixelTurret;

    // 注册模组炮台。
    public static void load(){
        starcrystal = new StarcrystalTurret("starcrystal");
        pixelTurret = new PixelTurret("pixel-turret");
    }

    // 归一化文本，便于识别描述行。
    private static String normalizeStatText(String text){
        if(text == null) return "";
        return text
            .replaceAll("\\[[^\\]]*\\]", "")
            .replace(" ", "")
            .replace(" ", "")
            .toLowerCase();
    }

    // 将弹药描述中的普通伤害替换为法术伤害，并隐藏建筑倍率行。
    static void rewriteDamageText(Element element){
        if(element instanceof Label){
            Label label = (Label)element;
            String text = String.valueOf(label.getText());

            String compact = normalizeStatText(text);
            boolean cnBuildingDamage = compact.contains("0x")
                && compact.contains(damageKey)
                && (compact.contains(buildingText) || compact.contains(buildingObjectText) || compact.contains(blockText));
            boolean enBuildingDamage = compact.contains("0x")
                && compact.contains("damage")
                && compact.contains("building");

            if(cnBuildingDamage || enBuildingDamage){
                label.remove();
                return;
            }

            if(text.contains(damageKey) && !text.contains("法术伤害")){
                label.setText(text.replace(damageKey, magicDamageText));
            }
        }
        if(element instanceof Group){
            Group group = (Group)element;
            for(Element child : group.getChildren()){
                rewriteDamageText(child);
            }
        }
    }

    // 创建基础炮使用的普通子弹。
    static BasicBulletType createPixelBullet(){
        BasicBulletType bullet = new BasicBulletType(5f, 11f);
        bullet.width = 6f;
        bullet.height = 6f;
        bullet.lifetime = 60f;
        bullet.trailLength = 8;
        bullet.trailWidth = 1.8f;
        bullet.shootEffect = Fx.shootSmall;
        bullet.smokeEffect = Fx.shootSmallSmoke;
        bullet.hitEffect = Fx.hitBulletSmall;
        bullet.despawnEffect = Fx.hitBulletSmall;
        bullet.frontColor = Color.white;
        bullet.backColor = Color.valueOf("ffd700");
        bullet.trailColor = bullet.backColor;
        return bullet;
    }

    // 创建星晶炮秘银弹（法术伤害）。
    static MagicBasicBulletType createMithrilMagicBullet(){
        MagicBasicBulletType bullet = new MagicBasicBulletType(6f, 60f);
        bullet.damage = 30f;
        bullet.speed = 20f;
        bullet.width = 10f;
        bullet.height = 14f;
        bullet.lifetime = 15f;
        bullet.ammoMultiplier = 4f;
        bullet.homingPower = 0.2f;
        bullet.homingRange = 150f;
        bullet.hitColor = Color.gray.cpy().mul(1.2f);
        bullet.backColor = bullet.hitColor;
        bullet.trailColor = bullet.hitColor;
        bullet.frontColor = Color.white.cpy().add(0.2f, 0.2f, 0.2f);
        bullet.trailLength = 16;
        bullet.trailWidth = 4f;
        bullet.hitEffect = Fx.hitLancer;
        bullet.despawnEffect = Fx.hitBulletColor;
        return bullet;
    }

    // 创建星晶炮相织布弹（法术伤害）。
    static MagicBasicBulletType createPhaseMagicBullet(){
        MagicBasicBulletType bullet = new MagicBasicBulletType(7f, 90f);
        bullet.damage = 45f;
        bullet.speed = 20f;
        bullet.width = 12f;
        bullet.height = 16f;
        bullet.lifetime = 15f;
        bullet.ammoMultiplier = 3f;
        bullet.reloadMultiplier = 0.9f;
        bullet.homingPower = 0.2f;
        bullet.homingRange = 200f;
        bullet.pierceCap = 2;
        bullet.pierceBuilding = true;
        bullet.hitColor = Color.valueOf("87CEEB").cpy().a(0.8f);
        bullet.backColor = bullet.hitColor;
        bullet.trailColor = bullet.hitColor;
        bullet.frontColor = Color.white;
        bullet.trailLength = 24;
        bullet.trailWidth = 5f;
        bullet.hitEffect = Fx.blastExplosion;
        bullet.despawnEffect = Fx.hitBulletColor;
        bullet.splashDamage = 25f;
        bullet.splashDamageRadius = 30f;
        return bullet;
    }
}

// 星晶炮台本体定义。
class StarcrystalTurret extends ItemTurret{
    // 初始化星晶炮台参数。
    StarcrystalTurret(String name){
        super(name);
        requirements(Category.turret, ItemStack.with(
            Items.copper, 200,
            Items.silicon, 150,
            Items.phaseFabric, 80,
            Items.thorium, 100
        ));
        localizedName = "星晶";
        size = 3;
        health = 800;
        range = 280f;
        reload = 20f;
        recoil = 3f;
        rotateSpeed = 8f;
        inaccuracy = 60f;
        shootCone = 8f;
        shootY = 6f;
        shake = 2f;
        shootSound = Sounds.shootMissileLong;
        shootSoundVolume = 0.8f;
        coolant = consumeCoolant(0.2f);
        coolantMultiplier = 8f;
        depositCooldown = 2f;

        buildType = () -> new StarcrystalTurretBuild(this);
        ammo(
            FLItems.mithril, FLTurret.createMithrilMagicBullet(),
            Items.phaseFabric, FLTurret.createPhaseMagicBullet()
        );
        drawer = new DrawTurret("reinforced-");
    }

    @Override
    // 注册星晶炮台魔力条。
    public void setBars(){
        super.setBars();
        addBar("mana", (ItemTurretBuild b) -> new Bar(
            () -> FLTurret.manaText + " " + Strings.autoFixed(b.tile == null ? 0f : ManaState.get(b.tile), 1)
                + "/" + Strings.autoFixed(b.tile == null ? FLTurret.starcrystalManaCapacity : ManaState.capacity(b.tile), 1),
            () -> FLTurret.starcrystalManaColor,
            () -> b.tile == null ? 0f : ManaState.fraction(b.tile)
        ));
    }

    @Override
    // 重写弹药描述渲染。
    public void setStats(){
        super.setStats();
        // 星晶炮台按每发显示耗魔量（并入输入项）。
        stats.add(Stat.input, StatValues.string("[#8fe6ff]魔力[]消耗：@/发", Strings.autoFixed(FLTurret.starcrystalManaPerShot, 2)));
        stats.remove(Stat.ammo);
        stats.add(Stat.ammo, table -> {
            StatValues.ammo(ammoTypes).display(table);
            FLTurret.rewriteDamageText(table);
        });
    }
}

// 星晶炮台建筑实体逻辑。
class StarcrystalTurretBuild extends ItemTurret.ItemTurretBuild{
    // 绑定外部炮台类型。
    StarcrystalTurretBuild(StarcrystalTurret turret){
        turret.super();
    }

    @Override
    // 仅接收低纯魔液。
    public boolean acceptLiquid(Building source, Liquid liquid){
        if(liquid == FLLiquids.mana){
            return liquids != null && liquids.get(liquid) < block.liquidCapacity - 0.0001f;
        }
        return super.acceptLiquid(source, liquid);
    }

    @Override
    // 放置时初始化魔力缓存。
    public void placed(){
        super.placed();
        if(tile != null){
            ManaState.init(tile, FLTurret.starcrystalManaCapacity, 0f);
        }
    }

    @Override
    // 拆除时清理魔力缓存。
    public void onRemoved(){
        if(tile != null){
            ManaState.remove(tile);
        }
        super.onRemoved();
    }

    @Override
    // 仅在魔力足够时允许消耗与开火。
    public boolean canConsume(){
        return tile != null && super.canConsume() && ManaState.get(tile) >= FLTurret.starcrystalManaPerShot;
    }

    @Override
    // 每帧把魔液转入魔力缓存。
    public void updateTile(){
        ManaState.fillFromLiquid(this, FLLiquids.mana);
        super.updateTile();
    }

    @Override
    // 开火前先扣除魔力。
    protected void shoot(BulletType type){
        if(tile == null) return;
        if(!ManaState.consume(tile, FLTurret.starcrystalManaPerShot)) return;
        super.shoot(type);
    }
}

// 基础炮台定义。
class PixelTurret extends ItemTurret{
    // 初始化基础炮参数与绘制部件。
    PixelTurret(String name){
        super(name);
        requirements(Category.turret, ItemStack.with(Items.copper, 10, Items.lead, 5));
        localizedName = "基础炮";
        size = 1;
        health = 450;
        rotateSpeed = 8f;
        reload = 60f;
        range = 120f;
        recoil = 2f;
        ammo(FLItems.mithril, FLTurret.createPixelBullet());

        DrawTurret pixelDrawer = new DrawTurret();
        RegionPart midPart = new RegionPart("-mid");
        midPart.progress = PartProgress.recoil;
        midPart.under = false;
        midPart.moveY = -1.25f;
        pixelDrawer.parts.add(midPart);
        drawer = pixelDrawer;

        shoot = new ShootAlternate(3.5f);
    }
}
