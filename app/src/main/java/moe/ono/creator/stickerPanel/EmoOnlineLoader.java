package moe.ono.creator.stickerPanel;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import moe.ono.util.DataUtils;
import moe.ono.util.HttpUtils;
import moe.ono.util.Logger;

public class EmoOnlineLoader {
    public static ExecutorService syncThread = Executors.newFixedThreadPool(16);

    public static void submit(EmoPanel.EmoInfo info, Runnable run) {
        syncThread.submit(() -> {
            try {
                String CacheDir = Env.app_save_path + "/Cache/img_" + info.MD5;
                if (info.MD5.equals(DataUtils.getFileMD5(new File(CacheDir)))) {
                    info.Path = CacheDir;
                    new Handler(Looper.getMainLooper()).post(run);
                    return;
                }
                Logger.d("del CacheDir", String.valueOf(new File(CacheDir).delete()));

                HttpUtils.DownloadToFile(info.URL, CacheDir);
                info.Path = CacheDir;
                new Handler(Looper.getMainLooper()).post(run);
            } catch (Throwable th) {
                new Handler(Looper.getMainLooper()).post(run);
            }

        });
    }

}
