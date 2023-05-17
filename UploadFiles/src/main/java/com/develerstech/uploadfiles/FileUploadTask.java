package com.develerstech.uploadfiles;


import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class FileUploadTask extends AsyncTask<Void, Integer, Void> {

    private Context mContext;
    private File mDirectory;
    private String mServerUrl;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private int mNotificationId;
    private int mFileCount = 0;
    private int mUploadedCount = 0;
    ProgressDialog progressDialog;

    public FileUploadTask(Context context, File directory, String serverUrl, int notificationId) {
        mContext = context;
        mDirectory = directory;
        mServerUrl = serverUrl;
        mNotificationId = notificationId;
//        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
//        mNotificationBuilder = new NotificationCompat.Builder(mContext, "chanel_id")
//                .setContentTitle("Uploading Files")
//                .setSmallIcon(android.R.drawable.ic_menu_upload)
//                .setOngoing(true)
//                .setProgress(0, 0, true);
        progressDialog=new ProgressDialog(context);
        progressDialog.setTitle("uploading Images 0/"+String.valueOf(directory.listFiles().length));
        progressDialog.setMessage("Uploading Images");
        progressDialog.setCancelable(false);
        progressDialog.show();

    }

    @Override
    protected Void doInBackground(Void... voids) {
        File[] files = mDirectory.listFiles();
        if (files != null) {
            mFileCount = files.length;
            for (int i = 0; i < mFileCount; i++) {
                if (isCancelled()) {
                    break;
                }
                File file = files[i];
                if (file.isFile()) {
                    try {
                        FileInputStream fileInputStream = new FileInputStream(file);
                        String fileName = file.getName();
                        String boundary = "*****";
                        String lineEnd = "\r\n";
                        String twoHyphens = "--";

                        HttpURLConnection urlConnection = (HttpURLConnection) new URL(mServerUrl).openConnection();
                        urlConnection.setDoInput(true);
                        urlConnection.setDoOutput(true);
                        urlConnection.setUseCaches(false);
                        urlConnection.setRequestMethod("POST");
                        urlConnection.setRequestProperty("Connection", "Keep-Alive");
                        urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                        DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());
                        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                        outputStream.writeBytes("Content-Disposition: form-data; name=\"sqlite_database\"; filename=\"" + fileName + "\"" + lineEnd);
                        outputStream.writeBytes(lineEnd);

                        int bufferSize = 1024;
                        byte[] buffer = new byte[bufferSize];
                        int bytesRead;
                        int totalBytesRead = 0;
                        int fileSize = (int) file.length();
                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            int progress = (int) ((totalBytesRead / (float) fileSize) * 100);
                            publishProgress(progress);
                        }
                        outputStream.writeBytes(lineEnd);
                        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        fileInputStream.close();
                        outputStream.flush();
                        outputStream.close();

                        InputStream inputStream = urlConnection.getInputStream();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        String responseLine;
                        StringBuilder response = new StringBuilder();
                        while ((responseLine = bufferedReader.readLine()) != null) {
                            response.append(responseLine);
                        }
                        bufferedReader.close();
                        inputStream.close();
                        urlConnection.disconnect();
                        if(mNotificationId==2) {
                            if (file.exists()) {
                                if (file.delete()) {
                                  //  System.out.println("File deleted successfully");
                                }
                            }
                        }
                        Log.d("FileUploadTask", "File " + fileName + " uploaded to server with response: " + response.toString());
                        mUploadedCount++;

                    } catch (IOException e) {
                        Log.e("FileUploadTask", "Error");
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
//        mNotificationBuilder.setProgress(100, progress, false)
//                .setContentText(String.format(Locale.getDefault(), "Uploading file %d of %d", mUploadedCount + 1, mFileCount));
//        mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
        progressDialog.setTitle(String.format(Locale.getDefault(),"uploading Images %d/"+String.valueOf(mDirectory.listFiles().length),mUploadedCount));

    }

    @Override
    protected void onPostExecute(Void aVoid) {
//        mNotificationBuilder.setProgress(0, 0, false)
//                .setContentText("File upload complete");
//        mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
        progressDialog.setMessage("File upload complete");
        progressDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage("All "+String.valueOf(mUploadedCount)+" files have been uploaded")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Handle OK button click if needed
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    protected void onCancelled() {
        mNotificationBuilder.setProgress(0, 0, false)
                .setContentText("File upload cancelled");
        mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
    }
}


