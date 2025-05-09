package moe.ono.hooks.message;

import static moe.ono.util.HostInfo.requireMinQQVersion;
import static moe.ono.util.HostInfo.requireMinTimVersion;

import java.io.Serializable;
import java.util.Objects;

import moe.ono.bridge.kernelcompat.ContactCompat;
import moe.ono.reflex.XField;
import moe.ono.util.Initiator;
import moe.ono.util.Logger;
import moe.ono.util.QQVersion;
import moe.ono.util.TIMVersion;

public class SessionUtils {

    public static ContactCompat AIOParam2Contact(Object AIOParam) {
        try {
            Object AIOSession = XField.obj(AIOParam).type(Initiator.loadClass("com.tencent.aio.data.AIOSession")).get();
            Object AIOContact = XField.obj(AIOSession).type(Initiator.loadClass("com.tencent.aio.data.AIOContact")).get();
            ContactCompat contact = new ContactCompat();
            contact.setPeerUid(getCurrentPeerIDByAIOContact(AIOContact));

            int chatType = getCurrentChatTypeByAIOContact(AIOContact);
            contact.setChatType(chatType);

            if (chatType == 4) {
                throw new RuntimeException("chatType == 4 is not supported");
            }
            return contact;
        } catch (Exception e) {
            Logger.e("SessionUtils.AIOParam2Contact", e);
            return null;
        }
    }

    public static Serializable AIOParam2ContactRaw(Object AIOParam) {
        return Objects.requireNonNull(AIOParam2Contact(AIOParam)).toKernelObject();
    }

    public static String getCurrentPeerIDByAIOContact(Object AIOContact) throws Exception {
        return XField.obj(AIOContact).name(
                (requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA) || requireMinQQVersion(QQVersion.QQ_9_1_70))
                        ? "e" : "f").type(String.class).get();
    }

    public static int getCurrentChatTypeByAIOContact(Object AIOContact) throws Exception {
        return XField.obj(AIOContact).name(
                (requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA) || requireMinQQVersion(QQVersion.QQ_9_1_70))
                        ? "d" : "e").type(int.class).get();
    }
}
