package ru.x5.gk.visitor.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StringUtils;
import ru.x5.gk.visitor.GkHostDeterminer;

public class GkPasswordDeterminer {

    private final GkHostDeterminer hostDeterminer;
    private final SshLoginSource loginSource;
    private final SshPasswordProviderUrlSource passwordProviderUrlSource;

    public GkPasswordDeterminer(GkHostDeterminer hostDeterminer, SshLoginSource loginSource, SshPasswordProviderUrlSource passwordProviderUrlSource) {
        this.hostDeterminer = hostDeterminer;
        this.loginSource = loginSource;
        this.passwordProviderUrlSource = passwordProviderUrlSource;
    }

    public String determinePassword(String sapNum) {
        try {
            String password = findPassword(sapNum);
            if (StringUtils.isEmpty(password)) {
                throw new IllegalStateException("Пароль не найден!");
            } else {
                System.out.println("password: " + password);
                return password;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Ошибка при поиске пароля", e);
        }
    }

    private String findPassword(String sapNum) throws IOException {
        URL url = new URL(passwordProviderUrlSource.get() + sapNum);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(3000);
        try (InputStream is = conn.getInputStream()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            return bufferedReader.readLine();
        }
    }
}
