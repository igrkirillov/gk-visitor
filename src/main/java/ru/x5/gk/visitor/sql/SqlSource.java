package ru.x5.gk.visitor.sql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SqlSource {

    private final String sql;

    public SqlSource() {
        try {
            sql = Files.readString(Paths.get("./sql.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getSql() {
        return sql;
    }
}
