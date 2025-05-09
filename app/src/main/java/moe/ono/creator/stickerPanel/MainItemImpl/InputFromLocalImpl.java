package moe.ono.creator.stickerPanel.MainItemImpl;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import moe.ono.R;
import moe.ono.creator.stickerPanel.ICreator;
import moe.ono.creator.stickerPanel.LocalDataHelper;
import moe.ono.creator.stickerPanel.RandomUtils;
import moe.ono.util.DataUtils;
import moe.ono.util.FileUtils;
import moe.ono.util.Logger;
import moe.ono.util.SyncUtils;

public class InputFromLocalImpl implements ICreator.IMainPanelItem {

    private final Context mContext;

    public InputFromLocalImpl(Context mContext) {
        this.mContext = mContext;
    }

    /* -------------------------------------- UI -------------------------------------- */

    @Override
    public View getView() {
        ViewGroup root = (ViewGroup) View.inflate(mContext, R.layout.sticker_panel_impl_input_from_local, null);
        EditText edPath = root.findViewById(R.id.input_path);

        // 确认按钮
        Button btnConfirm = root.findViewById(R.id.btn_confirm_input);
        btnConfirm.setOnClickListener(v -> {
            String txt = edPath.getText().toString().trim();
            File dir = new File(txt);
            if (!dir.isDirectory()) {
                showInvalidPathDialog();
            } else {
                inputWorker(mContext, txt);
            }
        });


        return root;
    }

    /* -------------------------------- 入口（路径） ------------------------------- */

    private static void inputWorker(Context ctx, String path) {
        internalImport(ctx, new File(path));
    }


    /** 向用户询问导入目标分组 */
    private static void internalImport(Context ctx, Object directory) {
        List<LocalDataHelper.LocalPath> paths = LocalDataHelper.readPaths();
        List<String> names = new ArrayList<>();
        for (LocalDataHelper.LocalPath p : paths) names.add(p.Name);

        new MaterialAlertDialogBuilder(ctx)
                .setTitle("选择导入分组")
                .setItems(names.toArray(new String[0]),
                        (d, which) -> copyIntoExisting(ctx, directory, paths.get(which)))
                .setNegativeButton("创建新分组",
                        (d, w) -> copyIntoNew(ctx, directory))
                .show();
    }

    /* ------------------------------ 导入到已有分组 ------------------------------ */

    private static void copyIntoExisting(Context ctx, Object dir, LocalDataHelper.LocalPath dst) {
        ProgressDialog pd = makeProgress(ctx);
        SyncUtils.async(() -> {
            int ok = performCopy(ctx, dir, dst, pd);
            finishProgress(ctx, pd, ok);
        });
    }

    /* ------------------------------ 导入到新建分组 ------------------------------ */

    private static void copyIntoNew(Context ctx, Object dir) {
        EditText et = new EditText(ctx);
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("输入分组名称")
                .setView(et)
                .setNegativeButton("确定导入", (d, w) -> {
                    ProgressDialog pd = makeProgress(ctx);
                    SyncUtils.async(() -> {
                        String id = RandomUtils.getRandomString(8);
                        LocalDataHelper.LocalPath lp = new LocalDataHelper.LocalPath();
                        lp.storePath = id;
                        lp.Name = et.getText().toString();
                        lp.coverName = "";
                        LocalDataHelper.addPath(lp);

                        int ok = performCopy(ctx, dir, lp, pd);
                        List<LocalDataHelper.LocalPicItems> list = LocalDataHelper.getPicItems(id);
                        if (!list.isEmpty()) LocalDataHelper.setPathCover(lp, list.get(0));
                        finishProgress(ctx, pd, ok);
                    });
                })
                .show();
    }

    /* ------------------------------ 进度条 ------------------------------ */

    private static ProgressDialog makeProgress(Context ctx) {
        ProgressDialog pd = new ProgressDialog(ctx);
        pd.setTitle("正在导入...");
        pd.setCancelable(false);
        pd.show();
        return pd;
    }

    private static void finishProgress(Context ctx, ProgressDialog pd, int ok) {
        SyncUtils.runOnUiThread(() -> {
            pd.dismiss();
            new MaterialAlertDialogBuilder(ctx)
                    .setTitle("导入完成")
                    .setMessage("成功导入 " + ok + " 张图片")
                    .setPositiveButton("确定", null)
                    .show();
            ICreator.dismissAll();
        });
    }

    /* ------------------------------ 核心复制循环 ------------------------------ */

    private static int performCopy(Context ctx, Object directory,
                                   LocalDataHelper.LocalPath dst, ProgressDialog pd) {
        Iterable<?> imgs = listImages(ctx, directory);

        // 统计总数
        int total = 0;
        for (Object ignored : imgs) total++;

        int done = 0, ok = 0;
        for (Object obj : listImages(ctx, directory)) {
            try {
                if (obj instanceof File) {
                    importFile(ctx, (File) obj, dst);
                } else if (obj instanceof DocumentFile) {
                    importFile(ctx, (DocumentFile) obj, dst);
                }
                ok++;
            } catch (Exception e) {
                Logger.e("import", e);
            } finally {
                done++;
                int d = done, o = ok, t = total;
                SyncUtils.runOnUiThread(
                        () -> pd.setMessage("已完成 " + d + "/" + t + ", 有效 " + o)
                );
            }
        }
        return ok;
    }

