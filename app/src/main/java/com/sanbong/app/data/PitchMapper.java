package com.sanbong.app.data;
import com.sanbong.app.R;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.model.PitchType;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
public final class PitchMapper {
    private PitchMapper() {}
    public static Pitch fromJson(JSONObject row) {
        PitchType type;
        try { type = PitchType.valueOf(row.optString("pitch_type", "SEVEN")); } catch (IllegalArgumentException ignored) { type = PitchType.SEVEN; }
        JSONArray raw = row.optJSONArray("amenities");
        List<String> amenities = new ArrayList<>();
        if (raw != null) for (int i = 0; i < raw.length(); i++) amenities.add(raw.optString(i));
        String key = row.optString("image_key", "");
        int image = R.drawable.img_pitch_1;
        switch (key) {
            case "img_pitch_1": image = R.drawable.img_pitch_1; break;
            case "img_pitch_2": image = R.drawable.img_pitch_2; break;
            case "img_pitch_sunset": image = R.drawable.img_pitch_sunset; break;
            case "img_pitch_night": image = R.drawable.img_pitch_night; break;
            default: break;
        }
        return new Pitch(row.optString("id"), row.optString("name"), row.optString("district"), row.optString("address"), type,
                row.optLong("base_price"), row.optDouble("rating"), amenities, row.optString("description"), image, key);
    }
}
