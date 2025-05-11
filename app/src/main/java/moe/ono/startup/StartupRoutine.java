package moe.ono.startup;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;

import moe.ono.HostInfo;
import moe.ono.core.NativeCoreBridge;
import moe.ono.core.QLauncher;
import moe.ono.hookimpl.InMemoryClassLoaderHelper;
import moe.ono.hookimpl.LibXposedNewApiByteCodeGenerator;
import moe.ono.service.inject.ServletPool;
import moe.ono.util.Initiator;
import moe.ono.util.Logger;
import moe.ono.util.SyncUtils;

public class StartupRoutine {

    private StartupRoutine() {
        throw new AssertionError("No instance for you!");
    }

    /**
     * From now on, kotlin, androidx or third party libraries may be accessed without crashing the ART.
     * <p>
     * Kotlin and androidx are dangerous, and should be invoked only after the class loader is ready.
     *
     * @param ctx         Application context for host
     * @param step        Step instance
     * @param lpwReserved null, not used
     * @param bReserved   false, not used
     */
    public static void execPostStartupInit(@NonNull Context ctx, @Nullable Object step, String lpwReserved, boolean bReserved) {
        // init all kotlin utils here
        HostInfo.init((Application) ctx);
        Initiator.init(ctx.getClassLoader());

        overrideLSPatchModifiedVersionCodeIfNecessary(ctx);
        // perform full initialization for native core -- including primary and secondary native libraries
        StartupInfo.getLoaderService().setClassLoaderHelper(InMemoryClassLoaderHelper.INSTANCE);
        LibXposedNewApiByteCodeGenerator.init();
        NativeCoreBridge.initNativeCore();
        ServletPool.INSTANCE.injectServlet();

        // ------------------------------------------

        Logger.d("execPostStartupInit -> processName: " + SyncUtils.getProcessName());
        QLauncher launcher = new QLauncher();
        launcher.init(ctx.getClassLoader(), ctx.getApplicationInfo(), ctx.getApplicationInfo().sourceDir, ctx);
    }

    private static void overrideLSPatchModifiedVersionCodeIfNecessary(Context ctx) {
        if (HostInfo.isInHostProcess() && HostInfo.getHostInfo().getVersionCode32() == 1) {
            if ("com.tencent.mobileqq".equals(ctx.getPackageName())) {
                ClassLoader cl = ctx.getClassLoader();
                // try to get version code from Lcooperation/qzone/QUA;->QUA:Ljava/lang/String;
                try {
                    Class<?> kQUA = cl.loadClass("cooperation.qzone.QUA");
                    Field QUA = kQUA.getDeclaredField("QUA");
                    QUA.setAccessible(true);
                    String qua = (String) QUA.get(null);
                    if (qua != null && qua.startsWith("V1_AND_")) {
                        // "V1_AND_SQ_8.9.0_3060_YYB_D"
                        String[] split = qua.split("_");
                        if (split.length >= 5) {
                            int versionCode = Integer.parseInt(split[4]);
                            HostInfo.overrideVersionCodeForLSPatchModified1(versionCode);
                        }
                    }
                } catch (ReflectiveOperationException | NumberFormatException e) {
                    Logger.e("Failed to get version code from Lcooperation/qzone/QUA;->QUA:Ljava/lang/String;", e);
                }
            }
        }
    }


}
