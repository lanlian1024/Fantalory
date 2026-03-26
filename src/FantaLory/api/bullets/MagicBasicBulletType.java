package FantaLory.api.bullets;

import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Building;
import mindustry.gen.Hitboxc;

public class MagicBasicBulletType extends BasicBulletType{
    public MagicBasicBulletType(float speed, float damage){
        super(speed, damage);
        MagicBulletSupport.setupDefaults(this);
    }

    public MagicBasicBulletType(float speed, float damage, String sprite){
        super(speed, damage, sprite);
        MagicBulletSupport.setupDefaults(this);
    }

    public MagicBasicBulletType(){
        super();
        MagicBulletSupport.setupDefaults(this);
    }

    @Override
    public void removed(Bullet b){
        MagicBulletSupport.onRemoved(this, b);
        super.removed(b);
    }

    @Override
    public void hitEntity(Bullet b, Hitboxc entity, float health){
        MagicBulletSupport.onHitEntity(this, b, entity, health);
    }

    @Override
    public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
        MagicBulletSupport.onHitTile(this, b, build, x, y, initialHealth, direct);
    }
}
