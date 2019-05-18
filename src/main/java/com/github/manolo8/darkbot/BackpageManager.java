package com.github.manolo8.darkbot;

import com.github.manolo8.darkbot.utils.Time;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class BackpageManager extends Thread {
    private final Main main;
    private static final int SECOND = 1000, MINUTE = 60 * SECOND;

    private static final String[] ACTIONS = new String[] {
            "internalStart", "internalDock", "internalAuction", "internalGalaxyGates", "internalPilotSheet"
    };
    private static String getRandomAction() {
        return ACTIONS[(int) (Math.random() * ACTIONS.length)];
    }

    private String sid;
    private String instance;

    private long waitTime = 0;
    private long lastUpdate = System.currentTimeMillis();
    private int sidStatus = -1;

    public BackpageManager(Main main) {
        super("BackpageManager");
        this.main = main;
        start();
    }

    @Override
    public void run() {
        while (true) {
            lastUpdate = System.currentTimeMillis();
            int waitTime;

            if (isInvalid()) {
                sidStatus = -1;
                waitTime = SECOND;
            } else {
                waitTime = validTick();
            }

            if (sidStatus == 302) break;

            this.waitTime = (int) (waitTime + waitTime * Math.random());
            sleep(waitTime);
        }
    }

    private boolean isInvalid() {
        this.sid = main.statsManager.sid;
        this.instance = main.statsManager.instance;
        return sid == null || instance == null || sid.isEmpty() || instance.isEmpty();
    }

    private int validTick() {
        try {
            sidStatus = sidKeepAlive();
        } catch (Exception e) {
            sidStatus = -2;
            e.printStackTrace();
            return 5 * MINUTE;
        }
        return 10 * MINUTE;
    }

    private int sidKeepAlive() throws Exception {
        return getConnection("indexInternal.es?action=" + getRandomAction()).getResponseCode();
    }

    public HttpURLConnection getConnection(String params) throws Exception{
        if (!isInvalid()) {
            HttpURLConnection conn = (HttpURLConnection) new URL(this.instance + params)
                    .openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Cookie", "dosid=" + this.sid);
            return conn;
        }
        return null;
    }

    public String getDataInventory(String params){
        String data = null;
        try {
            HttpURLConnection conn = getConnection(params);
            conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseb = new StringBuilder();
            String currentLine;

            while ((currentLine = in.readLine()) != null){
                responseb.append(currentLine);
            }

            in.close();
            byte[] base64Decode = Base64.getDecoder().decode(responseb.toString());
            data = new String(base64Decode, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    private boolean sleep(int millis) {
        if (millis <= 0) return true;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
        return true;
    }

    public synchronized String sidStatus() {
        return sidStat() + (sidStatus != -1 && sidStatus != 302 ?
                " " + Time.toString(System.currentTimeMillis() - lastUpdate) + "/" + Time.toString(waitTime) : "");
    }

    private String sidStat() {
        switch (sidStatus) {
            case -1: return "--";
            case -2: return "ERR";
            case 200: return "OK";
            case 302: return "KO";
            default: return sidStatus + "";
        }
    }

}
