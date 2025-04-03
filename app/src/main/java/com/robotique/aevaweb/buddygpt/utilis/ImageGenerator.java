package com.robotique.aevaweb.buddygpt.utilis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.util.Log;

import com.robotique.aevaweb.buddygpt.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageGenerator extends AsyncTask<String, Void, Bitmap> {

    private WeakReference<Context> contextRef;
    private String filename;

    public ImageGenerator(Context context, String filename) {
        contextRef = new WeakReference<>(context);
        this.filename = filename;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        String imageUrl = params[0];
        Bitmap bitmap = null;

        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        Log.e("IMG", "Downloaded");
        if (bitmap != null) {
            saveImageToInternalStorage(bitmap, filename);
        }
    }

    private void saveImageToInternalStorage(Bitmap bitmap, String fileName) {
        Context context = contextRef.get();

        if (context != null) {
            File directory = new File( context.getString(R.string.path), "BuddyGPT");

            // Vérifiez si le répertoire existe, sinon, créez-le
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.e("saveImageToCustomDirectory", "Erreur: Impossible de créer le répertoire");
                    return;
                }
            }

            File file = new File(directory, fileName);

            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
                // Mettre à jour la galerie pour que l'image soit visible
                MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}



