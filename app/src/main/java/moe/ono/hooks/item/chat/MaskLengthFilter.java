package moe.ono.hooks.item.chat;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import moe.ono.hooks._base.BaseSwitchFunctionHookItem;
import moe.ono.hooks._core.annotation.HookItem;


@SuppressLint("DiscouragedApi")
@HookItem(path = "聊天与消息/屏蔽字数限制", description = "可能已经失效，如有需要移步到“开发者选项/QQPacketHelper”")
public class MaskLengthFilter extends BaseSwitchFunctionHookItem {
    public void doHook(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("com.tencent.mobileqq.aio.input.sendmsg.AIOSendMsgVMDelegate", classLoader, "v", java.util.List.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(false);
                super.beforeHookedMethod(param);
            }
        });

        XposedHelpers.findAndHookMethod("com.tencent.mobileqq.aio.input.sendmsg.AIOSendMsgVMDelegate", classLoader, "w", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(false);
                super.beforeHookedMethod(param);
            }
        });
    }

    @Override
    public void entry(@NonNull ClassLoader classLoader) throws Throwable {
        doHook(classLoader);
    }
}