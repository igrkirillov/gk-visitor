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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.IOUtils;
import org.springframework.util.CollectionUtils;
import ru.x5.gk.visitor.ExcelExporter;
import ru.x5.gk.visitor.GkHostDeterminer;
import ru.x5.gk.visitor.GkPasswordDeterminer;
import ru.x5.gk.visitor.ResultData;
import ru.x5.gk.visitor.ResultData.ResultDataRow;
import ru.x5.gk.visitor.ResultLogger;
import ru.x5.gk.visitor.ShopsSource;

import static org.springframework.util.CollectionUtils.isEmpty;

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

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        List<Callable<Object>> tasks = new ArrayList<>(shopsSource.get().size());
        for (String shop : shopsSource.get()) {
            tasks.add(Executors.callable(() -> runTask(shop, ssh, hostDeterminer, loginSource.get(), passwordDeterminer, resultData)));
        }
        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        }

        executorService.shutdown();

        ExcelExporter excelExporter = new ExcelExporter(resultData);
        String resultFilePath = "./result_" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .replaceAll("[\\.:-]", "_") + ".xlsx";
        excelExporter.exportTo(new File(resultFilePath));
    }

    private static void runTask(String shop, String ssh, GkHostDeterminer hostDeterminer, String login,
                                GkPasswordDeterminer passwordDeterminer, ResultData resultData) {
        List<String> sshOutput = runCommand(ssh, hostDeterminer.determineHost(shop), login,
                passwordDeterminer.determinePassword(shop));
        if (!isEmpty(sshOutput)) {
            for (String outputRow : sshOutput) {
                ResultDataRow dataRow = resultData.newRow();
                dataRow.addColValue(HEADER_SHOP, shop);
                dataRow.addColValue(HEADER_LOG, outputRow);
                logger.log(dataRow.toDebugString());
            }
        } else {
            ResultDataRow dataRow = resultData.newRow();
            dataRow.addColValue(HEADER_SHOP, shop);
            dataRow.addColValue(HEADER_LOG, "");
            logger.log(dataRow.toDebugString());
        }
    }

    private static String getSsh() {
        try {
            return new String(Files.readAllBytes(Paths.get("./ssh.txt")));
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return "";
        }
    }

    private static List<String> runCommand(String command, String host, String login, String password) {
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

            return IOUtils.readLines(output, StandardCharsets.UTF_8);
        } catch (JSchException | IOException e) {
            closeConnection(channel, session);
            e.printStackTrace(System.err);
            return List.of("");
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