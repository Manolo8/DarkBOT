package com.github.manolo8.darkbot.utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Time {

    private static final DateTimeFormatter FILENAME_FRIENDLY_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd_HH-mm-ss_SSS");
    private static final DateTimeFormatter LOG_DATE = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss.SSS");

    public static String toString(Integer time) {
        if (time == null) return "-";
        return toString(time.intValue());
    }

    public static String toString(long time) {
        StringBuilder builder = new StringBuilder();
        int seconds = (int) (time / 1000L);
        if (seconds >= 3600) {
            int hours = seconds / 3600;
            if (hours < 10) {
                builder.append('0');
            }
            builder.append(hours).append(':');
        }
        if (seconds >= 60) {
            int minutes = seconds % 3600 / 60;
            if (minutes < 10) {
                builder.append('0');
            }
            builder.append(minutes).append(':');
        }
        if ((seconds %= 60) < 10) {
            builder.append('0');
        }
        builder.append(seconds);
        return builder.toString();
    }

    public static String filenameFriendly() {
        return LocalDateTime.now().format(FILENAME_FRIENDLY_DATE);
    }

    public static void sleep(long millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {}
    }

    public static class PrintStreamWithDate extends PrintStream {
        public PrintStreamWithDate(OutputStream out) {
            super(out);
        }

        @Override
        public void println(String string) {
            super.println("[" + LocalDateTime.now().format(LOG_DATE) + "] " + string);
        }
    }

}
