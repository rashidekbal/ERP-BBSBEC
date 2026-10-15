package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class FeesHistoryResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public Data getData() { return data; }

    public static class Data {
        @SerializedName("total_fee")
        private double totalFee;

        @SerializedName("amount_paid")
        private double amountPaid;

        @SerializedName("balance")
        private double balance;

        @SerializedName("installments")
        private List<InstallmentItem> installments = new ArrayList<>();

        public double getTotalFee() { return totalFee; }
        public double getAmountPaid() { return amountPaid; }
        public double getBalance() { return balance; }
        public List<InstallmentItem> getInstallments() { return installments != null ? installments : new ArrayList<>(); }
    }

    public static class InstallmentItem {
        @SerializedName("installment_no")
        private int installmentNo;

        @SerializedName("head_name")
        private String headName;

        @SerializedName("amount")
        private double amount;

        @SerializedName("due_date")
        private String dueDate;

        @SerializedName("status")
        private String status;

        @SerializedName("receipt_no")
        private String receiptNo;

        public int getInstallmentNo() { return installmentNo; }
        public String getHeadName() { return headName != null ? headName : "Fee Installment"; }
        public double getAmount() { return amount; }
        public String getDueDate() { return dueDate != null ? dueDate : ""; }
        public String getStatus() { return status != null ? status : "PENDING"; }
        public String getReceiptNo() { return receiptNo != null ? receiptNo : ""; }
        public boolean isPaid() { return "PAID".equalsIgnoreCase(status); }
    }
}
