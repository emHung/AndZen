package com.sanbong.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.sanbong.app.R;
import com.sanbong.app.util.PitchImageLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class AdminPitchFormDialog {
    private final AppCompatActivity activity;
    private final JSONObject current;
    private final Consumer<JSONObject> onSave;
    private final Runnable onRequestPickImage;
    private AlertDialog dialog;

    private ImageView ivPreview;
    private TextView badgeStatus;
    private EditText etId, etName, etPrice, etDistrict, etAddress, etAmenities, etDescription;
    private Spinner spType;
    private SwitchMaterial swActive;
    private TextView tvError;
    private TextView[] presetChips;

    private String selectedPresetKey = "img_pitch_1";
    private Bitmap customPickedBitmap = null;
    private Uri customPickedUri = null;

    public AdminPitchFormDialog(AppCompatActivity activity, JSONObject current,
                                Runnable onRequestPickImage, Consumer<JSONObject> onSave) {
        this.activity = activity;
        this.current = current;
        this.onRequestPickImage = onRequestPickImage;
        this.onSave = onSave;
    }

    public void show() {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_admin_pitch_form, null);
        initViews(view);
        bindData();
        setupListeners(view);

        dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        dialog.show();
        if (dialog.getWindow() != null) {
            int displayHeight = activity.getResources().getDisplayMetrics().heightPixels;
            int displayWidth = activity.getResources().getDisplayMetrics().widthPixels;
            dialog.getWindow().setLayout((int) (displayWidth * 0.94), (int) (displayHeight * 0.88));
        }
    }

    public void handleImagePicked(Uri uri) {
        if (uri == null) return;
        this.customPickedUri = uri;
        activity.runOnUiThread(() -> {
            try (InputStream boundsInput = activity.getContentResolver().openInputStream(uri)) {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(boundsInput, null, bounds);
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = Math.max(1, Math.max(bounds.outWidth, bounds.outHeight) / 1024);
                try (InputStream stream = activity.getContentResolver().openInputStream(uri)) {
                    Bitmap bmp = BitmapFactory.decodeStream(stream, null, opts);
                    if (bmp != null) {
                        this.customPickedBitmap = bmp;
                        this.selectedPresetKey = null;
                        ivPreview.setImageBitmap(bmp);
                        badgeStatus.setText("✓ Đã chọn ảnh từ máy");
                        badgeStatus.setBackgroundResource(R.drawable.bg_tag_light_green);
                        badgeStatus.setTextColor(ContextCompat.getColor(activity, R.color.primary_green));
                        highlightPresetChip(null);
                    }
                }
            } catch (Exception ex) {
                Toast.makeText(activity, "Không đọc được ảnh: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews(View v) {
        ivPreview = v.findViewById(R.id.formPitchImagePreview);
        badgeStatus = v.findViewById(R.id.formPitchImageStatusBadge);
        etId = v.findViewById(R.id.formPitchId);
        etName = v.findViewById(R.id.formPitchName);
        spType = v.findViewById(R.id.formPitchTypeSpinner);
        etPrice = v.findViewById(R.id.formPitchPrice);
        etDistrict = v.findViewById(R.id.formPitchDistrict);
        etAddress = v.findViewById(R.id.formPitchAddress);
        etAmenities = v.findViewById(R.id.formPitchAmenities);
        etDescription = v.findViewById(R.id.formPitchDescription);
        swActive = v.findViewById(R.id.formPitchActiveSwitch);
        tvError = v.findViewById(R.id.formPitchError);

        presetChips = new TextView[]{
                v.findViewById(R.id.chipPreset1),
                v.findViewById(R.id.chipPreset2),
                v.findViewById(R.id.chipPresetSunset),
                v.findViewById(R.id.chipPresetNight)
        };

        spType.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Sân 5 người", "Sân 7 người", "Sân 11 người"}));
    }

    private void bindData() {
        TextView tvTitle = dialogTitleView();
        if (current == null) {
            etId.setEnabled(true);
            selectedPresetKey = "img_pitch_1";
            ivPreview.setImageResource(R.drawable.img_pitch_1);
            badgeStatus.setText("✓ Ảnh mẫu 1");
            highlightPresetChip("img_pitch_1");
            swActive.setChecked(true);
        } else {
            etId.setText(current.optString("id"));
            etId.setEnabled(false);
            etId.setAlpha(0.65f);
            etName.setText(current.optString("name"));
            etDistrict.setText(current.optString("district"));
            etAddress.setText(current.optString("address"));
            etPrice.setText(String.valueOf(current.optLong("base_price", 300000)));
            etDescription.setText(current.optString("description"));
            etAmenities.setText(joinAmenities(current.optJSONArray("amenities")));
            swActive.setChecked(current.optBoolean("is_active", true));

            int typeIdx = Arrays.asList("FIVE", "SEVEN", "ELEVEN").indexOf(current.optString("pitch_type"));
            if (typeIdx >= 0) spType.setSelection(typeIdx);

            String key = current.optString("image_key", "");
            loadExistingImage(current.optString("id"), key);
        }
    }

    private void loadExistingImage(String id, String key) {
        File localFile = PitchImageLoader.getPitchImageFile(activity, id, key);
        if (localFile != null && localFile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(localFile.getAbsolutePath());
            if (bmp != null) {
                ivPreview.setImageBitmap(bmp);
                badgeStatus.setText("✓ Ảnh từ máy");
                badgeStatus.setBackgroundResource(R.drawable.bg_tag_light_green);
                selectedPresetKey = null;
                highlightPresetChip(null);
                return;
            }
        }
        int preset = PitchImageLoader.resolvePresetRes(key);
        if (preset != 0) {
            selectedPresetKey = key;
            ivPreview.setImageResource(preset);
            badgeStatus.setText("✓ Ảnh mẫu");
            highlightPresetChip(key);
        } else {
            selectedPresetKey = "img_pitch_1";
            ivPreview.setImageResource(R.drawable.img_pitch_1);
            badgeStatus.setText("✓ Ảnh mặc định");
            highlightPresetChip("img_pitch_1");
        }
    }

    private void setupListeners(View v) {
        v.findViewById(R.id.btnPickImageDevice).setOnClickListener(btn -> {
            if (onRequestPickImage != null) onRequestPickImage.run();
        });

        presetChips[0].setOnClickListener(c -> selectPreset("img_pitch_1", R.drawable.img_pitch_1, "✓ Ảnh mẫu 1"));
        presetChips[1].setOnClickListener(c -> selectPreset("img_pitch_2", R.drawable.img_pitch_2, "✓ Ảnh mẫu 2"));
        presetChips[2].setOnClickListener(c -> selectPreset("img_pitch_sunset", R.drawable.img_pitch_sunset, "✓ Ảnh hoàng hôn"));
        presetChips[3].setOnClickListener(c -> selectPreset("img_pitch_night", R.drawable.img_pitch_night, "✓ Ảnh ban đêm"));

        setupAmenityTag(v, R.id.tagAmenityLed, "Đèn LED");
        setupAmenityTag(v, R.id.tagAmenityWater, "Nước miễn phí");
        setupAmenityTag(v, R.id.tagAmenityParking, "Bãi xe rộng");
        setupAmenityTag(v, R.id.tagAmenityBib, "Áo bib");
        setupAmenityTag(v, R.id.tagAmenityShower, "Phòng thay đồ");

        v.findViewById(R.id.btnCancelPitchForm).setOnClickListener(b -> dialog.dismiss());
        v.findViewById(R.id.btnSavePitchForm).setOnClickListener(b -> validateAndSave());
    }

    private void selectPreset(String key, int resId, String label) {
        customPickedBitmap = null;
        customPickedUri = null;
        selectedPresetKey = key;
        ivPreview.setImageResource(resId);
        badgeStatus.setText(label);
        badgeStatus.setBackgroundResource(R.drawable.bg_tag_light_green);
        badgeStatus.setTextColor(ContextCompat.getColor(activity, R.color.primary_green));
        highlightPresetChip(key);
    }

    private void highlightPresetChip(String key) {
        String[] keys = {"img_pitch_1", "img_pitch_2", "img_pitch_sunset", "img_pitch_night"};
        for (int i = 0; i < presetChips.length; i++) {
            boolean selected = keys[i].equals(key);
            presetChips[i].setBackgroundResource(selected ? R.drawable.bg_chip_selected_pill : R.drawable.bg_input_field);
            presetChips[i].setTextColor(ContextCompat.getColor(activity, selected ? R.color.white : R.color.text_secondary));
        }
    }

    private void setupAmenityTag(View v, int tagId, String text) {
        v.findViewById(tagId).setOnClickListener(view -> {
            String curr = etAmenities.getText().toString().trim();
            if (curr.isEmpty()) etAmenities.setText(text);
            else if (!curr.toLowerCase().contains(text.toLowerCase())) etAmenities.setText(curr + ", " + text);
        });
    }

    private void validateAndSave() {
        String id = etId.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String district = etDistrict.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (id.isEmpty() || name.isEmpty() || priceStr.isEmpty() || district.isEmpty() || address.isEmpty()) {
            showError("Vui lòng nhập đầy đủ các trường có dấu (*)");
            return;
        }

        long price;
        try {
            price = Long.parseLong(priceStr);
            if (price <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            showError("Giá cơ bản phải là số nguyên lớn hơn 0");
            return;
        }

        String imageKeyToSave = selectedPresetKey;
        if (customPickedBitmap != null) {
            String savedKey = saveCustomImageLocally(id, customPickedBitmap);
            if (savedKey != null) imageKeyToSave = savedKey;
        }

        try {
            JSONObject body = new JSONObject();
            if (current == null) body.put("id", id);
            body.put("name", name)
                    .put("district", district)
                    .put("address", address)
                    .put("pitch_type", new String[]{"FIVE", "SEVEN", "ELEVEN"}[spType.getSelectedItemPosition()])
                    .put("base_price", price)
                    .put("rating", current == null ? 0 : current.optDouble("rating", 0))
                    .put("description", etDescription.getText().toString().trim())
                    .put("image_key", imageKeyToSave == null ? JSONObject.NULL : imageKeyToSave)
                    .put("is_active", swActive.isChecked());

            JSONArray amenityList = new JSONArray();
            for (String part : etAmenities.getText().toString().split(",")) {
                if (!part.trim().isEmpty()) amenityList.put(part.trim());
            }
            body.put("amenities", amenityList);

            onSave.accept(body);
            dialog.dismiss();
        } catch (Exception ex) {
            showError("Lỗi cấu trúc dữ liệu: " + ex.getMessage());
        }
    }

    private String saveCustomImageLocally(String pitchId, Bitmap bmp) {
        try {
            File dir = new File(activity.getFilesDir(), "pitch_images");
            if (!dir.exists()) dir.mkdirs();
            String fileName = "pitch_" + pitchId + ".jpg";
            File file = new File(dir, fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            }
            return "local:" + fileName;
        } catch (Exception ex) {
            return selectedPresetKey;
        }
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private TextView dialogTitleView() {
        return dialog != null ? dialog.findViewById(R.id.formPitchTitle) : null;
    }

    private String joinAmenities(JSONArray arr) {
        if (arr == null) return "";
        List<String> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) list.add(arr.optString(i));
        return String.join(", ", list);
    }
}
