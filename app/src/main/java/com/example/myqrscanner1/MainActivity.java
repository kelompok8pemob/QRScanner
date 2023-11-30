package com.example.myqrscanner1;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ImageButton btnscan;
    private LinearLayout lHistory;
    private ImageButton btnclear;
    private ImageButton btnImage;
    private ImageButton btnbuatqr;
    private Switch switchBeep;
    private SharedPreferences sharedPreferences;
    private IntentIntegrator integrator;
    private Set<String> historySet; // Menggunakan LinkedHashSet untuk urutan yang dipertahankan

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        integrator = new IntentIntegrator(this);
        btnscan = findViewById(R.id.btnscan);
        lHistory = findViewById(R.id.lHistory);
        btnclear = findViewById(R.id.btnclear);
        btnImage = findViewById(R.id.btnimage);
        btnbuatqr = findViewById(R.id.btnbuatqr);

        // Find the Switch widget
        switchBeep = findViewById(R.id.switchBeep);



        lHistory.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            @Override
            public void onSwipeRight() {
                // Swipe to the right detected, remove the last item in the history
                if (!historySet.isEmpty()) {
                    String lastItem = historySet.iterator().next();
                    removeHistoryItem(lastItem);
                }
            }
        });


        btnbuatqr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent createQrIntent = new Intent(MainActivity.this, CreateQrActivity.class);
                startActivity(createQrIntent);
            }
        });


        // Inisialisasi Shared Preferences
        sharedPreferences = getSharedPreferences("ScanHistory", MODE_PRIVATE);

        // Ambil histori dari SharedPreferences saat aplikasi dimulai
        loadHistoryFromSharedPreferences();

        btnscan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Scanner();
            }
        });

        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImageFromStorage();
            }
        });

        // ImageButton untuk menghapus riwayat
        btnclear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearHistory();
            }
        });

        // Set background gambar "ic_menu_delete" pada tombol hapus
        btnclear.setBackgroundResource(android.R.drawable.ic_menu_delete);
    }

    private void loadHistoryFromSharedPreferences() {
        // Ambil histori dari SharedPreferences dalam format Set<String>
        Set<String> tempSet = sharedPreferences.getStringSet("history", new LinkedHashSet<String>());

        // Inisialisasi historySet dengan LinkedHashSet yang mempertahankan urutan
        historySet = new LinkedHashSet<>(tempSet);

        // Tampilkan histori yang diambil
        for (String item : historySet) {
            appendToHistory(item);
        }
    }

    private void Scanner() {
        // Check the state of the switch to determine whether to enable or disable the beep
        boolean isBeepEnabled = switchBeep.isChecked();

        // Tambahkan pilihan untuk mengganti kamera
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Kamera");
        builder.setItems(new CharSequence[]{"Kamera Belakang", "Kamera Depan"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    startScanWithCamera(0, isBeepEnabled); // 0 adalah kode untuk kamera belakang
                } else if (which == 1) {
                    startScanWithCamera(1, isBeepEnabled); // 1 adalah kode untuk kamera depan
                }
            }
        });
        builder.show();
    }


    private void startScanWithCamera(int cameraId, boolean isBeepEnabled) {
        integrator.setCameraId(cameraId); // Mengatur kamera belakang atau depan
        integrator.setPrompt("Volume up to Flash on");
        integrator.setBeepEnabled(isBeepEnabled); // Set the beep setting based on the switch state
        integrator.setOrientationLocked(true);
        integrator.setCaptureActivity(AnyOrientationCaptureActivity.class);
        integrator.initiateScan();
    }


    private void selectImageFromStorage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        try {
            startActivityForResult(intent, 1); // Angka 1 adalah kode permintaan untuk onActivityResult
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Aplikasi untuk memilih gambar tidak tersedia.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Memproses hasil pemindaian dari kamera
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanningResult != null) {
            String scannedText = scanningResult.getContents();
            if (scannedText != null && !scannedText.isEmpty()) {
                appendToHistory(scannedText);
            }
        }

        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    // Gunakan ZXing untuk memindai QR code dari gambar
                    Result result = scanQRCodeFromImage(imageUri);
                    if (result != null) {
                        String scannedText = result.getText();
                        appendToHistory(scannedText);
                    } else {
                        Toast.makeText(this, "Tidak ada QR code yang ditemukan", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private Result scanQRCodeFromImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] pixels = new int[width * height];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

                RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                MultiFormatReader reader = new MultiFormatReader();
                return reader.decode(binaryBitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void appendToHistory(String text) {
        // Tambahkan teks histori ke LinkedHashSet
        historySet.add(text);

        // Simpan seluruh LinkedHashSet ke SharedPreferences untuk mempertahankan urutan
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("history", historySet);
        editor.apply();

        // Buat LinearLayout untuk setiap item histori
        LinearLayout historyItemLayout = new LinearLayout(this);
        historyItemLayout.setOrientation(LinearLayout.HORIZONTAL); // Mengatur orientasi menjadi horizontal

        // Tambahkan teks histori ke LinearLayout
        TextView historyTextView = new TextView(this);
        historyTextView.setText(text);
        historyTextView.setTextSize(16);
        historyTextView.setTextColor(Color.BLACK);
        historyTextView.setPadding(10, 10, 10, 10);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        textParams.weight = 1;

        historyTextView.setLayoutParams(textParams);

        // Tambahkan tombol titik tiga (menu lebih banyak) ke LinearLayout
        ImageButton moreOptionsButton = new ImageButton(this);
        moreOptionsButton.setBackgroundResource(android.R.drawable.ic_menu_more);
        LinearLayout.LayoutParams moreOptionsButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );

        moreOptionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMoreOptionsPopup(text);
            }
        });

        historyItemLayout.addView(moreOptionsButton, moreOptionsButtonParams);

        // Tambahkan teks histori ke LinearLayout utama (historyItemLayout)
        historyItemLayout.addView(historyTextView);

        // Tambahkan historyItemLayout ke LinearLayout utama (lHistory)
        lHistory.addView(historyItemLayout, 0); // Menambahkan item di atas
    }

    private void showMoreOptionsPopup(final String text) {
        // Di sini, Anda dapat menampilkan popup menu lebih banyak untuk item histori tertentu,
        // seperti menghapus, menyalin, atau melakukan tindakan lainnya sesuai kebutuhan.
        // Anda dapat menggunakan AlertDialog atau popup menu sesuai preferensi Anda.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(new CharSequence[]{"Hapus", "Salin"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    removeHistoryItem(text);
                } else if (which == 1) {
                    copyToClipboard(text);
                }
            }
        });
        builder.show();
    }

    private void removeHistoryItem(String text) {
        // Hapus item dari histori
        historySet.remove(text);
        // Simpan perubahan histori ke SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("history", historySet);
        editor.apply();
        // Hapus item dari tampilan
        lHistory.removeAllViews();
        for (String item : historySet) {
            appendToHistory(item);
        }
    }

    private void clearHistory() {
        // Kosongkan histori dari LinkedHashSet
        historySet.clear();
        // Simpan perubahan histori ke SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("history", historySet);
        editor.apply();
        // Kosongkan tampilan
        lHistory.removeAllViews();
    }



    private void copyToClipboard(String text) {
        if (!TextUtils.isEmpty(text)) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Teks disalin ke clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}
