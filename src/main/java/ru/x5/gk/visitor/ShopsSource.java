package ru.x5.gk.visitor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ShopsSource {
    public List<String> get() {
        try {
            String text = Files.readString(Paths.get("./shops.txt"), StandardCharsets.UTF_8);
            return !text.isBlank() ? Arrays.stream(text.split("[,\n]")).map(String::trim).collect(Collectors.toList()) : Collections.emptyList();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return Collections.emptyList();
        }
    }
}
