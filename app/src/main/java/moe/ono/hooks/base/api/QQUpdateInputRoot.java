package moe.ono.hooks.base.api;

import static moe.ono.constants.Constants.MethodCacheKey_InputRoot;
import static moe.ono.util.SyncUtils.runOnUiThread;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;

import moe.ono.creator.PacketHelperDialog;
import moe.ono.dexkit.TargetManager;
import moe.ono.hooks._base.ApiHookItem;
import moe.ono.hooks._core.annotation.HookItem;
import moe.ono.util.Logger;

@SuppressLint("DiscouragedApi")
@HookItem(path = "API/获取InputRoot")
public class QQUpdateInputRoot extends ApiHookItem {
    public void update(ClassLoader classLoader) {
        hookAfter(TargetManager.requireMethod(MethodCacheKey_InputRoot), param -> {
            Button sendBtn = null;
            EditText editText = null;
            ViewGroup inputRoot = null;
            Field[] fs = param.thisObject.getClass().getDeclaredFields();
            for (Field f : fs) {
                Class<?> type = f.getType();
                if (type.equals(Button.class)) {
                    f.setAccessible(true);
                    sendBtn = (Button) f.get(param.thisObject);
                    Logger.d(String.valueOf(f.get(param.thisObject)));
                } else if (type.equals(EditText.class)) {
                    f.setAccessible(true);
                    editText = (EditText) f.get(param.thisObject);
                } else if (type.equals(ViewGroup.class)) {
                    f.setAccessible(true);
                    inputRoot = (ViewGroup) f.get(param.thisObject);
                    assert inputRoot != null;
                }
            }


            if (sendBtn != null && editText != null && inputRoot != null){
                EditText finalEditText = editText;
                sendBtn.setOnLongClickListener(v -> {
                    runOnUiThread(() -> PacketHelperDialog.createView(null, v.getContext(), finalEditText.getText().toString()));

                    return true;
                });
                EditText finalEditText1 = editText;
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        finalEditText1.setFilters( new InputFilter[]{ new InputFilter.LengthFilter(Integer.MAX_VALUE)});
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

            }
        });
    }

    @Override
    public void entry(@NonNull ClassLoader classLoader) throws Throwable {
        update(classLoader);
    }
}