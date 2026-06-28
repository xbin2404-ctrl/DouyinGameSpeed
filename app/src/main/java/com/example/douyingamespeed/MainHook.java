package com.douyin.game.speed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    
    private static final float SPEED_MULTIPLIER = 2.0f;
    private static long baseTime = 0;
    private static long baseRealTime = 0;
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.ss.android.ugc.aweme")) {
            return;
        }
        
        // 只 Hook System.currentTimeMillis() - 游戏最常用的计时
        XposedHelpers.findAndHookMethod("java.lang.System", lpparam.classLoader, 
            "currentTimeMillis", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (baseTime == 0) {
                        baseTime = System.currentTimeMillis();
                        baseRealTime = android.os.SystemClock.elapsedRealtime();
                    }
                    long realElapsed = android.os.SystemClock.elapsedRealtime() - baseRealTime;
                    param.setResult(baseTime + (long)(realElapsed * SPEED_MULTIPLIER));
                }
            });
    }
}
