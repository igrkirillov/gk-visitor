package ru.x5.gk.visitor.photo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import lombok.RequiredArgsConstructor;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.DestinationViewMBean;
import ru.x5.gk.visitor.ArticlesSource;
import ru.x5.gk.visitor.ExcelExporter;
import ru.x5.gk.visitor.ResultData;
import ru.x5.gk.visitor.ResultData.ResultDataRow;
import ru.x5.gk.visitor.ResultLogger;

import static java.util.function.Predicate.not;

public class PhotoVisitor {

    private static final String HEADER_ARTICLE = "Article";
    private static final String HEADER_SIZE = "Size";
    private static final String[] HEADERS = {HEADER_ARTICLE, HEADER_SIZE};

    private static final ResultLogger logger = new ResultLogger();

    public static void main(String[] args) {
        ArticlesSource articlesSource = new ArticlesSource();
        ResultData resultData = new ResultData(HEADERS);

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        List<Callable<Object>> tasks = new ArrayList<>(articlesSource.get().size());
        for (String article : articlesSource.get()) {
            tasks.add(Executors.callable(() -> runTask(article, resultData)));
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

    private static void runTask(String article, ResultData resultData) {
        PhotoRequester photoRequester = new PhotoRequester(article);
        photoRequester.execRequestAndAddInfo(resultData);
    }

    private static List<String> getQueues() {
        try {
            String text = Files.readString(Paths.get("./queues.txt"), StandardCharsets.UTF_8);
            return !text.isBlank() ? Arrays.stream(text.split("[,\n\\s]")).filter(not(String::isBlank)).collect(
                    Collectors.toList()) : Collections.emptyList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @RequiredArgsConstructor
    private static class PhotoRequester {
        private final String article;

        public void execRequestAndAddInfo(ResultData resultData) {
            ResultDataRow dataRow = resultData.newRow();
            dataRow.addColValue(HEADER_ARTICLE, article);
            try {
                dataRow.addColValue(HEADER_SIZE, getFileSize(downloadPhoto()));
            } catch (IOException e) {
                logger.log("Article " + article + " error " + e.getMessage());
                dataRow.addColValue(HEADER_SIZE, "error");
            }
            logger.log(dataRow.toDebugString());
        }

        private BufferedImage downloadPhoto() throws IOException {
            URL url = new URL(String.format("http://media.x5.ru/rest/x5/cdnplu?plu=%s&format=7", article));
            return ImageIO.read(url);
        }

        private int getFileSize(BufferedImage image) throws IOException {
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", tmp);
            tmp.close();
            return tmp.size();
        }
    }
}
