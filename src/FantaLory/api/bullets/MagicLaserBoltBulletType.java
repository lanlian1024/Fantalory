package FantaLory.api.bullets;

import mindustry.entities.bullet.LaserBoltBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Building;
import mindustry.gen.Hitboxc;

public class MagicLaserBoltBulletType extends LaserBoltBulletType{
    public MagicLaserBoltBulletType(float speed, float damage){
        super(speed, damage);
        MagicBulletSupport.setupDefaults(this);
    }

    public MagicLaserBoltBulletType(){
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
