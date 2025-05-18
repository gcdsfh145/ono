package moe.ono.hooks.item.chat;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;
import static moe.ono.util.Initiator.loadClass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import moe.ono.hooks._base.BaseSwitchFunctionHookItem;
import moe.ono.hooks._core.annotation.HookItem;
import moe.ono.util.Logger;

@SuppressLint("DiscouragedApi")
@HookItem(path = "聊天与消息/Telegram 模式", description = "目前只能做到隐藏自己的头像")
public class TelegramAvatarMode extends BaseSwitchFunctionHookItem {
    public void hideSelfAvatar(ClassLoader classLoader) throws ClassNotFoundException {
        Method m = findMethodExact(loadClass("com.tencent.mobileqq.aio.msglist.holder.component.avatar.AIOAvatarContentComponent$avatarContainer$2"), "invoke");
        hookAfter(m, new HookAction() {
            @Override
            public void call(XC_MethodHook.MethodHookParam param) {
                RelativeLayout relativeLayout = (RelativeLayout) param.getResult();
                ImageView view = (ImageView) relativeLayout.getChildAt(0);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager windowManager = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);

                view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int screenWidth = displayMetrics.widthPixels;
                        int[] location = new int[2];
                        view.getLocationOnScreen(location);
                        int absoluteX = location[0];

                        if (absoluteX < screenWidth / 2) {/* nothing to do */} else {
                            relativeLayout.setVisibility(View.GONE);
                        }

                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

                relativeLayout.post(() -> {
                    int screenWidth = displayMetrics.widthPixels;
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);
                    int absoluteX = location[0];

                    if (absoluteX < screenWidth / 2) {
                        Logger.d("ImageView is on the left side");
                    } else {
                        Logger.d("ImageView is on the right side");
                        relativeLayout.setVisibility(View.GONE);
                    }
                });
            }
        });

    }

    @Override
    public void entry(@NonNull ClassLoader classLoader) throws Throwable {
        hideSelfAvatar(classLoader);
    }
}