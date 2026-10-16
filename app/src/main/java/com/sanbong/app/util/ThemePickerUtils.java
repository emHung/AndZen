package com.sanbong.app.util;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import com.sanbong.app.R;

import java.util.Calendar;

public final class ThemePickerUtils {

    public interface OnOptionSelectedListener {
        void onSelected(String option);
    }

    private ThemePickerUtils() {}

    public static void showOptionPicker(Context context, String title, String subtitle,
                                        int iconRes, String[] options, String currentSelection,
                                        OnOptionSelectedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_option_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ((TextView) view.findViewById(R.id.tvPickerTitle)).setText(title);
        ((TextView) view.findViewById(R.id.tvPickerSubtitle)).setText(subtitle);
        ((ImageView) view.findViewById(R.id.ivPickerIcon)).setImageResource(iconRes);

        RadioGroup rg = view.findViewById(R.id.rgPickerOptions);
        for (int i = 0; i < options.length; i++) {
            String opt = options[i];
            RadioButton rb = new RadioButton(context);
            rb.setText(opt);
            rb.setTextSize(14f);
            rb.setTextColor(context.getResources().getColor(R.color.text_primary));
            rb.setBackgroundResource(R.drawable.bg_input_field);
            rb.setPadding(24, 24, 24, 24);
            rb.setButtonTintList(context.getColorStateList(R.color.primary_green));
            RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 14;
            rb.setLayoutParams(lp);

            if (opt.equalsIgnoreCase(currentSelection)) {
                rb.setChecked(true);
            }

            rb.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onSelected(opt);
            });
            rg.addView(rb);
        }

        view.findViewById(R.id.btnPickerClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public static void showDatePicker(Context context, Calendar initial, DatePickerDialog.OnDateSetListener listener) {
        showDatePicker(context, initial, null, null, listener);
    }

    public static void showDatePicker(Context context, Calendar initial, Calendar minimum,
                                      Calendar maximum, DatePickerDialog.OnDateSetListener listener) {
        DatePickerDialog dp = new DatePickerDialog(context, R.style.ThemeOverlay_SanBong_DatePicker, listener,
                initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        if (minimum != null) dp.getDatePicker().setMinDate(minimum.getTimeInMillis());
        if (maximum != null) dp.getDatePicker().setMaxDate(maximum.getTimeInMillis());
        dp.setButton(android.content.DialogInterface.BUTTON_POSITIVE, "Chọn", (dialog, which) -> {
            android.widget.DatePicker picker = dp.getDatePicker();
            listener.onDateSet(picker, picker.getYear(), picker.getMonth(), picker.getDayOfMonth());
        });
        dp.setButton(android.content.DialogInterface.BUTTON_NEGATIVE, "Hủy", (dialog, which) -> dp.dismiss());
        dp.setOnShowListener(dialog -> {
            android.widget.Button confirm = dp.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            android.widget.Button cancel = dp.getButton(android.content.DialogInterface.BUTTON_NEGATIVE);
            confirm.setTextColor(context.getColor(R.color.primary_green));
            cancel.setTextColor(context.getColor(R.color.text_secondary));
            confirm.setVisibility(android.view.View.VISIBLE);
            cancel.setVisibility(android.view.View.VISIBLE);
        });
        dp.show();
    }
}
