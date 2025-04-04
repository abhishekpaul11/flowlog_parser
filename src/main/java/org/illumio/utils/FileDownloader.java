package org.illumio.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;

public class FileDownloader {

    public static boolean download(String fileURL, String savePath) {
        try {
            URL url = URI.create(fileURL).toURL();
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");

            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedInputStream inputStream = new BufferedInputStream(httpConn.getInputStream());
                     BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(savePath))) {

                    byte[] buffer = new byte[4096]; // 4KB buffer
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    System.out.println("File downloaded successfully.");
                }
            } else {
                System.err.println("Failed to download the file. Server responded with: " + responseCode);
            }
            httpConn.disconnect();
            return true;
        } catch (IOException e) {
            System.err.println("Error during file download: " + e.getMessage());
            return false;
        }
    }
}
