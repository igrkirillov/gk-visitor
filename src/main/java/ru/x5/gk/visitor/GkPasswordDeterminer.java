package ru.x5.gk.visitor;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import ru.x5.gk.visitor.ssh.SshLoginSource;
import ru.x5.gk.visitor.ssh.SshPasswordSource;

public class GkPasswordDeterminer {

    private final GkHostDeterminer hostDeterminer;
    private final SshLoginSource loginSource;
    private final SshPasswordSource passwordSource;

    public GkPasswordDeterminer(GkHostDeterminer hostDeterminer, SshLoginSource loginSource, SshPasswordSource passwordSource) {
        this.hostDeterminer = hostDeterminer;
        this.loginSource = loginSource;
        this.passwordSource = passwordSource;
    }

    public String determinePassword(String sapNum) {
        String local = System.getProperty("local");
        if (local != null && !local.isBlank()) {
            return "laba";
        }
        if (testPassword(sapNum, passwordSource.getPassword1())) {
            return passwordSource.getPassword1();
        } else if (testPassword(sapNum, passwordSource.getPassword2())) {
            return passwordSource.getPassword2();
        }
        throw new IllegalStateException("Ни один из паролей не подошёл!");
    }

    private boolean testPassword(String sapNum, String gkpassword) {
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(loginSource.get(), hostDeterminer.determineHost(sapNum));
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.setConfig("StrictHostKeyChecking", "no"); // disable check for RSA key
            session.setPassword(gkpassword);
            session.connect();
            System.out.printf("Пароль подошёл %s %s%n", sapNum, gkpassword);
            return true;
        } catch (JSchException e) {
            System.out.printf("Пароль не подошёл %s %s%n", sapNum, gkpassword);
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
