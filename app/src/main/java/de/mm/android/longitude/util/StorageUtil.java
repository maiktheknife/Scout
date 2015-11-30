package de.mm.android.longitude.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Max on 04.09.2015.
 */
public class StorageUtil {
    private static final String TAG = StorageUtil.class.getSimpleName();

    public static boolean storeBitmap(Context context, String email, Bitmap image) {
        Log.d(TAG, "storeBitmap: " + email);
        try {
            FileOutputStream fos = context.openFileOutput(fileNameFromMail(email), Context.MODE_PRIVATE);
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return true;

        } catch (IOException e) {
            Log.e(TAG, "storeBitmap", e);
            return false;
        }
    }

    public static Bitmap loadBitmap(Context context, String email) {
        Log.d(TAG, "loadBitmap: " + email);
        try {
            FileInputStream fis = context.openFileInput(fileNameFromMail(email));
            Bitmap pic = BitmapFactory.decodeStream(fis);
            fis.close();
            return pic;

        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            Log.w(TAG, "loadBitmap", e);
        }
        return null;
    }

    private static String fileNameFromMail(String email){
        return "contact_" + email;
    }

    private StorageUtil (){}

}