package moe.ono.hooks.item.bugfix;

import static moe.ono.constants.Constants.MethodCacheKey_MarkdownAIO;
import static moe.ono.util.Utils.findMethodByName;
import static moe.ono.util.Utils.replaceInvalidLinks;
import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tencent.qqnt.kernel.nativeinterface.MarkdownElement;

import java.lang.reflect.Method;

import moe.ono.BuildConfig;
import moe.ono.config.ConfigManager;
import moe.ono.hooks._base.BaseSwitchFunctionHookItem;
import moe.ono.hooks._core.annotation.HookItem;
import moe.ono.util.Logger;


@SuppressLint("DiscouragedApi")
@HookItem(path = "优化与修复/拦截异常 Markdown 消息", description = "劫持非官方机器人发送的 Markdown 消息，并阻止其中的图片资源加载")
public class MarkdownGuard extends BaseSwitchFunctionHookItem {
    public void fix(@NonNull ClassLoader cl) {
        String cachedMethodSignature = ConfigManager.getCache().getString(MethodCacheKey_MarkdownAIO, null);
        Method targetMethod = null;

        if (cachedMethodSignature != null) {
            try {
                String[] parts = cachedMethodSignature.split("#");
                String className = parts[0];
                String methodName = parts[1];
                Class<?> clazz = cl.loadClass(className);
                targetMethod = findMethodByName(clazz, methodName);
            } catch (Exception e) {
                Logger.e("拦截异常 Markdown 消息", e);
            }
        }



        try {
            hookBefore(targetMethod, param -> {
                String content = ((MarkdownElement)param.args[0]).getContent();
                String[] replacedMessage = replaceInvalidLinks(content);
                Logger.d("replacedMessage\n" + replacedMessage[0]);
                String content_replaced = replacedMessage[0];
                if (Boolean.parseBoolean(replacedMessage[1])){
                    content_replaced = content_replaced.replace("`", "^");
                    content_replaced += "\n***\n# 以下消息来自ovo!\n\n- 提示: 此markdown不合法！已阻止资源加载\n- 原因：此消息内包含了一个或多个非官方的图片资源链接。";
                }
                MarkdownElement markdownElement = new MarkdownElement(content_replaced);
                param.args[0] = markdownElement;
            });
        } catch (Exception e) {
            Logger.e("拦截异常 Markdown 消息","err:"+e);
        }
    }

    @Override
    public void entry(@NonNull ClassLoader classLoader) throws Throwable {
        fix(classLoader);
    }
}