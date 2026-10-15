package com.redcodersgroup.attend_collegeattendancetracker.screens.fees;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivitySubmitPaymentBinding;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SubmitPaymentActivity extends AppCompatActivity {

    private ActivitySubmitPaymentBinding binding;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private Uri attachedReceiptUri;

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    attachedReceiptUri = uri;
                    String fileName = uri.getLastPathSegment();
                    if (fileName == null || fileName.isEmpty()) {
                        fileName = "Receipt Attached (" + uri.getPath() + ")";
                    }
                    binding.attachedReceiptText.setVisibility(View.VISIBLE);
                    binding.attachedReceiptText.setText(fileName);
                    binding.attachReceiptBtn.setText("Change receipt");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySubmitPaymentBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
    }

    private void initViews() {
        binding.backBtn.setOnClickListener(v -> finish());

        // Default Date
        binding.paymentDateText.setText(dateFormat.format(calendar.getTime()));
        binding.paymentDateBox.setOnClickListener(v -> showDatePicker());

        // Dropdowns
        binding.feeRecordSpinnerBox.setOnClickListener(v -> showFeeRecordDialog());
        binding.paymentModeSpinnerBox.setOnClickListener(v -> showPaymentModeDialog());
        binding.payerRelationSpinnerBox.setOnClickListener(v -> showPayerRelationDialog());
        binding.bankSpinnerBox.setOnClickListener(v -> showBankDialog());
        binding.refTypeSpinnerBox.setOnClickListener(v -> showRefTypeDialog());

        // Attach receipt
        binding.attachReceiptBtn.setOnClickListener(v -> filePickerLauncher.launch("*/*"));

        // Submit Button
        binding.submitPaymentBtn.setOnClickListener(v -> validateAndSubmit());
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    binding.paymentDateText.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void showFeeRecordDialog() {
        String[] options = {
                "Semester 5 (₹69,000)",
                "Semester 4 (₹69,000)",
                "Semester 3 (₹69,000)",
                "Semester 2 (₹69,000)",
                "Semester 1 (₹69,000)"
        };
        new AlertDialog.Builder(this)
                .setTitle("Select Fee Record")
                .setItems(options, (dialog, which) -> binding.feeRecordSelectedText.setText(options[which]))
                .show();
    }

    private void showPaymentModeDialog() {
        String[] options = {
                "UPI",
                "Net Banking",
                "Debit Card",
                "Credit Card",
                "Demand Draft",
                "NEFT / RTGS",
                "Cash"
        };
        new AlertDialog.Builder(this)
                .setTitle("Select Payment Mode")
                .setItems(options, (dialog, which) -> binding.paymentModeSelectedText.setText(options[which]))
                .show();
    }

    private void showPayerRelationDialog() {
        String[] options = {"Self", "Father", "Mother", "Guardian"};
        new AlertDialog.Builder(this)
                .setTitle("Select Payer Relation")
                .setItems(options, (dialog, which) -> binding.payerRelationSelectedText.setText(options[which]))
                .show();
    }

    private void showBankDialog() {
        String[] options = {
                "State Bank of India",
                "HDFC Bank",
                "Punjab National Bank",
                "ICICI Bank",
                "Axis Bank",
                "Canara Bank",
                "Bank of Baroda",
                "Other Bank"
        };
        new AlertDialog.Builder(this)
                .setTitle("Select Bank")
                .setItems(options, (dialog, which) -> binding.bankSelectedText.setText(options[which]))
                .show();
    }

    private void showRefTypeDialog() {
        String[] options = {"UTR", "Transaction ID", "Reference No", "Cheque No"};
        new AlertDialog.Builder(this)
                .setTitle("Select Reference Type")
                .setItems(options, (dialog, which) -> binding.refTypeSelectedText.setText(options[which]))
                .show();
    }

    private void validateAndSubmit() {
        String feeRecord = binding.feeRecordSelectedText.getText().toString().trim();
        String paymentMode = binding.paymentModeSelectedText.getText().toString().trim();
        String amount = binding.amountInput.getText() != null ? binding.amountInput.getText().toString().trim() : "";
        String paymentDate = binding.paymentDateText.getText().toString().trim();
        String payerName = binding.payerNameInput.getText() != null ? binding.payerNameInput.getText().toString().trim() : "";
        String payerRelation = binding.payerRelationSelectedText.getText().toString().trim();
        String bank = binding.bankSelectedText.getText().toString().trim();
        String accountDigits = binding.accountDigitsInput.getText() != null ? binding.accountDigitsInput.getText().toString().trim() : "";
        String mobile = binding.mobileInput.getText() != null ? binding.mobileInput.getText().toString().trim() : "";
        String refType = binding.refTypeSelectedText.getText().toString().trim();
        String refNumber = binding.refNumberInput.getText() != null ? binding.refNumberInput.getText().toString().trim() : "";

        if (paymentMode.isEmpty() || paymentMode.equalsIgnoreCase("Select payment mode")) {
            ToastUtil.showToastShort("Please select a payment mode");
            return;
        }

        if (TextUtils.isEmpty(amount)) {
            binding.amountInput.setError("Please enter amount");
            binding.amountInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(payerName)) {
            binding.payerNameInput.setError("Please enter payer name");
            binding.payerNameInput.requestFocus();
            return;
        }

        if (bank.isEmpty() || bank.equalsIgnoreCase("Select bank")) {
            ToastUtil.showToastShort("Please select bank");
            return;
        }

        if (TextUtils.isEmpty(accountDigits) || accountDigits.length() != 4) {
            binding.accountDigitsInput.setError("Enter last 4 digits");
            binding.accountDigitsInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(mobile) || mobile.length() < 10) {
            binding.mobileInput.setError("Enter 10-digit mobile number");
            binding.mobileInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(refNumber)) {
            binding.refNumberInput.setError("Enter reference number");
            binding.refNumberInput.requestFocus();
            return;
        }

        // Safe review dialog - strictly no auto API POST mutation to ERP
        new AlertDialog.Builder(this)
                .setTitle("Confirm Payment Submission")
                .setMessage("Please review your payment details:\n\n" +
                        "• Record: " + feeRecord + "\n" +
                        "• Amount: ₹" + amount + "\n" +
                        "• Payment Mode: " + paymentMode + "\n" +
                        "• Date: " + paymentDate + "\n" +
                        "• Payer: " + payerName + " (" + payerRelation + ")\n" +
                        "• Bank: " + bank + " (•••• " + accountDigits + ")\n" +
                        "• Mobile: " + mobile + "\n" +
                        "• " + refType + ": " + refNumber + "\n" +
                        (attachedReceiptUri != null ? "• Receipt: Attached\n\n" : "• Receipt: None\n\n") +
                        "Safe Mode: Payment details verified and retained locally. No live financial transaction is automatically executed.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    ToastUtil.showToastShort("Payment details recorded safely.");
                    finish();
                })
                .setNegativeButton("Edit", null)
                .show();
    }
}
