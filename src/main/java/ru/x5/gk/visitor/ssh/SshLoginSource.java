package ru.x5.gk.visitor.ssh;

public class SshLoginSource {

    private final String login = "line3";

    public String get() {
        String local = System.getProperty("local");
        if (local != null && !local.isBlank()) {
            return "laba";
        }
        return login;
    }
}