    /* ------------------------------ 单文件复制 ------------------------------ */

    private static void importFile(Context ctx, File src,
                                   LocalDataHelper.LocalPath dstPath) {
        LocalDataHelper.LocalPicItems item =
                buildItem(DataUtils.getFileMD5(src), extractExt(src.getName()));
        FileUtils.copy(src.getAbsolutePath(),
                LocalDataHelper.getLocalItemPath(dstPath, item));
        LocalDataHelper.addPicItem(dstPath.storePath, item);
    }

    private static void importFile(Context ctx, DocumentFile src,
                                   LocalDataHelper.LocalPath dstPath)
            throws IOException, NoSuchAlgorithmException {

        String ext = extractExt(src.getName());
        MessageDigest md = MessageDigest.getInstance("MD5");

        // 先用随机名写入，计算 MD5
        String tmpId = RandomUtils.getRandomString(16);
        LocalDataHelper.LocalPicItems tmpItem = buildItem(tmpId, ext);
        String tmpPath = LocalDataHelper.getLocalItemPath(dstPath, tmpItem);

        try (InputStream in = ctx.getContentResolver().openInputStream(src.getUri());
             DigestInputStream din = new DigestInputStream(in, md);
             FileOutputStream out = new FileOutputStream(tmpPath)) {

            byte[] buf = new byte[8192];
            int len;
            while ((len = din.read(buf)) != -1) out.write(buf, 0, len);
        }

        String md5 = bytesToHex(md.digest());
        LocalDataHelper.LocalPicItems realItem = buildItem(md5, ext);
        String realPath = LocalDataHelper.getLocalItemPath(dstPath, realItem);

        // 尝试重命名到最终文件名
        if (!new File(tmpPath).renameTo(new File(realPath))) {
            Logger.w("import", "重命名失败, 使用随机文件名保存");
            realItem = tmpItem;
        }
        LocalDataHelper.addPicItem(dstPath.storePath, realItem);
    }

    /* -------------------------- LocalPicItems 构造 -------------------------- */

    private static LocalDataHelper.LocalPicItems buildItem(String id, String ext) {
        LocalDataHelper.LocalPicItems it = new LocalDataHelper.LocalPicItems();
        it.id = id;
        it.url = "";
        it.thumbUrl = "";
        it.addTime = System.currentTimeMillis();
        it.fileName = id + (ext == null ? "" : ext);
        return it;
    }

    /* ---------------------- 文件 & 图片判断 / 列表 ----------------------- */

    private static Iterable<?> listImages(Context ctx, Object directory) {
        List<Object> out = new ArrayList<>();
        Deque<Object> dq = new ArrayDeque<>();
        dq.push(directory);

        while (!dq.isEmpty()) {
            Object dir = dq.pop();

            if (dir instanceof File) {
                File[] arr = ((File) dir).listFiles();
                if (arr == null) continue;

                for (File f : arr) {
                    if (f.isDirectory()) {
                        dq.push(f);
                    } else if (isImageFile(f.getAbsolutePath())) {
                        out.add(f);
                    }
                }

            } else if (dir instanceof DocumentFile) {
                DocumentFile[] arr = ((DocumentFile) dir).listFiles();
                if (arr == null) continue;

                for (DocumentFile df : arr) {
                    if (df.isDirectory()) {
                        dq.push(df);
                    } else if (isImageDocumentFile(df)) {
                        out.add(df);
                    }
                }
            }
        }
        return out;
    }

    private static boolean isImageFile(String path) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opt);
        return opt.outWidth != -1;
    }

    private static boolean isImageDocumentFile(DocumentFile f) {
        String mime = f.getType();
        if (mime != null && mime.startsWith("image")) return true;

        String ext = extractExt(f.getName());
        if (ext == null) return false;

        switch (ext.toLowerCase()) {
            case ".png":
            case ".jpg":
            case ".jpeg":
            case ".gif":
            case ".webp":
            case ".bmp":
                return true;
            default:
                return false;
        }
    }

    private static String extractExt(String name) {
        if (name == null) return null;
        int i = name.lastIndexOf('.');
        return (i == -1) ? null : name.substring(i);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /* -------------------------- ICreator.IMainPanelItem -------------------------- */

    @Override
    public void onViewDestroy() {
        // no-op
    }

    @Override
    public long getID() {
        return 8888;
    }

    @Override
    public void notifyViewUpdate0() {
        // no-op
    }

    /* -------------------------- 错误弹窗 -------------------------- */

    private void showInvalidPathDialog() {
        new MaterialAlertDialogBuilder(mContext)
                .setTitle("错误")
                .setMessage("路径无效或无权限")
                .setPositiveButton("确定", null)
                .show();
    }
}
