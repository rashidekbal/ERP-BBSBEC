package com.redcodersgroup.attend_collegeattendancetracker.screens.registration.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemSelectableSubjectBinding;

import java.util.List;

public class SelectableSubjectAdapter extends RecyclerView.Adapter<SelectableSubjectAdapter.ViewHolder> {

    public static class SelectableSubjectItem {
        private final String title;
        private final String subtitle;
        private boolean isSelected;

        public SelectableSubjectItem(String title, String subtitle, boolean isSelected) {
            this.title = title;
            this.subtitle = subtitle;
            this.isSelected = isSelected;
        }

        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public boolean isSelected() { return isSelected; }
        public void setSelected(boolean selected) { isSelected = selected; }
    }

    private final Context context;
    private final List<SelectableSubjectItem> items;

    public SelectableSubjectAdapter(Context context, List<SelectableSubjectItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSelectableSubjectBinding binding = ItemSelectableSubjectBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SelectableSubjectItem item = items.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> {
            item.setSelected(!item.isSelected());
            notifyItemChanged(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSelectableSubjectBinding binding;

        public ViewHolder(@NonNull ItemSelectableSubjectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SelectableSubjectItem item) {
            if (item == null) return;
            binding.subjectTitleText.setText(item.getTitle());
            binding.subjectSubtitleText.setText(item.getSubtitle());
            binding.subjectCheckbox.setChecked(item.isSelected());

            if (item.isSelected()) {
                binding.subjectCard.setCardBackgroundColor(Color.parseColor("#F4F6FE"));
                binding.subjectCard.setStrokeColor(Color.parseColor("#818CF8"));
            } else {
                binding.subjectCard.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
                binding.subjectCard.setStrokeColor(Color.parseColor("#E2E8F0"));
            }
        }
    }
}
