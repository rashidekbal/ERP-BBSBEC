package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.subject.AdditionalSubjectInfoActivity;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

import java.util.List;

public class PendingSetupAdapter extends RecyclerView.Adapter<PendingSetupAdapter.ViewHolder> {
    private final List<SubjectSchema> subjectSchemas;
    private final Context context;

    public PendingSetupAdapter(Context context, List<SubjectSchema> subjectSchemas) {
        this.subjectSchemas = subjectSchemas;
        this.context = context;
    }

    @NonNull
    @Override
    public PendingSetupAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.subject_setup_card_rv, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PendingSetupAdapter.ViewHolder holder, int position) {
        SubjectSchema subject = subjectSchemas.get(position);
        int accentColor = ThemeManager.getAccentColorInt();

        int subColor = accentColor;
        try {
            if (subject.getUniqueColorCode() != null && !subject.getUniqueColorCode().isEmpty()) {
                subColor = Color.parseColor(subject.getUniqueColorCode());
            }
        } catch (Exception ignored) {
        }

        holder.colorDot.setCardBackgroundColor(subColor);
        holder.subjectName.setText(subject.getSubjectName());

        if (holder.pendingLabBadge != null) {
            holder.pendingLabBadge.setVisibility(subject.isLab() ? View.VISIBLE : View.GONE);
        }

        if (holder.subjectPendingSub != null) {
            String typeLabel = subject.isLab() ? "Lab Subject" : "Theory Subject";
            holder.subjectPendingSub.setText(typeLabel + " · Set schedule & target criteria");
        }

        holder.setupBtn.setBackgroundTintList(ColorStateList.valueOf(accentColor));

        View.OnClickListener openSetupClick = v -> {
            if (context instanceof AppCompatActivity) {
                context.startActivity(AdditionalSubjectInfoActivity.getAdditionalSubjectInfoActivityIntent((AppCompatActivity) context, subject.getSid()));
            }
        };

        holder.setupBtn.setOnClickListener(openSetupClick);
        holder.itemView.setOnClickListener(openSetupClick);
    }

    @Override
    public int getItemCount() {
        return subjectSchemas != null ? subjectSchemas.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        AppCompatTextView subjectName;
        AppCompatTextView pendingLabBadge;
        AppCompatTextView subjectPendingSub;
        MaterialCardView colorDot;
        MaterialButton setupBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorDot = itemView.findViewById(R.id.accent_color);
            subjectName = itemView.findViewById(R.id.subjectName);
            pendingLabBadge = itemView.findViewById(R.id.pendingLabBadge);
            subjectPendingSub = itemView.findViewById(R.id.subjectPendingSub);
            setupBtn = itemView.findViewById(R.id.setupBtn);
        }
    }
}
