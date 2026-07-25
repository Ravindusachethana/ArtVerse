package com.artverse.app.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Saves a bought digital artwork to the device's gallery at its original
 * quality (FR - digital delivery).
 *
 * The bytes are taken straight from Glide's cache of the file the artist
 * uploaded and copied verbatim - never re-encoded from the on-screen bitmap -
 * so what lands in Pictures/ArtVerse is the artist's file, not a screenshot of
 * it. Writing goes through MediaStore on Android 10+ (no permission needed);
 * older devices write to the public Pictures directory, which needs
 * WRITE_EXTERNAL_STORAGE (requested by ArtworkViewerActivity).
 */
public final class ArtworkDownloader {

    /** Gallery album the downloads are filed under. */
    public static final String ALBUM = "ArtVerse";

    /**
     * Held in a nested class so the worker thread and the main-thread handler
     * are only created the first time something is actually downloaded - which
     * also keeps the naming rules below testable off a device.
     */
    private static final class Workers {
        static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
        static final Handler MAIN = new Handler(Looper.getMainLooper());
    }

    private ArtworkDownloader() { }

    public interface Callback {
        void onSaved(String fileName);
        void onFailed(String message);
    }

    /**
     * Downloads (or reuses the cached copy of) the image at {@code url} and
     * files it in the gallery. Network and disk work happen off the main
     * thread; the callback is delivered back on it.
     */
    public static void saveToGallery(Context context, String url, String artworkTitle, Callback callback) {
        if (url == null || url.isEmpty()) {
            callback.onFailed("This artwork has no file to download");
            return;
        }

        Context appContext = context.getApplicationContext();
        String fileName = fileNameFor(artworkTitle, url);

        Workers.EXECUTOR.execute(() -> {
            try {
                // asFile() hands back the original downloaded bytes rather
                // than a decoded, resized bitmap.
                File source = Glide.with(appContext).asFile().load(url).submit().get();
                writeToGallery(appContext, source, fileName);
                Workers.MAIN.post(() -> callback.onSaved(fileName));
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Workers.MAIN.post(() -> callback.onFailed(message));
            }
        });
    }

    private static void writeToGallery(Context context, File source, String fileName) throws IOException {
        ContentResolver resolver = context.getContentResolver();

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, mimeTypeFor(fileName));
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + File.separator + ALBUM);
            // Hides the half-written file from the gallery until the copy ends.
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            Uri target = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (target == null) throw new IOException("Could not create the file in your gallery");

            try (OutputStream out = resolver.openOutputStream(target)) {
                if (out == null) throw new IOException("Could not open the file in your gallery");
                copy(source, out);
            } catch (IOException e) {
                resolver.delete(target, null, null);
                throw e;
            }

            ContentValues done = new ContentValues();
            done.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(target, done, null, null);
            return;
        }

        File albumDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM);
        if (!albumDir.exists() && !albumDir.mkdirs()) {
            throw new IOException("Could not access the Pictures folder");
        }
        File target = new File(albumDir, fileName);
        try (OutputStream out = new FileOutputStream(target)) {
            copy(source, out);
        }
        // Without this the file exists but never shows up in the gallery app.
        MediaScannerConnection.scanFile(context, new String[]{target.getAbsolutePath()},
                new String[]{mimeTypeFor(fileName)}, null);
    }

    private static void copy(File source, OutputStream out) throws IOException {
        try (InputStream in = new FileInputStream(source)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    /**
     * Builds the name the artwork is saved under: the title, made safe for a
     * file system, keeping the original file's extension.
     */
    public static String fileNameFor(String artworkTitle, String url) {
        String base = artworkTitle == null ? "" : artworkTitle.trim();
        base = base.replaceAll("[^A-Za-z0-9 _-]", "").replaceAll("\\s+", "-");
        if (base.length() > 60) base = base.substring(0, 60);
        if (base.isEmpty()) base = "Artwork";
        return "ArtVerse-" + base + extensionOf(url);
    }

    /**
     * Extension of the file a download URL points at, ".jpg" when the URL
     * carries none. Firebase Storage URLs keep the name in the path and hang
     * an "?alt=media&token=..." query off the end, which is stripped first.
     */
    public static String extensionOf(String url) {
        if (url == null) return ".jpg";
        String path = url;
        int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);

        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) return ".jpg";

        String extension = path.substring(dot + 1).toLowerCase(Locale.ROOT);
        // Anything longer is a dot in the path rather than a real extension.
        return extension.matches("[a-z0-9]{3,4}") ? "." + extension : ".jpg";
    }

    public static String mimeTypeFor(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg";
    }
}
