package moe.ono.creator.stickerPanel;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.core.BottomPopupView;
import com.lxj.xpopup.util.XPopupUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import eightbitlab.com.blurview.BlurAlgorithm;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;
import eightbitlab.com.blurview.RenderScriptBlur;
import moe.ono.R;
import moe.ono.config.ONOConf;
import moe.ono.creator.stickerPanel.MainItemImpl.LocalStickerImpl;
import moe.ono.creator.stickerPanel.MainItemImpl.PanelSetImpl;
import moe.ono.creator.stickerPanel.MainItemImpl.RecentStickerImpl;
import moe.ono.ui.CommonContextWrapper;
import moe.ono.util.HostInfo;
import moe.ono.util.LayoutHelper;

@SuppressLint("ResourceType")
public class ICreator extends BottomPopupView {

    private static BasePopupView popupView;

    private LinearLayout topSelectBar;
    private ScrollView   itemContainer;
    private IMainPanelItem currentTab;

    private final ArrayList<ViewGroup> mItems = new ArrayList<>();

    /* ---------- 常量 ---------- */
    private static final long   ANIM_DURATION        = 150L;
    private static final float  SELECTED_SCALE       = 1.15f;
    private static final float  DEFAULT_SCALE        = 1.0f;
    private static final int    INDICATOR_COLOR      = Color.parseColor("#FFFFFF");
    private static final float  CORNER_RADIUS_DP     = 6f;
    private static final int    ICON_RESOLUTION_DP   = 48;
    private static final int    ICON_TOP_PADDING_DP  = 10;
    private static final float  POPUP_HEIGHT_RATIO   = 0.7f;

    /* ---------- 状态 ---------- */
    private boolean open_last_select;
    private static long savedSelectID;
    private static int  savedScrollTo;
    private ViewGroup recentUse;

    /* ---------- BlurView ---------- */
    private BlurView blurBg;

    public ICreator(@NonNull Context context) { super(context); }

    /* ========= 外部 API ========= */
    public static void createPanel(Context context) {
        Context ctx = CommonContextWrapper.createAppCompatContext(context);
        popupView = new XPopup.Builder(ctx)
                .moveUpToKeyboard(false)
                .isDestroyOnDismiss(true)
                .asCustom(new ICreator(ctx));
        popupView.show();
    }
    public static void dismissAll() { if (popupView != null) popupView.dismiss(); }

    /* ========= 控件初始化 ========= */
    private void initTopSelectBar() {
        topSelectBar = findViewById(R.id.Sticker_Pack_Select_Bar);
        topSelectBar.setClipChildren(false);
        topSelectBar.setClipToPadding(false);
    }

    private long scrollTime = 0;
    private void initListView() {
        itemContainer = findViewById(R.id.sticker_panel_pack_container);
        itemContainer.setOnScrollChangeListener((v, sx, sy, osx, osy) -> {
            if (System.currentTimeMillis() - scrollTime > 20) {
                if (currentTab != null) currentTab.notifyViewUpdate0();
                scrollTime = System.currentTimeMillis();
            }
        });
    }

    private void initStickerPacks() {
        List<LocalDataHelper.LocalPath> paths = LocalDataHelper.readPaths();
        for (LocalDataHelper.LocalPath p : paths) {
            IMainPanelItem item = new LocalStickerImpl(p, getContext());
            AtomicReference<ViewGroup> ref = new AtomicReference<>();
            ViewGroup tab = (ViewGroup) createPicImage(p.coverName, p.Name, v -> {
                itemContainer.scrollTo(0, 0);
                switchToItem(ref.get());
            }, p);
            tab.setTag(item);
            topSelectBar.addView(tab);
            ref.set(tab);
        }
    }

    private void initDefItemsBefore() {
        IMainPanelItem recent = new RecentStickerImpl(getContext());
        AtomicReference<ViewGroup> ref = new AtomicReference<>();
        ViewGroup tab = (ViewGroup) createPicImage(
                R.drawable.sticker_panel_recent_icon, "最近使用",
                v -> switchToItem(ref.get()));
        tab.setTag(recent);
        ref.set(tab);
        topSelectBar.addView(tab);
        recentUse = tab;
    }

