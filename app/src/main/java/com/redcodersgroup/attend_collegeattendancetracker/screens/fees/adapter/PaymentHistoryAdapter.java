package com.redcodersgroup.attend_collegeattendancetracker.screens.fees.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemPaymentRecordBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.PaymentsResponse;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder> {
    private final Context context;
    private final List<PaymentsResponse.PaymentItem> items;
    private final NumberFormat currencyFormatter;

    public PaymentHistoryAdapter(Context context, List<PaymentsResponse.PaymentItem> items) {
        this.context = context;
        this.items = items;
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPaymentRecordBinding binding = ItemPaymentRecordBinding.inflate(
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
        private final ItemPaymentRecordBinding binding;

        public ViewHolder(@NonNull ItemPaymentRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Context context, PaymentsResponse.PaymentItem item, NumberFormat formatter) {
            if (item == null) return;
            binding.txnRefText.setText("Ref: " + item.getReferenceNo());
            String mode = item.getPaymentMode();
            if (!item.getUpiApp().isEmpty()) mode += " (" + item.getUpiApp() + ")";
            binding.paymentDateText.setText(item.getPaymentDate() + " · " + mode);
            binding.paymentAmountText.setText(formatter.format(item.getAmount()));
            binding.paymentStatusBadge.setText(item.getStatus());
            if ("VERIFIED".equalsIgnoreCase(item.getStatus()) || "SUCCESS".equalsIgnoreCase(item.getStatus())) {
                binding.paymentStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success));
            } else {
                binding.paymentStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.accent));
            }
        }
    }
}
