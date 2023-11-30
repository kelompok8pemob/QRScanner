package com.example.myqrscanner1;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.HashSet;
import java.util.Set;

public class CreateQrActivity extends AppCompatActivity {

    private EditText edtQrText;
    private ImageView imgQrCode;
    private Button btnSaveQr;
    private Button btnGenerateQr;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_qr);

        edtQrText = findViewById(R.id.edtQrText);
        imgQrCode = findViewById(R.id.imgQrCode);
        btnSaveQr = findViewById(R.id.btnSaveQr);
        btnGenerateQr = findViewById(R.id.btnGenerateQr);

        // Inisialisasi SharedPreferences
        sharedPreferences = getSharedPreferences("QrCodeInfo", MODE_PRIVATE);

        imgQrCode.setVisibility(View.GONE); // Sembunyikan ImageView saat awal

        btnSaveQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Kembali ke MainActivity
                onBackPressed();
            }
        });

        btnGenerateQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateQrCode();
            }
        });
    }

    private void generateQrCode() {
        String qrText = edtQrText.getText().toString().trim();

        if (!qrText.isEmpty()) {
            try {
                MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                BitMatrix bitMatrix = multiFormatWriter.encode(qrText, BarcodeFormat.QR_CODE, 500, 500);
                Bitmap bitmap = Bitmap.createBitmap(bitMatrix.getWidth(), bitMatrix.getHeight(), Bitmap.Config.ARGB_8888);

                for (int x = 0; x < bitMatrix.getWidth(); x++) {
                    for (int y = 0; y < bitMatrix.getHeight(); y++) {
                        bitmap.setPixel(x, y, bitMatrix.get(x, y) ? getResources().getColor(R.color.black) : getResources().getColor(R.color.white));
                    }
                }

                // Set gambar QR code ke ImageView
                imgQrCode.setImageBitmap(bitmap);
                // Tampilkan ImageView
                imgQrCode.setVisibility(View.VISIBLE);

                // Simpan informasi QR code ke SharedPreferences
                saveQrCodeInfo(qrText);
                Toast.makeText(this, "QR code berhasil dihasilkan", Toast.LENGTH_SHORT).show();
            } catch (WriterException e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal menghasilkan QR code", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Masukkan teks untuk menghasilkan QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQrCodeInfo(String qrText) {
        Set<String> qrCodeSet = sharedPreferences.getStringSet("qrCodes", new HashSet<String>());
        qrCodeSet.add("Nama File: qr_code_" + System.currentTimeMillis() + ".png, Teks: " + qrText);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("qrCodes", qrCodeSet);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        // Kembali ke MainActivity
        super.onBackPressed();
    }
}
