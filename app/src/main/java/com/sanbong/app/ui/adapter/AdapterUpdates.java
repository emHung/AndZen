package com.sanbong.app.ui.adapter;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class AdapterUpdates {
    private AdapterUpdates() {}

    public static <T> void replace(RecyclerView.Adapter<?> adapter, List<T> current,
                                   List<T> incoming, Function<T, String> key) {
        List<T> previous = new ArrayList<>(current);
        List<T> next = incoming == null ? new ArrayList<>() : new ArrayList<>(incoming);
        DiffUtil.DiffResult changes = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return previous.size(); }
            @Override public int getNewListSize() { return next.size(); }
            @Override public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return Objects.equals(key.apply(previous.get(oldPosition)), key.apply(next.get(newPosition)));
            }
            @Override public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return Objects.equals(previous.get(oldPosition), next.get(newPosition));
            }
        });
        current.clear();
        current.addAll(next);
        changes.dispatchUpdatesTo(adapter);
    }
}
