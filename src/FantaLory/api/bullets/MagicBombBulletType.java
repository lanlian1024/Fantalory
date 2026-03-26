package FantaLory.api.bullets;

import mindustry.entities.bullet.BombBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Building;
import mindustry.gen.Hitboxc;

public class MagicBombBulletType extends BombBulletType{
    public MagicBombBulletType(float damage, float radius, String sprite){
        super(damage, radius, sprite);
        MagicBulletSupport.setupDefaults(this);
    }

    public MagicBombBulletType(float damage, float radius){
        super(damage, radius);
        MagicBulletSupport.setupDefaults(this);
    }

    public MagicBombBulletType(){
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
