package ru.x5.gk.visitor;

public class GkHostDeterminer {
    public String determineHost(String sapNum) {
        String local = System.getProperty("local");
        if (local != null && !local.isBlank()) {
            return "localhost";
        } else {
            return "BO-" + sapNum;
        }
    }
}
