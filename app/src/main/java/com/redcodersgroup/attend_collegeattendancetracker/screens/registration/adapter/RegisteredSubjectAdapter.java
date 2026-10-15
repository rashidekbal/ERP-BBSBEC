package com.redcodersgroup.attend_collegeattendancetracker.screens.registration.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemRegisteredSubjectBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.SemesterRegistrationResponse;
import java.util.List;

public class RegisteredSubjectAdapter extends RecyclerView.Adapter<RegisteredSubjectAdapter.ViewHolder> {
    private final Context context;
    private final List<SemesterRegistrationResponse.RegisteredSubject> items;

    public RegisteredSubjectAdapter(Context context, List<SemesterRegistrationResponse.RegisteredSubject> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRegisteredSubjectBinding binding = ItemRegisteredSubjectBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SemesterRegistrationResponse.RegisteredSubject item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRegisteredSubjectBinding binding;

        public ViewHolder(@NonNull ItemRegisteredSubjectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SemesterRegistrationResponse.RegisteredSubject item) {
            if (item == null) return;
            binding.subjectCodeText.setText(item.getSubjectCode().isEmpty() ? "SUB" : item.getSubjectCode());
            binding.subjectNameText.setText(item.getSubjectName());
            binding.creditsText.setText(item.getCredits() + " Credits");
        }
    }
}
