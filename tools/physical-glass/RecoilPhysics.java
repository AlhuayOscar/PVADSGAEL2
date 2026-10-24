package pvads;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import se.krka.kahlua.vm.KahluaTable;
public final class RecoilPhysics {
    private static volatile float attachmentRecoil=1, attachmentRecovery=1;
    private static long shotAt;
    public static void profile(KahluaTable table){
        attachmentRecoil=table==null?1:Math.max(0.25f,Math.min(1.5f,Api.f(table,"attachmentRecoilMul",1)));
        attachmentRecovery=table==null?1:Math.max(1,Math.min(1.25f,Api.f(table,"attachmentRecoveryMul",1)));
    }
    public static float strengthScale(int level) {
        int s=Math.max(0,Math.min(10,level));
        double old=s>=5?Math.pow(0.60,s-5):1+(5-s)*0.40;
        return (float)(old/1.5); // +50% recoil control at every strength level.
    }
    public static float shotScale(float amount) {
        shotAt=System.nanoTime();
        IsoPlayer player=IsoPlayer.players[0];
        if(player==null || ADS.P==null)return amount;
        int strength=player.getPerkLevel(PerkFactory.Perks.Strength);
        int aiming=player.getPerkLevel(PerkFactory.Perks.Aiming);
        return amount*strengthScale(strength)*attachmentRecoil/(1+0.08f*Math.max(0,Math.min(10,aiming)));
    }
    public static void stepSprings(float dt) {
        if(!Float.isFinite(dt)||dt<=0)return;
        dt=Math.min(0.1f,dt);
        // Recovery only: let the initial 75 ms of the shot keep its original impulse.
        float age=(System.nanoTime()-shotAt)*1e-9f;
        float ramp=Math.max(0,Math.min(1,(age-0.075f)/0.1f));
        ramp=ramp*ramp*(3-2*ramp);
        dt*=1+(attachmentRecovery-1)*ramp;
        int steps=Math.max(1,(int)Math.ceil(dt*240));float h=dt/steps;
        for(int i=0;i<steps;i++){
            ADS.vKickV+=(-160*ADS.vKick-22*ADS.vKickV)*h;ADS.vKick+=ADS.vKickV*h;
            ADS.hKickV+=(-160*ADS.hKick-22*ADS.hKickV)*h;ADS.hKick+=ADS.hKickV*h;
            ADS.gKickV+=(-220*ADS.gKick-26*ADS.gKickV)*h;ADS.gKick+=ADS.gKickV*h;
            ADS.gBackV+=(-220*ADS.gBack-26*ADS.gBackV)*h;ADS.gBack+=ADS.gBackV*h;
        }
    }
}
