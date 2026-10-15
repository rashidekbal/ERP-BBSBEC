package com.redcodersgroup.attend_collegeattendancetracker.screens.fees.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemSemesterFeeCardBinding;

import java.util.List;

public class SemesterFeeAdapter extends RecyclerView.Adapter<SemesterFeeAdapter.ViewHolder> {

    public static class SemesterFeeItem {
        private final String semesterName;
        private final String statusBadge;
        private final String tuitionFee;
        private final String hostelNonAc;
        private final String hostelAc;
        private final String netPayable;
        private final String paid;
        private final String outstanding;

        public SemesterFeeItem(String semesterName, String statusBadge, String tuitionFee,
                               String hostelNonAc, String hostelAc, String netPayable,
                               String paid, String outstanding) {
            this.semesterName = semesterName;
            this.statusBadge = statusBadge;
            this.tuitionFee = tuitionFee;
            this.hostelNonAc = hostelNonAc;
            this.hostelAc = hostelAc;
            this.netPayable = netPayable;
            this.paid = paid;
            this.outstanding = outstanding;
        }

        public String getSemesterName() { return semesterName; }
        public String getStatusBadge() { return statusBadge; }
        public String getTuitionFee() { return tuitionFee; }
        public String getHostelNonAc() { return hostelNonAc; }
        public String getHostelAc() { return hostelAc; }
        public String getNetPayable() { return netPayable; }
        public String getPaid() { return paid; }
        public String getOutstanding() { return outstanding; }
    }

    private final Context context;
    private final List<SemesterFeeItem> items;

    public SemesterFeeAdapter(Context context, List<SemesterFeeItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSemesterFeeCardBinding binding = ItemSemesterFeeCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSemesterFeeCardBinding binding;

        public ViewHolder(@NonNull ItemSemesterFeeCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SemesterFeeItem item) {
            if (item == null) return;
            binding.semesterTitleText.setText(item.getSemesterName());
            binding.semesterStatusBadge.setText(item.getStatusBadge());

            if ("Paid".equalsIgnoreCase(item.getStatusBadge())) {
                binding.semesterStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"));
                binding.semesterStatusBadge.setTextColor(Color.parseColor("#15803D"));
            } else if ("Part paid".equalsIgnoreCase(item.getStatusBadge())) {
                binding.semesterStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"));
                binding.semesterStatusBadge.setTextColor(Color.parseColor("#B45309"));
            } else {
                binding.semesterStatusBadge.setBackgroundColor(Color.parseColor("#FEE2E2"));
                binding.semesterStatusBadge.setTextColor(Color.parseColor("#B91C1C"));
            }

            binding.lineItem1Label.setText("Tuition Fee *");
            binding.lineItem1Amount.setText(item.getTuitionFee());

            if (item.getHostelNonAc() != null && !item.getHostelNonAc().isEmpty()) {
                binding.lineItem2Row.setVisibility(View.VISIBLE);
                binding.lineItem2Amount.setText(item.getHostelNonAc());
            } else {
                binding.lineItem2Row.setVisibility(View.GONE);
            }

            if (item.getHostelAc() != null && !item.getHostelAc().isEmpty()) {
                binding.lineItem3Row.setVisibility(View.VISIBLE);
                binding.lineItem3Amount.setText(item.getHostelAc());
            } else {
                binding.lineItem3Row.setVisibility(View.GONE);
            }

            binding.netPayableText.setText(item.getNetPayable());
            binding.paidAmountText.setText(item.getPaid());
            binding.outstandingAmountText.setText(item.getOutstanding());
        }
    }
}
