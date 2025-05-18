package moe.ono.hooks.item.bugfix;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;

import android.widget.TextView;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import moe.ono.hooks._base.BaseSwitchFunctionHookItem;
import moe.ono.hooks._core.annotation.HookItem;

@HookItem(path = "优化与修复/拦截卡屏文字", description = "非必要不建议开启")
public class TextViewLongTextFixHook extends BaseSwitchFunctionHookItem {
    @Override
    public void entry(@NonNull ClassLoader classLoader) throws Throwable {
        XC_MethodHook hodorHodor = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                CharSequence text = (CharSequence) param.args[0];
                if (text != null) {
                    if (text.length() > 8000) {
                        String truncatedText = "ovo: *疑似卡屏，已阻止此文字消息的加载";
                        param.args[0] = truncatedText;
                    }
                }
            }
        };


        Method m = findMethodExact(TextView.class, "setText", CharSequence.class, TextView.BufferType.class,
                boolean.class, int.class);
        Method m2 = findMethodExact(TextView.class, "setText", CharSequence.class, hodorHodor);

        hookBefore(m, this::fix);
        hookBefore(m2, this::fix);
    }

    private void fix(XC_MethodHook.MethodHookParam param) {
        CharSequence text = (CharSequence) param.args[0];
        if (text != null) {
            if (text.length() > 8000) {
                String truncatedText = "ovo: *疑似卡屏，已阻止此文字消息的加载";
                param.args[0] = truncatedText;
            }
        }
    }
}
