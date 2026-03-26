package FantaLory.api.bullets;

import mindustry.entities.bullet.LaserBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Building;
import mindustry.gen.Hitboxc;

public class MagicLaserBulletType extends LaserBulletType{
    public MagicLaserBulletType(float damage){
        super(damage);
        MagicBulletSupport.setupDefaults(this);
    }

    public MagicLaserBulletType(){
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
