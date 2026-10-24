package pvads;
public class VerifyRecoilPhysics {
    public static void main(String[] args){
        float previous=Float.MAX_VALUE;
        for(int level=0;level<=10;level++){
            float value=RecoilPhysics.strengthScale(level);
            double old=level>=5?Math.pow(0.6,level-5):1+(5-level)*0.4;
            if(Math.abs(value-old/1.5)>1e-6||value>=previous)throw new AssertionError("Strength level "+level);
            previous=value;
        }
        for(int fps:new int[]{10,15,30,60,144}){
            ADS.gKick=ADS.gBack=ADS.vKick=ADS.hKick=0;
            ADS.gKickV=10;ADS.gBackV=1;ADS.vKickV=ADS.hKickV=2;
            float peak=0;
            for(int i=0;i<fps*2;i++){
                RecoilPhysics.stepSprings(1f/fps);
                if(!Float.isFinite(ADS.gKick)||Math.abs(ADS.gKick)>1||ADS.gBack < -0.001f)throw new AssertionError("Unstable at "+fps);
                peak=Math.max(peak,ADS.gKick);
            }
            if(Math.abs(ADS.gKick)>0.001f||Math.abs(ADS.gBack)>0.001f)throw new AssertionError("No recovery at "+fps);
            System.out.println("Stable recoil at "+fps+" FPS; peak="+peak);
        }
        System.out.println("All strength levels have 1.5x control; spring recovery bounded across 10-144 FPS.");
    }
}
