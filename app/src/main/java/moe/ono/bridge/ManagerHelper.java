package moe.ono.bridge;

import static moe.ono.util.Initiator.load;
import static moe.ono.util.Initiator.loadClass;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import moe.ono.reflex.Reflex;
import moe.ono.util.AppRuntimeHelper;
import moe.ono.util.Initiator;
import moe.ono.util.IoUtils;
import moe.ono.util.Logger;
import mqq.app.AppRuntime;

public class ManagerHelper {

    private ManagerHelper() {
    }

    public static Object getTroopManager() throws Exception {
        int troopMgrId = -1;
        Class<?> cl_QQManagerFactory = load("com.tencent.mobileqq.app.QQManagerFactory");
        try {
            if (cl_QQManagerFactory != null) {
                troopMgrId = (int) cl_QQManagerFactory.getField("TROOP_MANAGER").get(null);
            }
        } catch (Throwable e) {
            Logger.e(e);
        }
        if (troopMgrId != -1) {
            // >=8.4.10
            return getManager(troopMgrId);
        } else {
            // 8.4.8 or earlier
            Object mTroopManager = getManager(51);
            if (!mTroopManager.getClass().getName().contains("TroopManager")) {
                mTroopManager = getManager(52);
            }
            return mTroopManager;
        }
    }

    public static Object getQQMessageFacade() throws ReflectiveOperationException {
        AppRuntime app = AppRuntimeHelper.getQQAppInterface();
        return Reflex.invokeVirtualAny(Objects.requireNonNull(app), Initiator._QQMessageFacade());
    }

    public static Object getManager(int index) throws ReflectiveOperationException {
        return Reflex.invokeVirtual(Objects.requireNonNull(AppRuntimeHelper.getQQAppInterface()), "getManager", index, int.class);
    }

    public static Object getFriendListHandler() {
        try {
            Object appInterface = AppRuntimeHelper.getQQAppInterface();
            // BusinessHandler is likely to be obfuscated
            Class<?> kBusinessHandler = load("com/tencent/mobileqq/app/BusinessHandler");
            if (kBusinessHandler == null) {
                // QQ lite 3.5.0, has obfuscated FriendListHandler, other versions look good
                kBusinessHandler = loadClass("com/tencent/mobileqq/app/FriendListHandler").getSuperclass();
            }
            Method getBusinessHandler = null;
            for (Method m : Initiator._QQAppInterface().getMethods()) {
                // public, non-static
                if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                // For 8.5.0+, getBusinessHandler use string as parameter
                if (m.getName().equals("getBusinessHandler")) {
                    getBusinessHandler = m;
                    break;
                }
                if (m.getReturnType() == kBusinessHandler && "a".equals(m.getName())) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 1 && (params[0] == int.class || params[0] == String.class)) {
                        getBusinessHandler = m;
                        break;
                    }
                }
            }
            if (getBusinessHandler == null) {
                throw new NoSuchMethodException("QQAppInterface.getBusinessHandler");
            }
            Class<?> type = getBusinessHandler.getParameterTypes()[0];
            if (type == String.class) {
                return getBusinessHandler.invoke(appInterface, loadClass("com/tencent/mobileqq/app/FriendListHandler").getName());
            } else {
                return getBusinessHandler.invoke(appInterface, 1);
            }
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw IoUtils.unsafeThrowForIteCause(e);
        }
    }

    @Deprecated
    public static Object getBusinessHandler(int type) {
        try {
            Class cl_bh = load("com/tencent/mobileqq/app/BusinessHandler");
            if (cl_bh == null) {
                Class cl_flh = load("com/tencent/mobileqq/app/FriendListHandler");
                assert cl_flh != null;
                cl_bh = cl_flh.getSuperclass();
            }
            Object appInterface = AppRuntimeHelper.getQQAppInterface();
            try {
                return Reflex.invokeVirtual(appInterface, "a", type, int.class, cl_bh);
            } catch (NoSuchMethodException e) {
                Method m = appInterface.getClass().getMethod("getBusinessHandler", int.class);
                m.setAccessible(true);
                return m.invoke(appInterface, type);
            }
        } catch (Exception e) {
            Logger.e(e);
            return null;
        }
    }
}
