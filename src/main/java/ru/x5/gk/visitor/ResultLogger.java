package ru.x5.gk.visitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ResultLogger {

    public synchronized void log(String row) {
        try {
            System.out.println(row);
            Files.write(Paths.get("./result_logger.txt"), List.of(row), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}