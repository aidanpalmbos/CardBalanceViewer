package com.example.cardbalanceviewer;

import java.io.IOException;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.FormBody;

public class RESTHelper {
    static String host = "http://127.0.0.168:3309/";
    static String apiKey = "";

    /**Check if client can access Internet. Returns True if connection can be made.*/
    public static boolean checkConnection() {
        String command = "ping -c 1 google.com";
        try {
            return Runtime.getRuntime().exec(command).waitFor() == 0;
        } catch (InterruptedException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }
    /**Check if valid data has been received from the server. Returns True if the response is valid.*/
    public static boolean checkResponse(String response) {
        return !response.equals("false");
    }

    /**Function to make request for POST*/
    public static Request createRequest(String url, String[] data) {
        FormBody.Builder builder = new FormBody.Builder();
        for (int i = 0; i < data.length; i += 2) {
            builder.add(data[i], data[i + 1]);
        }

        RequestBody body = builder.build();
        return new Request.Builder().url(host + url).post(body).build();
    }
}
