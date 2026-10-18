package com.sanbong.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import com.sanbong.app.R;
import com.sanbong.app.model.Pitch;
import java.io.File;

public final class PitchImageLoader {
    private PitchImageLoader() {}

    public static void load(ImageView iv, Pitch pitch) {
        if (iv == null) return;
        if (pitch == null) {
            iv.setImageResource(R.drawable.img_pitch_1);
            return;
        }
        load(iv, pitch.getId(), pitch.getImageKey(), pitch.getImageResId());
    }

    public static void load(ImageView iv, String pitchId, String imageKey, int defaultResId) {
        if (iv == null) return;
        int fallback = defaultResId != 0 ? defaultResId : R.drawable.img_pitch_1;
        Context context = iv.getContext();

        if (context != null) {
            File localImg = getPitchImageFile(context, pitchId, imageKey);
            if (localImg != null && localImg.exists()) {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(localImg.getAbsolutePath());
                    if (bmp != null) {
                        iv.setImageBitmap(bmp);
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }

        int preset = resolvePresetRes(imageKey);
        iv.setImageResource(preset != 0 ? preset : fallback);
    }

    public static File getPitchImageFile(Context context, String pitchId, String imageKey) {
        if (context == null) return null;
        File dir = new File(context.getFilesDir(), "pitch_images");
        if (imageKey != null && imageKey.startsWith("local:")) {
            File f = new File(dir, imageKey.substring("local:".length()));
            if (f.exists()) return f;
        }
        if (pitchId != null && !pitchId.isEmpty()) {
            File f = new File(dir, "pitch_" + pitchId + ".jpg");
            if (f.exists()) return f;
        }
        return null;
    }

    public static int resolvePresetRes(String key) {
        if (key == null) return 0;
        switch (key) {
            case "img_pitch_1": return R.drawable.img_pitch_1;
            case "img_pitch_2": return R.drawable.img_pitch_2;
            case "img_pitch_sunset": return R.drawable.img_pitch_sunset;
            case "img_pitch_night": return R.drawable.img_pitch_night;
            default: return 0;
        }
    }
}
