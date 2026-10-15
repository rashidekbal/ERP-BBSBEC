package com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemSubjectRiskBinding;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model.SubjectRiskModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class SubjectRiskAdapter extends RecyclerView.Adapter<SubjectRiskAdapter.SubjectRiskViewHolder> {

    public interface OnSubjectClickListener {
        void onSubjectClick(SubjectRiskModel item);
    }

    public interface OnSubjectLongClickListener {
        void onSubjectLongClick(SubjectRiskModel item);
    }

    private final Context context;
    private final List<SubjectRiskModel> items = new ArrayList<>();
    private OnSubjectClickListener listener;
    private OnSubjectLongClickListener longClickListener;
    private boolean holdAtZero = false;

    public SubjectRiskAdapter(Context context) {
        this.context = context;
    }

    public void setListener(OnSubjectClickListener listener) {
        this.listener = listener;
    }

    public void setLongClickListener(OnSubjectLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setItems(List<SubjectRiskModel> newItems) {
        List<SubjectRiskModel> targetList = newItems != null ? newItems : new ArrayList<>();
        if (this.items.isEmpty()) {
            this.items.addAll(targetList);
            notifyDataSetChanged();
            return;
        }

        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(
                new androidx.recyclerview.widget.DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return items.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return targetList.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        SubjectRiskModel oldItem = items.get(oldItemPosition);
                        SubjectRiskModel newItem = targetList.get(newItemPosition);
                        if (oldItem.getSubject() == null || newItem.getSubject() == null) return false;
                        return oldItem.getSubject().getSid() == newItem.getSubject().getSid() &&
                                oldItem.isLab() == newItem.isLab();
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return items.get(oldItemPosition).equals(targetList.get(newItemPosition));
                    }
                }
        );

        this.items.clear();
        this.items.addAll(targetList);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public SubjectRiskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSubjectRiskBinding binding = ItemSubjectRiskBinding.inflate(
                LayoutInflater.from(context), parent, false
        );
        return new SubjectRiskViewHolder(binding);
    }

    public void resetToZero(@Nullable RecyclerView recyclerView) {
        holdAtZero = true;
        if (recyclerView == null) return;
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (vh instanceof SubjectRiskViewHolder) {
                ((SubjectRiskViewHolder) vh).resetProgressToZero();
            }
        }
    }

    public void animateAllVisible(@Nullable RecyclerView recyclerView) {
        holdAtZero = false;
        if (recyclerView == null) return;
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (vh instanceof SubjectRiskViewHolder) {
                ((SubjectRiskViewHolder) vh).animateProgressToTarget();
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectRiskViewHolder holder, int position) {
        if (position >= 0 && position < items.size()) {
            SubjectRiskModel model = items.get(position);
            holder.bind(model, listener, longClickListener, holdAtZero);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class SubjectRiskViewHolder extends RecyclerView.ViewHolder {
        private final ItemSubjectRiskBinding binding;
        private int targetPct = 0;

        public SubjectRiskViewHolder(ItemSubjectRiskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void resetProgressToZero() {
            Object prevAnimator = binding.progressBar.getTag(com.redcodersgroup.attend_collegeattendancetracker.R.id.progress_animator_tag);
            if (prevAnimator instanceof android.animation.ValueAnimator) {
                ((android.animation.ValueAnimator) prevAnimator).cancel();
            }
            binding.progressBar.setProgressCompat(0, false);
            binding.percentageText.setText("0%");
        }

        public void animateProgressToTarget() {
            com.redcodersgroup.attend_collegeattendancetracker.utils.ProgressAnimationUtil.animateProgress(
                    binding.progressBar, targetPct, binding.percentageText);
        }

        public void bind(SubjectRiskModel model, OnSubjectClickListener listener, OnSubjectLongClickListener longClickListener, boolean startAtZero) {
            Context ctx = itemView.getContext();
            binding.subjectNameText.setText(model.getSubject().getSubjectName());
            binding.subjectLabBadge.setVisibility(model.isLab() ? View.VISIBLE : View.GONE);

            String ratio = model.getAttended() + " / " + model.getConducted() + " sessions attended";
            binding.attendedRatioText.setText(ratio);

            this.targetPct = model.getPercentage();
            if (startAtZero) {
                resetProgressToZero();
            } else {
                com.redcodersgroup.attend_collegeattendancetracker.utils.ProgressAnimationUtil.animateProgress(
                        binding.progressBar, targetPct, binding.percentageText);
            }
            binding.marginStatusText.setText(model.getMarginText());

            int accentColor = ThemeManager.getAccentColorInt();
            int dangerColor = ContextCompat.getColor(ctx, R.color.danger);
            int successColor = ContextCompat.getColor(ctx, R.color.success);

            if (model.getRiskCategory() == SubjectRiskModel.RiskCategory.AT_RISK) {
                // Danger / Red
                binding.riskBadgeCard.setCardBackgroundColor(dangerColor);
                binding.percentageText.setTextColor(ContextCompat.getColor(ctx, R.color.all_white));
                binding.progressBar.setIndicatorColor(dangerColor);
                binding.marginStatusText.setTextColor(dangerColor);
            } else if (model.getRiskCategory() == SubjectRiskModel.RiskCategory.WARNING) {
                // Warning / Accent
                binding.riskBadgeCard.setCardBackgroundColor(accentColor);
                binding.percentageText.setTextColor(ContextCompat.getColor(ctx, R.color.all_white));
                binding.progressBar.setIndicatorColor(accentColor);
                binding.marginStatusText.setTextColor(accentColor);
            } else {
                // Safe / Success Green
                binding.riskBadgeCard.setCardBackgroundColor(successColor);
                binding.percentageText.setTextColor(ContextCompat.getColor(ctx, R.color.all_white));
                binding.progressBar.setIndicatorColor(successColor);
                binding.marginStatusText.setTextColor(successColor);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubjectClick(model);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onSubjectLongClick(model);
                    return true;
                }
                return false;
            });
        }
    }
}
