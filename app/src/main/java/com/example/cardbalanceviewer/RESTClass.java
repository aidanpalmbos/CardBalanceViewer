package com.example.cardbalanceviewer;

import java.io.IOException;

public class RESTClass {
    static long threadSleep = 2500;
    static String host = "http://192.168.50.168:3309/";
    static String apiKey = "/palmbos0103palmbos0103/";
    public static String SetupString(String function) {
        return host + function + apiKey;
    }

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
}
