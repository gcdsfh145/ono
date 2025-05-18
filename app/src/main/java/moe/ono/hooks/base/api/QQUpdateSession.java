package moe.ono.hooks.base.api;

import static moe.ono.constants.Constants.MethodCacheKey_AIOParam;
import static moe.ono.hooks._core.factory.HookItemFactory.getItem;
import static moe.ono.hooks.message.SessionUtils.getCurrentChatTypeByAIOContact;
import static moe.ono.hooks.message.SessionUtils.getCurrentPeerIDByAIOContact;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.github.kyuubiran.ezxhelper.utils.Log;

import moe.ono.config.ONOConf;
import moe.ono.dexkit.TargetManager;
import moe.ono.hooks._base.ApiHookItem;
import moe.ono.hooks._core.annotation.HookItem;
import moe.ono.hooks.item.chat.ChatScrollMemory;
import moe.ono.hooks.item.chat.QQBubbleRedirect;
import moe.ono.reflex.XField;
import moe.ono.util.Initiator;
import moe.ono.util.Logger;
import moe.ono.util.Session;

@HookItem(path = "API/获取Session")
public class QQUpdateSession extends ApiHookItem {

    private void update(ClassLoader classLoader) {
        hookBefore(TargetManager.requireMethod(MethodCacheKey_AIOParam), param -> {
            Logger.d("on QQUpdateSession");

            Bundle bundle = (Bundle) param.args[0];
            Object cAIOParam = bundle.getParcelable("aio_param");

            Object AIOSession = XField.obj(cAIOParam).type(Initiator.loadClass("com.tencent.aio.data.AIOSession")).get();
            Object AIOContact = XField.obj(AIOSession).type(Initiator.loadClass("com.tencent.aio.data.AIOContact")).get();

            String cPeerUID = getCurrentPeerIDByAIOContact(AIOContact);
            int cChatType = getCurrentChatTypeByAIOContact(AIOContact);

            new Thread(() -> {
                int seq0 = ONOConf.getInt("ChatScrollMemory", cPeerUID, -1);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                int seq = ONOConf.getInt("ChatScrollMemory", cPeerUID, -1);
                Logger.d("seq: " + seq0 + "::" + seq);

                ChatScrollMemory.createGrayTip(seq0, seq, cPeerUID, cChatType);
            }).start();


            Session.setCurrentPeerID(cPeerUID);
            Session.setCurrentChatType(cChatType);
            // -------------------------------------------

            QQBubbleRedirect.invokeGetAioVasMsgData();

//            ChatScrollMemory.createGrayTip();
        });
    }

    @Override
    public void entry(@NonNull ClassLoader classLoader) throws Throwable {
        update(classLoader);
    }
}