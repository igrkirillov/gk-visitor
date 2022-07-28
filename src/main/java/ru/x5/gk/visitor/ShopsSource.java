package ru.x5.gk.visitor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class ShopsSource {

    private final List<String> shops;

    public ShopsSource() {
        try {
            String text = Files.readString(Paths.get("./shops.txt"), StandardCharsets.UTF_8);
            shops = !text.isBlank() ? Arrays.stream(text.split("[,\n\\s]")).filter(not(String::isBlank)).collect(Collectors.toList()) : Collections.emptyList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<String> get() {
        return shops;
    }
}
