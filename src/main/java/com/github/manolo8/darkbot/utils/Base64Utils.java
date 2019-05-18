package com.github.manolo8.darkbot.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;

public class Base64Utils{

    public static String base64Decode(InputStream input) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(input));
        StringBuilder responseb = new StringBuilder();
        String currentLine;

        while ((currentLine = in.readLine()) != null){
            responseb.append(currentLine);
        }

        in.close();

        return new String(Base64.getDecoder().decode(responseb.toString()),"UTF-8");
    }
    public static String base64Encode(String text) throws Exception {
        return Base64.getEncoder().encodeToString(text.getBytes("UTF-8"));
    }
}
