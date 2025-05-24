package moe.ono.config;

import android.annotation.SuppressLint;
import android.app.Activity;

import com.tencent.qqnt.kernel.nativeinterface.MsgRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import moe.ono.bridge.kernelcompat.ContactCompat;
import moe.ono.util.Logger;

public class CacheConfig {

    private CacheConfig() {
        throw new AssertionError("No instance for you!");
    }

    private static Boolean isAutoMosaicNameNT;
    private static Boolean isSetEntry;
    private static final ArrayList<String> registerReceiverList = new ArrayList<>();
    private static int recreateCount;
    private static String rkeyGroup;
    private static String rkeyPrivate;
    @SuppressLint("StaticFieldLeak")
    private static Activity splashActivity;
    private static String itemid;
    private static Class x5WebViewClass;
    private static Class x5ValueCallbackClass;
    private static MsgRecord msgRecord;
    private static ContactCompat contactCompat;
    private static Object iqqntWrapperSessionInstance;

    public static final Map<Integer, PbSendMsgPacket> pbSendMsgPacketMap = new HashMap<>();
    private static int nextPbSendMsgPacketIndex = 0;

    public static String getRKeyGroup() {
        if (rkeyGroup == null){
            Logger.e("rkeyGroup is null!");
            throw new NullPointerException("rkeyGroup is null!");
        }
        return rkeyGroup;
    }

    public static String getRKeyPrivate() {
        if (rkeyGroup == null){
            Logger.e("rkeyPrivate is null!");
            throw new NullPointerException("rkeyPrivate is null!");
        }
        return rkeyPrivate;
    }

    public static void setRKeyGroup(String rk) {
        Objects.requireNonNull(rk);
        CacheConfig.rkeyGroup = rk;
    }

    public static void setRKeyPrivate(String rk) {
        Objects.requireNonNull(rk);
        CacheConfig.rkeyPrivate = rk;
    }

    public static void setItemID(String itemID) {
        Objects.requireNonNull(itemID);
        CacheConfig.itemid = itemID;
    }


    public static String getItemID() {
        return CacheConfig.itemid;
    }

    public static void setX5WebViewClass(Class clazz) {
        Objects.requireNonNull(clazz);
        CacheConfig.x5WebViewClass = clazz;
    }

    public static Class getX5WebViewClass() {
        return CacheConfig.x5WebViewClass;
    }

    public static void setX5ValueCallbackClass(Class clazz) {
        Objects.requireNonNull(clazz);
        CacheConfig.x5ValueCallbackClass = clazz;
    }

    public static MsgRecord getMsgRecord() {
        return CacheConfig.msgRecord;
    }

    public static void setMsgRecord(MsgRecord msgRecord) {
        Objects.requireNonNull(msgRecord);
        CacheConfig.msgRecord = msgRecord;
    }


    public static Class getX5ValueCallbackClass() {
        return CacheConfig.x5ValueCallbackClass;
    }

    public static void setSplashActivity(Activity activity) {
        Objects.requireNonNull(activity);
        CacheConfig.splashActivity = activity;
    }

    public static Activity getSplashActivity() {
        if (splashActivity == null){
            Logger.e("splashActivity is null!");
            throw new NullPointerException("splashActivity is null!");
        }
        return splashActivity;
    }

    public static int getRecreateCount() {
        return Objects.requireNonNullElse(recreateCount, 0);
    }

    public static int getPbSendMsgPacketIndex() {
        if (pbSendMsgPacketMap.isEmpty()) {
            return -1;
        }

        Integer lastKey = null;
        for (Integer key : pbSendMsgPacketMap.keySet()) {
            lastKey = key;
        }
        return lastKey;
    }


    public static PbSendMsgPacket getPbSendMsgPacket(int index) {
        return pbSendMsgPacketMap.get(index);
    }

    public static boolean containsPbSendMsgPacket(int index) {
        return pbSendMsgPacketMap.containsKey(index);
    }


    public static void clearPbSendMsgPackets() {
        pbSendMsgPacketMap.clear();
        nextPbSendMsgPacketIndex = 0;
    }

    public static void removeLastPbSendMsgPacket() {
        if (!pbSendMsgPacketMap.isEmpty()) {
            Integer lastKey = null;
            for (Integer key : pbSendMsgPacketMap.keySet()) {
                lastKey = key;
            }
            if (lastKey != null) {
                pbSendMsgPacketMap.remove(lastKey);
                nextPbSendMsgPacketIndex = Math.min(nextPbSendMsgPacketIndex, lastKey);
            }
        }
    }


    public static Boolean isAutoMosaicNameNT() {
        return Objects.requireNonNullElse(isAutoMosaicNameNT, false);
    }

    public static Boolean isSetEntry() {
        return Objects.requireNonNullElse(isSetEntry, false);
    }

    public static void setAutoMosaicNameNT(Boolean isAutoMosaicNameNT) {
        Objects.requireNonNull(isAutoMosaicNameNT);
        CacheConfig.isAutoMosaicNameNT = isAutoMosaicNameNT;
    }

    public static ContactCompat getContactCompat() {
        return Objects.requireNonNullElse(contactCompat, null);
    }

    public static void setIQQntWrapperSessionInstance(Object object) {
        Objects.requireNonNull(object);
        CacheConfig.iqqntWrapperSessionInstance = object;
    }

    public static Object getIQQntWrapperSessionInstance() {
        return Objects.requireNonNullElse(iqqntWrapperSessionInstance, null);
    }

    public static void setContactCompat(ContactCompat contactCompat) {
        CacheConfig.contactCompat = contactCompat;
    }


    public static void setIsSetEntry(Boolean isSetEntry) {
        Objects.requireNonNull(isSetEntry);
        CacheConfig.isSetEntry = isSetEntry;
    }

    public static void addRecreateCount() {
        CacheConfig.recreateCount = getRecreateCount() + 1;
    }

    public static int addPbSendMsgPacket(PbSendMsgPacket value) {
        Objects.requireNonNull(value, "Packet value cannot be null");
        int index = nextPbSendMsgPacketIndex++;
        pbSendMsgPacketMap.put(index, value);
        return index;
    }


    public static void addReceiver(String name) {
        Objects.requireNonNull(name);

        if (!isReceiverRegistered(name)) {
            CacheConfig.registerReceiverList.add(name);
        } else {
            Logger.i("Receiver " + name + " is already registered.");
        }
    }

    public static boolean isReceiverRegistered(String name) {
        Objects.requireNonNull(name, "Receiver name cannot be null");
        try {
            return CacheConfig.registerReceiverList.contains(name);
        } catch (Exception e) {
            return false;
        }

    }

    public static class PbSendMsgPacket {
        private final String content;
        private final String peerid;

        public PbSendMsgPacket(String content, String peerid) {
            this.content = content;
            this.peerid =  peerid;
        }

        public String getContent() {
            return content;
        }

        public String getPeerid() {
            return peerid;
        }
    }

}

