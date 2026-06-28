package com.example.douyingamespeed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainHook implements IXposedHookLoadPackage {

    private static final float SPEED = 2.0f;

    // 记录模块加载时的真实时间作为基准
    private static final long T0_REAL = System.currentTimeMillis();
    private static final long T0_NANO = System.nanoTime();

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 只 hook 抖音主进程
        if (!lpparam.packageName.equals("com.ss.android.ugc.aweme")) {
            return;
        }

        XposedBridge.log("=== DouyinGameSpeed module loaded for " + lpparam.packageName + " ===");

        // 1) System.currentTimeMillis() - 墙钟时间
        XposedHelpers.findAndHookMethod("java.lang.System", lpparam.classLoader,
            "currentTimeMillis", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    long real = System.currentTimeMillis();
                    param.setResult(T0_REAL + (long)((real - T0_REAL) * SPEED));
                }
            });

        // 2) System.nanoTime() - 单调时间
        XposedHelpers.findAndHookMethod("java.lang.System", lpparam.classLoader,
            "nanoTime", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    long real = System.nanoTime();
                    param.setResult(T0_NANO + (long)((real - T0_NANO) * SPEED));
                }
            });

        // 3) SystemClock.uptimeMillis() - 开机时间(不含睡眠)
        final long t0Boot = android.os.SystemClock.uptimeMillis();
        XposedHelpers.findAndHookMethod("android.os.SystemClock", lpparam.classLoader,
            "uptimeMillis", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    long real = android.os.SystemClock.uptimeMillis();
                    param.setResult(t0Boot + (long)((real - t0Boot) * SPEED));
                }
            });

        // 4) SystemClock.elapsedRealtime() - 经过时间(含睡眠)
        final long t0Elapsed = android.os.SystemClock.elapsedRealtime();
        XposedHelpers.findAndHookMethod("android.os.SystemClock", lpparam.classLoader,
            "elapsedRealtime", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    long real = android.os.SystemClock.elapsedRealtime();
                    param.setResult(t0Elapsed + (long)((real - t0Elapsed) * SPEED));
                }
            });

        // 5) java.util.Date.getTime() - 跟随 currentTimeMillis
        XposedHelpers.findAndHookMethod("java.util.Date", lpparam.classLoader,
            "getTime", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    long real = System.currentTimeMillis();
                    param.setResult(T0_REAL + (long)((real - T0_REAL) * SPEED));
                }
            });

        // 6) WebView.evaluateJavascript 拦截(只记录日志,不修改脚本,避免破坏游戏)
        try {
            XposedHelpers.findAndHookMethod("android.webkit.WebView", lpparam.classLoader,
                "evaluateJavascript", String.class, android.webkit.ValueCallback.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String script = (String) param.args[0];
                        if (script != null && (script.contains("Date.now") || script.contains("performance.now"))) {
                            XposedBridge.log("DouyinGameSpeed: intercepted time call in WebView: " +
                                (script.length() > 80 ? script.substring(0, 80) + "..." : script));
                        }
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("DouyinGameSpeed: WebView hook skipped: " + t.getMessage());
        }

        XposedBridge.log("=== DouyinGameSpeed hooks installed (SPEED=" + SPEED + "x) ===");
    }
}
