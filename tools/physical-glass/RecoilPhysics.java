package pvads;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
public final class RecoilPhysics {
    public static float strengthScale(int level) {
        int s=Math.max(0,Math.min(10,level));
        double old=s>=5?Math.pow(0.60,s-5):1+(5-s)*0.40;
        return (float)(old/1.5); // +50% recoil control at every strength level.
    }
    public static float shotScale(float amount) {
        IsoPlayer player=IsoPlayer.players[0];
        if(player==null || ADS.P==null)return amount;
        int strength=player.getPerkLevel(PerkFactory.Perks.Strength);
        int aiming=player.getPerkLevel(PerkFactory.Perks.Aiming);
        return amount*strengthScale(strength)/(1+0.08f*Math.max(0,Math.min(10,aiming)));
    }
    public static void stepSprings(float dt) {
        if(!Float.isFinite(dt)||dt<=0)return;
        dt=Math.min(0.1f,dt);
        int steps=Math.max(1,(int)Math.ceil(dt*240));float h=dt/steps;
        for(int i=0;i<steps;i++){
            ADS.vKickV+=(-160*ADS.vKick-22*ADS.vKickV)*h;ADS.vKick+=ADS.vKickV*h;
            ADS.hKickV+=(-160*ADS.hKick-22*ADS.hKickV)*h;ADS.hKick+=ADS.hKickV*h;
            ADS.gKickV+=(-220*ADS.gKick-26*ADS.gKickV)*h;ADS.gKick+=ADS.gKickV*h;
            ADS.gBackV+=(-220*ADS.gBack-26*ADS.gBackV)*h;ADS.gBack+=ADS.gBackV*h;
        }
    }
}
