package moe.ono.loader.modern.codegen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import io.github.libxposed.api.XposedInterface;
import moe.ono.loader.modern.Lsp100HookImpl;
import moe.ono.loader.modern.Lsp100HookWrapper;
import moe.ono.loader.modern.dyn.Lsp100CallbackProxy;
import moe.ono.loader.hookapi.IClassLoaderHelper;

public class Lsp100ProxyClassMaker {

    private static Lsp100ProxyClassMaker sInstance = null;

    private static ClassLoader sProxyClassLoader = null;

    private static Method sWrapperMethod = null;

    private static Throwable sLoadClassException = null;

    public Class<?> createProxyClass(int priority) {
        String className = getClassNameForPriority(priority);
        // is already loaded?
        try {
            return Lsp100ProxyClassMaker.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException ignored) {
        }
        if (sLoadClassException != null) {
            throw new UnsupportedOperationException("reject to try again due to previous exception", sLoadClassException);
        }
        if (sProxyClassLoader != null) {
            try {
                return sProxyClassLoader.loadClass(className);
            } catch (ClassNotFoundException ignored) {
            }
        }
        byte[] dex = makeClassByteCodeForPriority(priority);
        return loadProxyClassForPriority(className, dex, priority);
    }

    private Class<?> loadProxyClassForPriority(@NonNull String className, @NonNull byte[] dex, int priority) {
        IClassLoaderHelper helper = Lsp100HookImpl.INSTANCE.getClassLoaderHelper();
        if (helper == null) {
            throw new UnsupportedOperationException("ClassLoaderHelper not set");
        }
        if (sLoadClassException != null) {
            throw new UnsupportedOperationException("reject to try again due to previous exception", sLoadClassException);
        }
        if (sProxyClassLoader == null) {
            synchronized (Lsp100ProxyClassMaker.class) {
                if (sProxyClassLoader == null) {
                    sProxyClassLoader = helper.createEmptyInMemoryMultiDexClassLoader(Lsp100HookImpl.class.getClassLoader());
                }
            }
        }
        // already loaded?
        try {
            return sProxyClassLoader.loadClass(className);
        } catch (ClassNotFoundException ignored) {
        }
        // load dex
        helper.injectDexToClassLoader(sProxyClassLoader, dex, null);
        // load class
        Class<?> proxyClass;
        try {
            // force a resolution
            proxyClass = Class.forName(className, true, sProxyClassLoader);
        } catch (ClassNotFoundException e) {
            sLoadClassException = e;
            throw new UnsupportedOperationException("Failed to load proxy class", e);
        }
        return proxyClass;
    }

    public static void setWrapperMethod(Method method) {
        sWrapperMethod = method;
    }

    public static Method getWrapperMethod() {
        return sWrapperMethod;
    }

    public static Lsp100ProxyClassMaker getInstance() throws UnsupportedOperationException {
        if (sInstance == null) {
            sInstance = new Lsp100ProxyClassMaker();
        }
        return sInstance;
    }

    private String mXposedHookerClassName;
    private String mBeforeInvocationClassName;
    private String mAfterInvocationClassName;

    private Lsp100ProxyClassMaker() {
        Class<?> templateClass = Lsp100CallbackProxy.P0000000050.class;
        {
            // get XposedHooker annotation class name
            Annotation[] annotations = templateClass.getAnnotations();
            // pick the first annotation
            if (annotations.length > 0) {
                mXposedHookerClassName = annotations[0].annotationType().getName();
            }
        }
        try {
            // get BeforeInvocation annotation class name
            Method before = templateClass.getMethod("before", XposedInterface.BeforeHookCallback.class);
            Annotation[] annotations = before.getAnnotations();
            // pick the first annotation
            if (annotations.length > 0) {
                mBeforeInvocationClassName = annotations[0].annotationType().getName();
            }
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("Method before not found in template class", e);
        }
        try {
            // get AfterInvocation annotation class name
            Method after = templateClass.getMethod("after",
                    XposedInterface.AfterHookCallback.class, Lsp100HookWrapper.InvocationParamWrapper.class);
            Annotation[] annotations = after.getAnnotations();
            // pick the first annotation
            if (annotations.length > 0) {
                mAfterInvocationClassName = annotations[0].annotationType().getName();
            }
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("Method after not found in template class", e);
        }
    }


    @NonNull
    private byte[] makeClassByteCodeForPriority(int priority) {
        String className = getClassNameForPriority(priority);
        return impl1(
                className,
                priority,
                XposedInterface.Hooker.class.getName(),
                XposedInterface.BeforeHookCallback.class.getName(),
                XposedInterface.AfterHookCallback.class.getName(),
                mXposedHookerClassName,
                mBeforeInvocationClassName,
                mAfterInvocationClassName
        );
    }

    @NonNull
    public static byte[] impl1(
            @NonNull String targetClassName,
            @NonNull Integer tagValue,
            @NonNull String classNameXposedInterfaceHooker,
            @NonNull String classBeforeHookCallback,
            @NonNull String classAfterHookCallback,
            @Nullable String classNameXposedHooker,
            @Nullable String classNameBeforeInvocation,
            @Nullable String classNameAfterInvocation
    ) {
        Method wrapperMethod = sWrapperMethod;
        if (wrapperMethod == null) {
            throw new UnsupportedOperationException("Wrapper method not set");
        }
        Object[] args = new Object[]{
                targetClassName,
                tagValue,
                classNameXposedInterfaceHooker,
                classBeforeHookCallback,
                classAfterHookCallback,
                classNameXposedHooker,
                classNameBeforeInvocation,
                classNameAfterInvocation
        };
        Integer version = 1;
        try {
            return (byte[]) wrapperMethod.invoke(null, version, args);
        } catch (ReflectiveOperationException e) {
            if (e instanceof InvocationTargetException) {
                Throwable targetException = ((InvocationTargetException) e).getTargetException();
                if (targetException instanceof RuntimeException) {
                    throw (RuntimeException) targetException;
                }
            }
            throw new UnsupportedOperationException("Failed to invoke wrapper method", e);
        }
    }

    private static String priorityToShortName(int priority) {
        // positive number and zero
        // 0:         P0000000000
        // 49:        P0000000049
        // 2147483647 P2147483647
        // negative number
        // -1:        N0000000001
        // -2147483648N2147483648
        if (priority >= 0) {
            return "P" + String.format(Locale.ROOT, "%010d", priority);
        } else {
            return "N" + String.format(Locale.ROOT, "%010d", -(long) priority);
        }
    }

    private static String getClassNameForPriority(int priority) {
        return "moe.ono.loader.modern.dyn.Lsp100CallbackProxy$" + priorityToShortName(priority);
    }

}
