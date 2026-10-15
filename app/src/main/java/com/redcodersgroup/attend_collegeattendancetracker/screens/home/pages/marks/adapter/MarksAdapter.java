package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.marks.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.MarksCardRvBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.marks.MarksSchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MarksAdapter extends RecyclerView.Adapter<MarksAdapter.ViewHolder> {
    private final Context context;
    private final List<MarksSchema> marksSchemaList;
    private int maxMarks = 24;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MarksSchema item, int midNumber);
    }

    public MarksAdapter(Context context, List<MarksSchema> marksSchemaList) {
        this.context = context;
        this.marksSchemaList = marksSchemaList;
        setHasStableIds(true);
    }

    public void setMaxMarks(int maxMarks) {
        this.maxMarks = maxMarks <= 0 ? 24 : maxMarks;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public long getItemId(int position) {
        return marksSchemaList.get(position).getMarksId();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MarksCardRvBinding binding = MarksCardRvBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MarksSchema item = marksSchemaList.get(position);
        holder.bind(item, maxMarks, listener, context);
    }

    @Override
    public int getItemCount() {
        return marksSchemaList != null ? marksSchemaList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final MarksCardRvBinding binding;

        public ViewHolder(@NonNull MarksCardRvBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(MarksSchema item, int maxMarks, OnItemClickListener listener, Context context) {
            binding.subjectName.setText(item.getSubjectName());
            binding.mid1Marks.setText(formMarks(item.getMid1(), maxMarks));
            binding.mid2Marks.setText(formMarks(item.getMid2(), maxMarks));
            binding.mid3Marks.setText(formMarks(item.getMid3(), maxMarks));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item, 1);
                }
            });

            if (listener != null) {
                binding.mid1Marks.setOnClickListener(v -> listener.onItemClick(item, 1));
                binding.mid2Marks.setOnClickListener(v -> listener.onItemClick(item, 2));
                binding.mid3Marks.setOnClickListener(v -> listener.onItemClick(item, 3));
            } else {
                binding.mid1Marks.setOnClickListener(null);
                binding.mid2Marks.setOnClickListener(null);
                binding.mid3Marks.setOnClickListener(null);
            }
        }

        private static void showMarksOptionsDialog(Context context, MarksSchema item, int maxMarks) {
            if (!(context instanceof androidx.fragment.app.FragmentActivity)) return;
            androidx.fragment.app.FragmentManager fm = ((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager();

            java.util.List<com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem> options = new java.util.ArrayList<>();
            options.add(new com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem(
                    "edit", "Edit Marks", "Enter or update mid semester marks", R.drawable.ic_edit, false
            ));

            if (item.getMid1() >= 0) {
                options.add(new com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem(
                        "clear_1", "Clear Mid 1 Marks", "Current: " + formMarks(item.getMid1(), maxMarks), R.drawable.ic_delete, true
                ));
            }
            if (item.getMid2() >= 0) {
                options.add(new com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem(
                        "clear_2", "Clear Mid 2 Marks", "Current: " + formMarks(item.getMid2(), maxMarks), R.drawable.ic_delete, true
                ));
            }
            if (item.getMid3() >= 0) {
                options.add(new com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem(
                        "clear_3", "Clear Mid 3 Marks", "Current: " + formMarks(item.getMid3(), maxMarks), R.drawable.ic_delete, true
                ));
            }
            if (item.getMid1() >= 0 || item.getMid2() >= 0 || item.getMid3() >= 0) {
                options.add(new com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem(
                        "clear_all", "Clear All Marks", "Reset all mid semester marks", R.drawable.ic_delete, true
                ));
            }

            com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionsModalBottomSheet.show(
                    fm,
                    item.getSubjectName(),
                    "Marks Management",
                    options,
                    optionId -> {
                        if ("edit".equals(optionId)) {
                            com.redcodersgroup.attend_collegeattendancetracker.screens.addMarks.AddMarksDialogFragment.showDialog(
                                    fm,
                                    item.getSubjectName(),
                                    1
                            );
                        } else if ("clear_1".equals(optionId)) {
                            confirmClearMid(context, item, 1, fm);
                        } else if ("clear_2".equals(optionId)) {
                            confirmClearMid(context, item, 2, fm);
                        } else if ("clear_3".equals(optionId)) {
                            confirmClearMid(context, item, 3, fm);
                        } else if ("clear_all".equals(optionId)) {
                            confirmClearAll(context, item, fm);
                        }
                    }
            );
        }

        private static void confirmClearMid(Context context, MarksSchema item, int midNumber, androidx.fragment.app.FragmentManager fm) {
            com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showDeleteConfirm(
                    fm,
                    "Clear Mid " + midNumber + " Marks?",
                    "Are you sure you want to clear Mid " + midNumber + " marks for " + item.getSubjectName() + "?",
                    () -> {
                        DbHelper.clearMidMarks(item.getSid(), midNumber, () -> {
                            ToastUtil.showToastShort("Mid " + midNumber + " marks cleared for " + item.getSubjectName());
                        });
                    }
            );
        }

        private static void confirmClearAll(Context context, MarksSchema item, androidx.fragment.app.FragmentManager fm) {
            com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showDeleteConfirm(
                    fm,
                    "Clear All Marks?",
                    "Are you sure you want to clear all mid semester marks for " + item.getSubjectName() + "?",
                    () -> {
                        DbHelper.clearAllMarksForSubject(item.getSid(), () -> {
                            ToastUtil.showToastShort("All marks cleared for " + item.getSubjectName());
                        });
                    }
            );
        }

        private static String formMarks(double marks, int maxMarks) {
            if (marks < 0) return "---";
            String scoreStr;
            if (marks == (long) marks) {
                scoreStr = String.format(Locale.US, "%d", (long) marks);
            } else {
                scoreStr = String.format(Locale.US, "%.1f", marks);
            }
            return scoreStr + "/" + maxMarks;
        }
    }
}
