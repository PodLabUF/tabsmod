package com.tabslab.tabsmod.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class Data {

    private static final ArrayList<Event> evts = new ArrayList<>();
    private static String playerName;

    public static void addEvent(String type, long time, Map<String, Object> data) {
        Event evt = new Event(type, time, data);

        // Print to log
        System.out.println("-----------------------------------------");
        System.out.println("Event Type: " + evt.getType());
        System.out.println("Time: " + evt.getTime());
        System.out.println("Data: " + evt.getDataString());
        System.out.println("-----------------------------------------");

        evts.add(evt);
    }

    public static void addEvent(String type, long time) {
        Event evt = new Event(type, time);

        // Print to log
        System.out.println("-----------------------------------------");
        System.out.println("Event Type: " + evt.getType());
        System.out.println("Time: " + evt.getTime());
        System.out.println("-----------------------------------------");

        evts.add(new Event(type, time));
    }

    public static void setName(String name) {
        playerName = name;
    }

    public static void printSummary() {
        System.out.println("-----------------------------------------");
        System.out.println("Event Summary");
        System.out.println(evts);
        System.out.println("-----------------------------------------");
    }

    public static void endSession() {
        writeToCSV();
        playerName = null;
        evts.clear();
    }

    public static void writeToCSV() {

        System.out.println("-----------------------------------------");
        System.out.println("Creating csv file...");
        System.out.println("-----------------------------------------");

        File file = new File(playerName + ".csv");

        try(PrintWriter pw = new PrintWriter(file)) {

            // Add headers
            pw.println("Player Name: " + playerName);

            // Write events
            String[] cols = { "Time", "Type", "Other Data" };
            pw.println(String.join(",", cols));

            for (Event evt : evts) {
                pw.println(evt.toCSV());
            }

            // Close connection
            pw.close();

            System.out.println("-----------------------------------------");
            System.out.println("Data File Created!");
            System.out.println("Absolute Path: " + file.getAbsolutePath());
            System.out.println("-----------------------------------------");

        } catch (IOException e) {

            System.out.println("-----------------------------------------");
            System.out.println("Exception during file creation:");
            System.out.println(e.getMessage());
            System.out.println("-----------------------------------------");
        }

    }
}
