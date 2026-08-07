package com.artverse.app.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Renders an on-screen view (the receipt card) into a single-page PDF and
 * saves it to the device's public Downloads folder, so receipts can be kept
 * as evidence outside the app. Uses MediaStore on Android 10+ (no permission
 * needed); on older devices it writes to the legacy Downloads directory,
 * which requires WRITE_EXTERNAL_STORAGE (requested by ReceiptActivity).
 */
public final class ReceiptPdfWriter {

    /** A4 width in PostScript points; the view is scaled to fit it. */
    private static final int PAGE_WIDTH = 595;
    private static final int MIN_PAGE_HEIGHT = 842;

    private ReceiptPdfWriter() { }

    /** @return the display name the receipt was saved under. */
    public static String saveToDownloads(Context context, View content, String fileName) throws IOException {
        if (content.getWidth() == 0 || content.getHeight() == 0) {
            throw new IOException("Receipt is not ready yet");
        }

        PdfDocument document = new PdfDocument();
        try {
            float scale = (float) PAGE_WIDTH / content.getWidth();
            int pageHeight = Math.max(MIN_PAGE_HEIGHT, Math.round(content.getHeight() * scale));

            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(PAGE_WIDTH, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);
            canvas.scale(scale, scale);
            content.draw(canvas);
            document.finishPage(page);

            try (OutputStream out = openDownloadsStream(context, fileName)) {
                document.writeTo(out);
            }
        } finally {
            document.close();
        }
        return fileName;
    }

    private static OutputStream openDownloadsStream(Context context, String fileName) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = context.getContentResolver()
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Could not create the file in Downloads");
            OutputStream out = context.getContentResolver().openOutputStream(uri);
            if (out == null) throw new IOException("Could not open the file in Downloads");
            return out;
        }

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not access the Downloads folder");
        }
        return new FileOutputStream(new File(dir, fileName));
    }
}
