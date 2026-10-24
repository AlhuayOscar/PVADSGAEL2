package pvads;
public class VerifyGlassTiming {
    public static void main(String[] args) {
        Object gun=new Object(),other=new Object();long start=1_000_000_000L;
        check(!PhysicalGlass.visibilityDelay(true,gun,start),"start");
        check(!PhysicalGlass.visibilityDelay(true,gun,start+199_000_000L),"before 0.2s");
        check(PhysicalGlass.visibilityDelay(true,gun,start+200_000_000L),"at 0.2s");
        check(PhysicalGlass.visibilityDelay(true,gun,start+900_000_000L),"held ADS");
        check(!PhysicalGlass.visibilityDelay(false,gun,start+910_000_000L),"release");
        check(!PhysicalGlass.visibilityDelay(true,gun,start+920_000_000L),"restart");
        check(!PhysicalGlass.visibilityDelay(true,other,start+1_500_000_000L),"weapon switch");
        check(!PhysicalGlass.visibilityDelay(true,null,start+1_900_000_000L),"no weapon");
        check(Math.abs(PhysicalGlass.distanceStep(0,1)-0.005f)<1e-6,"wheel step unchanged");
        check(Math.abs(PhysicalGlass.distanceStep(0.074f,1)-0.075f)<1e-6,"upper half-range");
        check(Math.abs(PhysicalGlass.distanceStep(-0.074f,-1)+0.075f)<1e-6,"lower half-range");
        System.out.println("Glass timing OK: 0.2s, hold, release, restart and weapon-switch resets.");
    }
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
