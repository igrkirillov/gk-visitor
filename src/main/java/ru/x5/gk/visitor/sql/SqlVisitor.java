package ru.x5.gk.visitor.sql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ru.x5.gk.visitor.ExcelExporter;
import ru.x5.gk.visitor.GkHostDeterminer;
import ru.x5.gk.visitor.ResultData;
import ru.x5.gk.visitor.ResultData.ResultDataRow;
import ru.x5.gk.visitor.ResultLogger;
import ru.x5.gk.visitor.ShopsSource;

public class SqlVisitor {

    private static final ResultLogger logger = new ResultLogger();

    private static final String HEADER_SHOP = "shop";

    public static void main(String[] args) {
        ShopsSource shopsSource = new ShopsSource();
        GkHostDeterminer hostDeterminer = new GkHostDeterminer();
        SqlSource sqlSource = new SqlSource();
        ResultData resultData = new ResultData(defineHeaders(sqlSource, shopsSource, hostDeterminer));

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        List<Callable<Object>> tasks = new ArrayList<>(shopsSource.get().size());
        for (String shop : shopsSource.get()) {
            tasks.add(Executors.callable(() -> runTask(shop, sqlSource.getSql(), hostDeterminer, resultData)));
        }
        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        }

        executorService.shutdown();

        logger.log("Выгрузка в Excel...");
        ExcelExporter excelExporter = new ExcelExporter(resultData);
        String resultFilePath = "./result_" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .replaceAll("[\\.:-]", "_") + ".xlsx";
        excelExporter.exportTo(new File(resultFilePath));
    }

    private static String[] defineHeaders(SqlSource sqlSource, ShopsSource shopsSource, GkHostDeterminer hostDeterminer) {
        String sql = sqlSource.getSql().toLowerCase();
        String shop = shopsSource.get().get(0);
        List<String> result = new ArrayList<>();
        result.add(HEADER_SHOP);
        try (Connection con = DriverManager.getConnection(
                "jdbc:postgresql://" + hostDeterminer.determineHost(shop) +
                        ":5432/postgres?currentSchema=gkretail", "gkretail", "gkretail");
                Statement statement = con.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            logger.log("Запрос для получения столбцов выполнен " + shop + " columns " + rs.getMetaData().getColumnCount());
            ResultSetMetaData md = rs.getMetaData();
            // индекс в rs col начинается от 1
            for (int col = 1; col <= md.getColumnCount(); ++col) {
                result.add(md.getColumnName(col));
            }
            logger.log(String.join("", result));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result.toArray(new String[0]);
    }

    private static void runTask(String shop, String sql, GkHostDeterminer hostDeterminer, ResultData resultData) {
        String[] headers = resultData.getHeaders();
        try (Connection con = DriverManager.getConnection(
                "jdbc:postgresql://" + hostDeterminer.determineHost(shop) +
                        ":5432/postgres?currentSchema=gkretail", "gkretail", "gkretail");
                Statement statement = con.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            logger.log("Запрос выполнен " + shop + " fetch size " + rs.getFetchSize());
            while (rs.next()) {
                ResultDataRow dataRow = resultData.newRow();
                ResultSetMetaData md = rs.getMetaData();
                dataRow.addColValue(HEADER_SHOP, shop);
                // индекс в rs начинается от 1
                for (int i = 1; i <= md.getColumnCount(); ++i) {
                    dataRow.addColValue(headers[i], rs.getString(i));
                }
                logger.log(dataRow.toDebugString());
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
    }
}
