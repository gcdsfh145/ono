package moe.ono.hooks.item.developer;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import moe.ono.hooks._base.BaseSwitchFunctionHookItem;
import moe.ono.hooks._core.annotation.HookItem;

@SuppressLint("DiscouragedApi")
@HookItem(
        path = "开发者选项/C2C 发包显示修复",
        description = "用于修复私聊发包后聊天界面不立刻显示发送内容的问题\n* 不稳定，可能会渲染多条 seq 一致的消息，如果私聊不经常用 QQPacketHelper 就不用开启。"
)
public class QQPacketHelperC2CDisplayFixer extends BaseSwitchFunctionHookItem {
    @Override
    public void entry(@NonNull ClassLoader classLoader) {}

}