    private void initDefItemsLast() {
        IMainPanelItem setting = new PanelSetImpl(getContext());
        AtomicReference<ViewGroup> ref = new AtomicReference<>();
        ViewGroup tab = (ViewGroup) createPicImage(
                R.drawable.sticker_panen_set_button_icon, "设置",
                v -> switchToItem(ref.get()));
        tab.setTag(setting);
        ref.set(tab);
        topSelectBar.addView(tab);
    }

    /* ========= 切换逻辑 ========= */
    private void switchToItem(ViewGroup item) {
        for (ViewGroup tab : mItems) {
            boolean selected = (tab == item);
            applySelectionStyle(tab, selected);
            if (!selected) ((IMainPanelItem) tab.getTag()).onViewDestroy();
        }
        currentTab = (IMainPanelItem) item.getTag();
        itemContainer.removeAllViews();
        itemContainer.addView(currentTab.getView());
        Async.runOnUi(currentTab::notifyViewUpdate0);
    }

    private void applySelectionStyle(ViewGroup panel, boolean selected) {
        ImageView img = (ImageView) panel.getChildAt(0);
        View indicator = panel.findViewById(887533);
        float scale = selected ? SELECTED_SCALE : DEFAULT_SCALE;
        float transY = selected ? img.getHeight() * (scale - 1f) / 2f : 0f;
        img.animate().scaleX(scale).scaleY(scale).translationY(transY)
                .setDuration(ANIM_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        if (indicator != null) {
            if (selected) {
                indicator.setVisibility(VISIBLE);
                indicator.animate().alpha(1f).setDuration(ANIM_DURATION).start();
            } else {
                indicator.animate().alpha(0f).setDuration(ANIM_DURATION)
                        .withEndAction(() -> indicator.setVisibility(GONE)).start();
            }
        }
    }

    /* ========= 顶部按钮工具 ========= */
    private View createPicImage(String imgPath, String title, OnClickListener l,
                                LocalDataHelper.LocalPath path) {
        ImageView img = new ImageView(getContext());
        img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout panel = initBasePanel(title, l, img);
        Glide.with(HostInfo.getApplication())
                .load(Env.app_save_path + "本地表情包/" + path.storePath + "/" + imgPath)
                .apply(RequestOptions.overrideOf(LayoutHelper.dip2px(getContext(), ICON_RESOLUTION_DP)).dontTransform())
                .into(img);
        addIndicator(panel);
        mItems.add(panel);
        return panel;
    }

    @SuppressLint("ResourceType")
    private View createPicImage(int resId, String title, OnClickListener l) {
        ImageView img = new ImageView(getContext());
        img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout panel = initBasePanel(title, l, img);
        Glide.with(HostInfo.getApplication())
                .load(resId)
                .apply(RequestOptions.overrideOf(LayoutHelper.dip2px(getContext(), ICON_RESOLUTION_DP)).dontTransform())
                .into(img);
        addIndicator(panel);
        mItems.add(panel);
        return panel;
    }

    private LinearLayout initBasePanel(String title, OnClickListener l, ImageView img) {
        LinearLayout panel = new LinearLayout(getContext());
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setOnClickListener(l);
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        panel.setPadding(0, LayoutHelper.dip2px(getContext(), ICON_TOP_PADDING_DP), 0, 0);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(
                LayoutHelper.dip2px(getContext(), 30), LayoutHelper.dip2px(getContext(), 30));
        imgLp.setMargins(LayoutHelper.dip2px(getContext(), 6), 0,
                LayoutHelper.dip2px(getContext(), 6), LayoutHelper.dip2px(getContext(), 4));
        img.setLayoutParams(imgLp);
        img.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        panel.addView(img);
        TextView tv = new TextView(getContext());
        tv.setText(title);
        tv.setTextColor(getContext().getColor(R.color.global_font_color));
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setTextSize(10);
        tv.setSingleLine();
        panel.addView(tv);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutHelper.dip2px(getContext(), 50), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(LayoutHelper.dip2px(getContext(), 5), 0,
                LayoutHelper.dip2px(getContext(), 5), 0);
        panel.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(LayoutHelper.dip2px(getContext(), CORNER_RADIUS_DP));
        bg.setColor(Color.TRANSPARENT);
        panel.setBackground(bg);
        return panel;
    }

    private void addIndicator(LinearLayout panel) {
        View bar = new View(getContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(LayoutHelper.dip2px(getContext(), CORNER_RADIUS_DP));
        bg.setColor(INDICATOR_COLOR);
        bar.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutHelper.dip2px(getContext(), 30), LayoutHelper.dip2px(getContext(), 3));
        lp.setMargins(LayoutHelper.dip2px(getContext(), 5),
                LayoutHelper.dip2px(getContext(), 4),
                LayoutHelper.dip2px(getContext(), 5), 0);
        bar.setLayoutParams(lp);
        bar.setId(887533);
        bar.setVisibility(GONE);
        bar.setAlpha(0f);
        panel.addView(bar);
    }

    /* ========= 生命周期 ========= */
    @Override
    protected void onCreate() {
        super.onCreate();

        blurBg = findViewById(R.id.blur_bg);
        if (blurBg != null) {
            float radiusPx = LayoutHelper.dip2px(getContext(), 10f);

            Activity act   = findActivity(getContext());
            View decor     = act.getWindow().getDecorView();
            ViewGroup root = decor.findViewById(android.R.id.content);
            Drawable winBg = decor.getBackground();

            BlurAlgorithm algo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? new RenderEffectBlur()
                    : new RenderScriptBlur(getContext());

            blurBg.setupWith(root, algo)
                    .setFrameClearDrawable(winBg)
                    .setBlurRadius(radiusPx)
                    .setOverlayColor(0x303C4043);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            initTopSelectBar();
            initListView();
            initDefItemsBefore();
            initStickerPacks();
            initDefItemsLast();

            open_last_select = ONOConf.getBoolean("global",
                    "sticker_panel_set_open_last_select", false);
            if (open_last_select) {
                savedSelectID = Long.parseLong(
                        ONOConf.getString("global",
                                "sticker_panel_set_last_select",
                                String.valueOf(savedSelectID)));
            }
            if (savedSelectID != 0) {
                for (ViewGroup tab : mItems) {
                    IMainPanelItem m = (IMainPanelItem) tab.getTag();
                    if (m.getID() == savedSelectID) {
                        switchToItem(tab);
                        Async.runOnUi(m::notifyViewUpdate0, 100);
                        Async.runOnUi(() -> itemContainer.scrollTo(0, savedScrollTo));
                        break;
                    }
                }
            } else {
                switchToItem(recentUse);
            }
        }, 50);
    }

