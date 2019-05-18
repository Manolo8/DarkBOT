package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.BackpageManager;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.entities.Drone;
import com.github.manolo8.darkbot.core.entities.Hangar;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;

import static com.github.manolo8.darkbot.Main.API;

public class HangarManager {

    private final Main main;
    private final BackpageManager backpageManager;
    private boolean disconnecting = false;
    private Character exitKey = 'l';
    private long lastChangeHangar = 0;
    private long disconectTime = 0;
    private ArrayList<Hangar> hangars;
    private ArrayList<Drone> drones;

    public HangarManager(Main main){
        this.main = main;
        this.backpageManager = main.backpage;
        this.hangars = new ArrayList<Hangar>();
        this.drones = new ArrayList<Drone>();
    }

    public boolean changeHangar(String hangarID) {
        if (!this.disconnecting) {
            disconnect();
        }
        if (this.lastChangeHangar <= System.currentTimeMillis() - 40000 && this.main.backpage.sidStatus().contains("OK")) {
            if (this.disconectTime <= System.currentTimeMillis() - 20000) {

                String url = "/indexInternal.es?action=internalDock&subAction=changeHangar&hangarId=" + hangarID;
                try {
                    backpageManager.getConnection(url).getResponseCode();
                    this.disconnecting = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                this.lastChangeHangar = System.currentTimeMillis();
            }
        } else {
            return false;
        }

        return true;
    }

    public void updateDrones() {
        try {
            String hangarID = getActiveHangar();

            if (hangarID != null) {
                String decodeParams = "{\"params\":{\"hi\":" + hangarID + "}}";
                String encodeParams = Base64.getEncoder().encodeToString(decodeParams.getBytes("UTF-8"));
                String url = "/flashAPI/inventory.php?action=getHangar&params="+encodeParams;
                String json = backpageManager.getDataInventory(url);
                JsonArray hangarArray =  new JsonParser().parse(json).getAsJsonObject().get("data")
                        .getAsJsonObject().get("ret").getAsJsonObject().get("hangars").getAsJsonArray();

                for (JsonElement hangar : hangarArray) {
                    if (hangar.getAsJsonObject().get("hangar_is_active").getAsBoolean()) {
                        JsonArray dronesArray = hangar.getAsJsonObject().get("general").getAsJsonObject().get("drones").getAsJsonArray();
                        for (JsonElement dron : dronesArray){
                            JsonObject dronJson = dron.getAsJsonObject();
                            String lootId = "drone_iris";
                            switch (dronJson.get("L").getAsInt()){
                                case 1:
                                    lootId = "drone_flax";
                                    break;
                                case 2:
                                    lootId = "drone_iris";
                                    break;
                                case 3:
                                    lootId = "drone_apis";
                                    break;
                                case 4:
                                    lootId = "drone_zeus";
                                    break;
                            }
                            int damage = Integer.parseInt(dronJson.get("HP").getAsString().replace("%"," ").trim());
                            this.drones.add(new Drone(lootId,dronJson.get("repair").getAsInt(),dronJson.get("I").getAsString(),
                                    dronJson.get("currency").getAsString(),dronJson.get("LV").getAsInt(),damage));
                        }
                    }
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean repairDron(Drone drone, String activeHangarId){
        try {
            String decodeParams =
                    "{\"action\":\"repairDrone\",\"lootId\":\"" + drone.getLootId() + "\",\"repairPrice\":" + drone.getRepairPrice() +
                            ",\"params\":{\"hi\":" + activeHangarId + "}," +
                            "\"itemId\":\"" + drone.getItemId() + "\",\"repairCurrency\":\"" + drone.getRepairCurrency() +
                            "\",\"quantity\":1,\"droneLevel\":" + drone.getDroneLevel() + "}";
            String encodeParams = Base64.getEncoder().encodeToString(decodeParams.getBytes("UTF-8"));
            String url = "/flashAPI/inventory.php?action=repairDrone&params="+encodeParams;
            String json = backpageManager.getDataInventory(url);
            if (json.contains("'isError':0")){
                return true;
            } else {
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public void updateHangars() {
        String params = "/flashAPI/inventory.php?action=getHangarList";
        String decodeString = backpageManager.getDataInventory(params);

        JsonArray hangarsArray =  new JsonParser().parse(decodeString).getAsJsonObject().get("data").getAsJsonObject()
                .get("ret").getAsJsonObject().get("hangars").getAsJsonArray();

        for (JsonElement hangar : hangarsArray) {
            this.hangars.add(new Hangar(hangar.getAsJsonObject().get("hangarID").getAsString(),
                    hangar.getAsJsonObject().get("hangar_is_active").getAsBoolean()));
        }
    }

    public String getActiveHangar(){
        updateHangars();
        for(Hangar hangar : hangars){
            if (hangar.isHangar_is_active()){
                return hangar.getHangarID();
            }
        }
        return null;
    }

    public void disconnect() {
        API.keyboardClick(exitKey);
        disconectTime = System.currentTimeMillis();
        disconnecting = true;
    }
}
