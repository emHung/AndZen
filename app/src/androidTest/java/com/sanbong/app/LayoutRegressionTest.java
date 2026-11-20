package com.sanbong.app;

import android.app.Activity;
import android.content.Intent;
import android.security.NetworkSecurityPolicy;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.platform.app.InstrumentationRegistry;
import com.sanbong.app.ui.AuthActivity;
import com.sanbong.app.ui.adapter.AdapterUpdates;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LayoutRegressionTest {
    @Test public void everyScreenAndDialogInflatesWithTheAppTheme() {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(instrumentation.getTargetContext(), AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = instrumentation.startActivitySync(intent);
        try {
            instrumentation.runOnMainSync(() -> {
                int[] layouts = {R.layout.activity_admin, R.layout.activity_auth, R.layout.activity_booking_confirm, R.layout.activity_booking_detail, R.layout.activity_booking_success, R.layout.activity_edit_profile, R.layout.activity_main, R.layout.activity_notifications, R.layout.activity_pitch_detail, R.layout.activity_pitch_filter, R.layout.dialog_admin_pitch_form, R.layout.dialog_cancel_booking, R.layout.dialog_option_picker, R.layout.dialog_reschedule_slot, R.layout.fragment_favorites, R.layout.fragment_my_bookings, R.layout.fragment_pitch_list, R.layout.fragment_profile, R.layout.item_admin_pitch, R.layout.item_booking_card, R.layout.item_date_chip, R.layout.item_favorite_pitch, R.layout.item_notification, R.layout.item_pitch_card, R.layout.item_time_slot, R.layout.section_admin_pitches};
                for (int layout : layouts) {
                    View view = activity.getLayoutInflater().inflate(layout, null);
                    assertNotNull(activity.getResources().getResourceName(layout), view);
                    view.measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(1280, View.MeasureSpec.AT_MOST));
                }
                View profile = activity.getLayoutInflater().inflate(R.layout.fragment_profile, null);
                assertTrue(profile.findViewById(R.id.tvProfilePhone) instanceof TextView);
                View edit = activity.getLayoutInflater().inflate(R.layout.activity_edit_profile, null);
                assertTrue(edit.findViewById(R.id.btnSelectDistrict) instanceof TextView);
                assertTrue(edit.findViewById(R.id.btnSelectBirthdate) instanceof TextView);
                assertTrue(edit.findViewById(R.id.btnSelectGender) instanceof TextView);
                assertEquals("Tổng số sân\n3", activity.getString(R.string.ui_message_9cfdfd8462, "3"));
            });
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    @Test public void debugHttpIsRestrictedToLocalDevelopmentHosts() {
        NetworkSecurityPolicy policy = NetworkSecurityPolicy.getInstance();
        assertTrue(policy.isCleartextTrafficPermitted("127.0.0.1"));
        assertTrue(policy.isCleartextTrafficPermitted("10.0.2.2"));
        assertFalse(policy.isCleartextTrafficPermitted("example.com"));
        assertFalse(policy.isCleartextTrafficPermitted("192.168.1.1"));
    }

    @Test public void listReplacementDispatchesItemChangesInsteadOfGlobalRefresh() {
        List<String> rows = new ArrayList<>(Arrays.asList("a", "b"));
        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<>() {
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
                throw new UnsupportedOperationException("No views needed for update notification test");
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) { }
            @Override public int getItemCount() { return rows.size(); }
        };
        int[] events = new int[3];
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() { events[0]++; }
            @Override public void onItemRangeInserted(int start, int count) { events[1] += count; }
            @Override public void onItemRangeRemoved(int start, int count) { events[2] += count; }
        });
        AdapterUpdates.replace(adapter, rows, Arrays.asList("b", "c"), value -> value);
        assertEquals(Arrays.asList("b", "c"), rows);
        assertArrayEquals(new int[] {0, 1, 1}, events);
    }
}
