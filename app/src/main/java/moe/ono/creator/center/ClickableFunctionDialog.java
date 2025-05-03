package moe.ono.creator.center;

import static moe.ono.constants.Constants.PrekCfgXXX;
import static moe.ono.constants.Constants.PrekClickableXXX;
import static moe.ono.hooks._core.factory.HookItemFactory.getItem;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lxj.xpopup.core.PositionPopupView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

import moe.ono.R;
import moe.ono.activity.BiliLoginActivity;
import moe.ono.config.ConfigManager;
import moe.ono.config.ONOConf;
import moe.ono.hooks._base.BaseClickableFunctionHookItem;
import moe.ono.hooks.item.chat.StickerPanelEntry;
import moe.ono.hooks.item.sigma.QQMessageTracker;
import moe.ono.hooks.item.sigma.QQSurnamePredictor;
import moe.ono.util.Logger;
import moe.ono.util.SyncUtils;
import moe.ono.util.api.ark.ArkRequest;

public class ClickableFunctionDialog {
    public static void showCFGDialogSurnamePredictor(BaseClickableFunctionHookItem item, Context context){
        if (context == null) return;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("猜姓氏");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        final MaterialCheckBox checkBox = new MaterialCheckBox(context);
        checkBox.setText("启用");

        final TextView textView = new TextView(context);
        final EditText input = new EditText(context);
        input.setHint("300");
        input.setText(String.valueOf(ConfigManager.dGetInt(PrekCfgXXX + item.getPath(), 300)));
        textView.setText("操作间隔（毫秒）");
        layout.addView(checkBox);
        layout.addView(textView);
        layout.addView(input);

        builder.setView(layout);

        final TextView warningText = new TextView(context);
        layout.addView(warningText);

        builder.setNegativeButton("关闭", (dialog, which) -> dialog.cancel());

        checkBox.setChecked(item.isEnabled());

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ConfigManager.dPutBoolean(PrekClickableXXX + getItem(QQSurnamePredictor.class).getPath(), isChecked);
            item.setEnabled(isChecked);
            if (isChecked) {
                item.startLoad();
            }
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    ConfigManager.dPutInt(PrekCfgXXX + getItem(QQSurnamePredictor.class).getPath(), Integer.parseInt(s.toString()));
                    warningText.setText("");
                } catch (NumberFormatException e) {
                    warningText.setText("输入错误");
                }

            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        builder.show();
    }

    @SuppressLint("SetTextI18n")
    public static void showCFGDialogQQMessageTracker(BaseClickableFunctionHookItem item, Context context){
        if (context == null) return;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("已读追踪");

        LinearLayout layout = new LinearLayout(context);
        ScrollView scrollView = new ScrollView(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        ScrollView.LayoutParams sParams = new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        );

        scrollView.setLayoutParams(sParams);

        layout.setPadding(50, 10, 50, 50);

        final MaterialCheckBox checkBox = new MaterialCheckBox(context);
        checkBox.setText("启用");

        final MaterialCheckBox checkBoxDef = new MaterialCheckBox(context);
        checkBoxDef.setText("使用默认服务器设置");

        final String i = "\n登陆信息: \n昵称: %s\nUID: %s\nArk-Coins: %s\n---------\n* 重新打开此窗口来刷新登陆信息";

        final TextView textView = new TextView(context);
        final TextView tvInfo = new TextView(context);
        final TextView subtitle = new TextView(context);

        textView.setText("\n提示：为了确保此功能不被滥用，我们需要您绑定第三方账号以便进行管理和验证。\n在未登录的情况下，此功能将无法使用。\n\n");
        tvInfo.setText(String.format(i, "未知", "未知", "未知"));

        subtitle.setText("服务器绑定");

        final MaterialButton materialButton = new MaterialButton(context);
        materialButton.setText("登录您的 哔哩哔哩 账号");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        materialButton.setLayoutParams(params);

        new Thread(new Runnable(){
            @Override
            public void run() {
                tvInfo.setText(String.format(i, "获取中...", "获取中...", "获取中..."));
                try {
                    StringBuilder sb = getStringBuilder();

                    String userinfo = String.valueOf(sb);
                    JSONObject jsonObject_userinfo = new JSONObject(userinfo).optJSONObject("card");
                    String userName = Objects.requireNonNull(jsonObject_userinfo).optString("name");
                    String userUID = Objects.requireNonNull(jsonObject_userinfo).optString("mid");
                    String arkCoins = ArkRequest.getArkCoinsByMid(userUID, context);
                    SyncUtils.post(() -> tvInfo.setText(String.format(i, userName, userUID, arkCoins)));
                } catch (Exception e){
                    Logger.e("showCFGDialogQQMessageTracker", e);
                    SyncUtils.post(() -> tvInfo.setText(String.format(i, "出错了！", "出错了！", "出错了！")));
                }
            }

            @NonNull
            private StringBuilder getStringBuilder() throws IOException {
                String urlPath = "https://account.bilibili.com/api/member/getCardByMid";
                URL url = new URL(urlPath);
                URLConnection conn = url.openConnection();

                String cookies = ONOConf.getString("global", "cookies", "");

                conn.setRequestProperty("Cookie", cookies);
                conn.setDoInput(true);
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                return sb;
            }
        }).start();

        final EditText signAddress = new EditText(context);
        signAddress.setHint("签名服务器地址");
        signAddress.setText(ConfigManager.dGetString(PrekCfgXXX + "signAddress", "https://ark.ouom.fun/"));

        final EditText authenticationAddress = new EditText(context);
        authenticationAddress.setHint("鉴权服务器地址");
        authenticationAddress.setText(ConfigManager.dGetString(PrekCfgXXX + "authenticationAddress", "https://q.lyhc.top/"));

        signAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ConfigManager.dPutString(PrekCfgXXX + "signAddress", String.valueOf(s));
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        authenticationAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ConfigManager.dPutString(PrekCfgXXX + "authenticationAddress", String.valueOf(s));
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        checkBox.setChecked(ConfigManager.dGetBoolean(PrekClickableXXX + getItem(QQMessageTracker.class).getPath()));
        checkBoxDef.setChecked(ConfigManager.dGetBooleanDefTrue(PrekCfgXXX + "usingDefSetting"));

        layout.addView(checkBox);
        layout.addView(materialButton);
        layout.addView(textView);

        layout.addView(subtitle);
        layout.addView(signAddress);
        layout.addView(authenticationAddress);
        layout.addView(checkBoxDef);
        layout.addView(tvInfo);

        if (checkBoxDef.isChecked()) {
            signAddress.setEnabled(false);
            authenticationAddress.setEnabled(false);
        }

        checkBoxDef.setOnCheckedChangeListener((buttonView, isChecked) -> {
            signAddress.setEnabled(!isChecked);
            authenticationAddress.setEnabled(!isChecked);

            ConfigManager.dPutBoolean(PrekCfgXXX + "usingDefSetting", isChecked);
            if (isChecked) {
                signAddress.setText("https://ark.ouom.fun/");
                authenticationAddress.setText("https://q.lyhc.top/");
            }
        });

        scrollView.addView(layout);

        builder.setView(scrollView);


        builder.setNegativeButton("关闭", (dialog, which) -> dialog.cancel());

        checkBox.setChecked(item.isEnabled());

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ConfigManager.dPutBoolean(PrekClickableXXX + item.getPath(), isChecked);
            item.setEnabled(isChecked);
            if (isChecked) {
                item.startLoad();
            }
        });

        materialButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), BiliLoginActivity.class);
            v.getContext().startActivity(intent);
        });



        builder.show();
    }

    public static void showCFGDialogStickerPanelEntry(BaseClickableFunctionHookItem item, Context context){
        if (context == null) return;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("表情面板");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        final MaterialCheckBox checkBox = new MaterialCheckBox(context);
        checkBox.setText("启用");

        final MaterialCheckBox checkBox2 = new MaterialCheckBox(context);
        checkBox2.setText("替换长按事件优先级（轻触即唤起）");

        checkBox.setChecked(ConfigManager.dGetBoolean(PrekClickableXXX + getItem(StickerPanelEntry.class).getPath()));
        checkBox2.setChecked(ConfigManager.dGetBoolean(PrekCfgXXX + "replaceStickerPanelClickEvent"));
        final TextView textView = new TextView(context);
        textView.setText("更多设置");
        layout.addView(checkBox);
        layout.addView(textView);
        layout.addView(checkBox2);

        builder.setView(layout);

        final TextView warningText = new TextView(context);
        layout.addView(warningText);

        builder.setNegativeButton("关闭", (dialog, which) -> dialog.cancel());

        checkBox.setChecked(item.isEnabled());

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ConfigManager.dPutBoolean(PrekClickableXXX + item.getPath(), isChecked);
            item.setEnabled(isChecked);
            if (isChecked) {
                item.startLoad();
            }
        });

        checkBox2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ConfigManager.dPutBoolean(PrekCfgXXX + "replaceStickerPanelClickEvent", isChecked);
            if (isChecked) {
                item.startLoad();
            }
        });



        builder.show();
    }
}
