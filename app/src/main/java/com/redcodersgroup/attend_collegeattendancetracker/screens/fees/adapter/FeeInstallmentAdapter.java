package com.redcodersgroup.attend_collegeattendancetracker.screens.fees.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemFeeInstallmentBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.FeesHistoryResponse;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class FeeInstallmentAdapter extends RecyclerView.Adapter<FeeInstallmentAdapter.ViewHolder> {
    private final Context context;
    private final List<FeesHistoryResponse.InstallmentItem> items;
    private final NumberFormat currencyFormatter;

    public FeeInstallmentAdapter(Context context, List<FeesHistoryResponse.InstallmentItem> items) {
        this.context = context;
        this.items = items;
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFeeInstallmentBinding binding = ItemFeeInstallmentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(context, items.get(position), currencyFormatter);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFeeInstallmentBinding binding;

        public ViewHolder(@NonNull ItemFeeInstallmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Context context, FeesHistoryResponse.InstallmentItem item, NumberFormat formatter) {
            if (item == null) return;
            binding.headNameText.setText(item.getHeadName() + " (Inst. " + item.getInstallmentNo() + ")");
            binding.amountText.setText(formatter.format(item.getAmount()));
            binding.dueDateText.setText(item.getDueDate().isEmpty() ? "Scheduled" : "Due Date: " + item.getDueDate());

            String status = item.getStatus();
            binding.statusBadge.setText(status);
            if (item.isPaid()) {
                binding.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.success));
                if (!item.getReceiptNo().isEmpty()) {
                    binding.receiptText.setVisibility(View.VISIBLE);
                    binding.receiptText.setText("Receipt: " + item.getReceiptNo());
                } else {
                    binding.receiptText.setVisibility(View.GONE);
                }
            } else {
                binding.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.accent));
                binding.receiptText.setVisibility(View.GONE);
            }
        }
    }
}
