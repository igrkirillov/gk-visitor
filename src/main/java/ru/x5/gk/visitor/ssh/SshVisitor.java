package ru.x5.gk.visitor.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.io.IOUtils;
import ru.x5.gk.visitor.ExcelExporter;
import ru.x5.gk.visitor.GkHostDeterminer;
import ru.x5.gk.visitor.GkPasswordDeterminer;
import ru.x5.gk.visitor.ResultData;
import ru.x5.gk.visitor.ResultLogger;
import ru.x5.gk.visitor.ShopsSource;

public class SshVisitor {

    private static final String HEADER_SHOP = "Shop";
    private static final String HEADER_LOG = "Log";
    private static final String[] HEADERS = {HEADER_SHOP, HEADER_LOG};

    private static final ResultLogger logger = new ResultLogger();

    public static void main(String... args) {
        String ssh = getSsh();
        ShopsSource shopsSource = new ShopsSource();
        GkHostDeterminer hostDeterminer = new GkHostDeterminer();
        SshLoginSource loginSource = new SshLoginSource();
        SshPasswordSource passwordSource = new SshPasswordSource();
        GkPasswordDeterminer passwordDeterminer = new GkPasswordDeterminer(hostDeterminer, loginSource, passwordSource);
        ResultData resultData = new ResultData(HEADERS);
        for (String shop : shopsSource.get()) {
            try {
                String sshOutput = runCommand(ssh, hostDeterminer.determineHost(shop), loginSource.get(),
                        passwordDeterminer.determinePassword(shop));
                resultData.newRow();
                resultData.addColValue(HEADER_SHOP, shop);
                resultData.addColValue(HEADER_LOG, sshOutput);
                logger.log(resultData.getCurrentRowInStringFormat());
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        ExcelExporter excelExporter = new ExcelExporter(resultData);
        String resultFilePath = "./result_" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(".", "_") + ".xlsx";
        excelExporter.exportTo(new File(resultFilePath));
    }

    private static String getSsh() {
        try {
            return new String(Files.readAllBytes(Paths.get("./ssh.txt")));
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return "";
        }
    }

    private static String runCommand(String command, String host, String login, String password) {
        Session session = null;
        ChannelExec channel = null;
        try {
            session = setupSshSession(host, login, password);
            session.connect();

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            InputStream output = channel.getInputStream();
            channel.connect();

            return IOUtils.toString(output, StandardCharsets.UTF_8);

        } catch (JSchException | IOException e) {
            closeConnection(channel, session);
            e.printStackTrace(System.err);
            return "";
        } finally {
            try {
                closeConnection(channel, session);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    private static Session setupSshSession(String host, String login, String password) throws JSchException {
        Session session = new JSch().getSession(login, host);
        session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
        session.setConfig("StrictHostKeyChecking", "no"); // disable check for RSA key
        session.setPassword(password);
        return session;
    }

    private static void closeConnection(ChannelExec channel, Session session) {
        try {
            channel.disconnect();
        } catch (Exception ignored) {
        }
        session.disconnect();
    }
}