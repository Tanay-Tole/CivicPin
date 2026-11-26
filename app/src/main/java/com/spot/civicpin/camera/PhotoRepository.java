package com.spot.civicpin.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoRepository {

    private static final String TAG = "PhotoRepository";
    private final Context context;

    public PhotoRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public File createPhotoFile() {
        File storageDir = new File(context.getFilesDir(), "photos");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return new File(storageDir, "JPEG_" + timeStamp + ".jpg");
    }

    public Uri getPhotoUri(File file) {
        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );
    }

    public String processAndSaveImage(Uri uri) {
        if (uri == null) return null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, options);
            }

            options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
            options.inJustDecodeBounds = false;

            Bitmap bitmap;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is, null, options);
            }

            if (bitmap == null) return null;

            try (InputStream exifStream = context.getContentResolver().openInputStream(uri)) {
                if (exifStream != null) {
                    ExifInterface exifInterface = new ExifInterface(exifStream);
                    int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                    bitmap = rotateBitmap(bitmap, orientation);
                }
            }

            File photoFile = createPhotoFile();
            try (FileOutputStream fos = new FileOutputStream(photoFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                fos.flush();
            }

            return photoFile.getAbsolutePath();
        } catch (Throwable t) {
            Log.e(TAG, "Error processing image safely from URI", t);
            return null;
        }
    }

    /** GPS saved in the photo as {lat, lng}, or null when it has none (or Android hid it from us). */
    public double[] readPhotoLatLong(Uri uri) {
        if (uri == null) return null;
        Uri source = uri;
        // Android 10+ blanks the GPS tag of gallery photos unless we ask for the original file.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && MediaStore.AUTHORITY.equals(uri.getAuthority())
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            source = MediaStore.setRequireOriginal(uri);
        }
        try (InputStream is = context.getContentResolver().openInputStream(source)) {
            if (is == null) return null;
            double[] latLng = new ExifInterface(is).getLatLong();
            // 0,0 is what a blanked tag reads as, not a real place.
            if (latLng == null || (latLng[0] == 0.0 && latLng[1] == 0.0)) return null;
            return latLng;
        } catch (Throwable t) {
            Log.w(TAG, "Could not read photo location", t);
            return null;
        }
    }

    public String saveBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            File photoFile = createPhotoFile();
            try (FileOutputStream fos = new FileOutputStream(photoFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                fos.flush();
            }
            return photoFile.getAbsolutePath();
        } catch (Throwable t) {
            Log.e(TAG, "Error saving bitmap safely", t);
            return null;
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        if (bitmap == null) return null;
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