    @Override
    protected void beforeDismiss() {
        if (currentTab != null) {
            savedScrollTo = itemContainer.getScrollY();
            savedSelectID = currentTab.getID();
            if (open_last_select) {
                ONOConf.setString("global",
                        "sticker_panel_set_last_select",
                        String.valueOf(savedSelectID));
            }
        }
        super.beforeDismiss();
    }

    @Override
    protected void onDismiss() {
        super.onDismiss();
        if (blurBg != null) blurBg.setBlurEnabled(false);  // 关闭模糊

        for (ViewGroup item : mItems) {
            ((IMainPanelItem) item.getTag()).onViewDestroy();
        }
        Glide.get(HostInfo.getApplication()).clearMemory();
    }

    @Override protected int getImplLayoutId() { return R.layout.sticker_panel_plus_main; }
    @Override protected int getMaxHeight()   { return (int) (XPopupUtils.getScreenHeight(getContext()) * POPUP_HEIGHT_RATIO); }
    @Override protected int getPopupHeight() { return (int) (XPopupUtils.getScreenHeight(getContext()) * POPUP_HEIGHT_RATIO); }

    /* ========= 子接口 ========= */
    public interface IMainPanelItem {
        View getView();
        void onViewDestroy();
        long getID();
        void notifyViewUpdate0();
    }

    /* ========= 工具 ========= */
    private static Activity findActivity(Context ctx) {
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) return (Activity) ctx;
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }
}
