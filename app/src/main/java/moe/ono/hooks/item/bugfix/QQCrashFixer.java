package moe.ono.hooks.item.bugfix;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static moe.ono.common.CheckUtils.isInteger;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.tencent.qqnt.kernel.nativeinterface.FaceBubbleElement;
import com.tencent.qqnt.kernel.nativeinterface.MarketFaceElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.SmallYellowFaceInfo;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import moe.ono.hooks.XHook;
import moe.ono.hooks._base.BaseSwitchFunctionHookItem;
import moe.ono.hooks._core.annotation.HookItem;
import moe.ono.reflex.XMethod;
import moe.ono.util.Logger;


@HookItem(path = "优化与修复/拦截部分闪退", description = "QQ 版本 > 9.1.15 可不开启\n* 重启生效")
@SuppressLint("DiscouragedApi")
public class QQCrashFixer extends BaseSwitchFunctionHookItem {
    public void fix(ClassLoader classLoader) {

        /*
        * Java层崩溃拦截
        * */
        findAndHookMethod(
                "java.lang.Thread",
                classLoader,
                "setDefaultUncaughtExceptionHandler",
                Thread.UncaughtExceptionHandler.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                        Thread.UncaughtExceptionHandler originalHandler = (Thread.UncaughtExceptionHandler) param.args[0];
                        param.args[0] = (Thread.UncaughtExceptionHandler) (t, e) -> {
                        };
                    }
                }
        );


        /*
         * QQ的原创表情在加载的时候会进行一次复制, 但是此处复制的时候会因为表情内容为空而闪退
         */
        try {
            hookBefore(XMethod.clz("com.tencent.qqnt.emotion.adapter.api.impl.MarketFaceApiImpl").name("queryEmoticonInfoFromDB").ignoreParam().get(), param -> {
                MarketFaceElement element = (MarketFaceElement) param.args[0];
                if (element.getEmojiId() == null){
                    element.emojiId = "";
                }

                if (element.getKey() == null){
                    element.key = "";
                }
                param.args[0] = element;
            });
        } catch (NoSuchMethodException e) {
            Logger.e(e);
        }


        /*
         * mqqapi://ecommerce/open?target= 后面必须接整数
         * 如果是其他数据类型会导致闪退
         */
        XposedHelpers.findAndHookMethod("com.tencent.ecommerce.biz.router.ECScheme", classLoader, "l", android.net.Uri.class,"com.tencent.ecommerce.base.router.api.IECSchemeCallback", boolean.class, java.util.Map.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String uri = String.valueOf(param.args[0]);
                if (!isInteger(uri.replace("mqqapi://ecommerce/open?target=", ""))){
                    param.setResult(0);
                }
                super.beforeHookedMethod(param);
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });


        /*
         * 修复QQ的表情回应的表情id导致的闪退
         * QQ的表情id判断了最大值, 但是没判断最小值, 如果id小于0则会由于数组下标越界导致闪退
         */
        findAndHookMethod(
            "com.tencent.mobileqq.emoticon.QQSysFaceUtil",
            classLoader,
            "getFaceDrawableFromLocal",
            int.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int index = (int) param.args[0];
                    if (index < 0) {
                        param.setResult(null);
                    }
                }
            }
        );

        /*
         * QQ的弹射表情在此处会进行一次取值,
         * 但是取出的值为null, 再引用其中的内容会导致闪退
         */
        findAndHookMethod("com.tencent.mobileqq.aio.msg.FaceBubbleMsgItem$msgElement$2", classLoader, "invoke", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.getResult() != null){
                    super.afterHookedMethod(param);
                    return;
                }
                MsgElement msgElement = new MsgElement();
                FaceBubbleElement faceBubbleElement = new FaceBubbleElement();
                SmallYellowFaceInfo smallYellowFaceInfo = new SmallYellowFaceInfo();
                smallYellowFaceInfo.setIndex(187);
                smallYellowFaceInfo.setText("幽灵");
                smallYellowFaceInfo.setCompatibleText("幽灵");
                faceBubbleElement.setFaceType(13);
                faceBubbleElement.setFaceCount(0);
                faceBubbleElement.setFaceFlag(0);
                faceBubbleElement.setFaceSummary("幽灵");
                faceBubbleElement.setContent("[幽灵]x0");
                faceBubbleElement.setYellowFaceInfo(smallYellowFaceInfo);
                msgElement.setElementType(27);
                msgElement.setFaceBubbleElement(faceBubbleElement);
                param.setResult(msgElement);

            }
        });
    }

    @Override
    public void entry(@NonNull ClassLoader classLoader) throws Throwable {
        fix(classLoader);
    }
}