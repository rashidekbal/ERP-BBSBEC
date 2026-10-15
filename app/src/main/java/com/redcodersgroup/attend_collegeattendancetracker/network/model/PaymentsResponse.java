package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class PaymentsResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("data")
    private List<PaymentItem> data = new ArrayList<>();

    public int getStatus() { return status; }
    public List<PaymentItem> getData() { return data != null ? data : new ArrayList<>(); }

    public static class PaymentItem {
        @SerializedName("id")
        private int id;

        @SerializedName("amount")
        private double amount;

        @SerializedName("payment_mode")
        private String paymentMode;

        @SerializedName("payment_date")
        private String paymentDate;

        @SerializedName("reference_no")
        private String referenceNo;

        @SerializedName("reference_type")
        private String referenceType;

        @SerializedName("upi_app")
        private String upiApp;

        @SerializedName("payer_name")
        private String payerName;

        @SerializedName("payer_mobile")
        private String payerMobile;

        @SerializedName("status")
        private String status;

        public int getId() { return id; }
        public double getAmount() { return amount; }
        public String getPaymentMode() { return paymentMode != null ? paymentMode : "ONLINE"; }
        public String getPaymentDate() { return paymentDate != null ? paymentDate : ""; }
        public String getReferenceNo() { return referenceNo != null ? referenceNo : ("TXN" + id); }
        public String getReferenceType() { return referenceType != null ? referenceType : ""; }
        public String getUpiApp() { return upiApp != null ? upiApp : ""; }
        public String getPayerName() { return payerName != null ? payerName : ""; }
        public String getPayerMobile() { return payerMobile != null ? payerMobile : ""; }
        public String getStatus() { return status != null ? status : "VERIFIED"; }
    }
}
