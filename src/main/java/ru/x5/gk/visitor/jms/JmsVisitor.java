package ru.x5.gk.visitor.jms;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import lombok.RequiredArgsConstructor;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.DestinationViewMBean;
import ru.x5.gk.visitor.ExcelExporter;
import ru.x5.gk.visitor.GkHostDeterminer;
import ru.x5.gk.visitor.ResultData;
import ru.x5.gk.visitor.ResultData.ResultDataRow;
import ru.x5.gk.visitor.ResultLogger;
import ru.x5.gk.visitor.ShopsSource;

public class JmsVisitor {

    private static final String HEADER_SHOP = "Shop";
    private static final String HEADER_QUEUE = "Queue";
    private static final String HEADER_QUEUE_SIZE = "QueueSize";
    private static final String HEADER_DEQUEUE_COUNT = "DequeueCount";
    private static final String HEADER_IN_FLIGHT_COUNT = "InFlightCount";
    private static final String HEADER_CONSUMER_COUNT = "ConsumerCount";
    private static final String[] HEADERS =
            {HEADER_SHOP, HEADER_QUEUE, HEADER_QUEUE_SIZE, HEADER_DEQUEUE_COUNT, HEADER_IN_FLIGHT_COUNT, HEADER_CONSUMER_COUNT};

    private static final String QUEUE_LOCAL_INVENTORY_TASK = "mrm.local.inventory.task.queue";
    private static final String QUEUE_INCOMING_SERIALIZING = "mrm.booked.incoming.serializing.queue";
    private static final String QUEUE_INCOMING_SENDING = "mrm.booked.incoming.sending.queue";
    private static final String[] QUEUES =
            new String[] {QUEUE_LOCAL_INVENTORY_TASK, QUEUE_INCOMING_SERIALIZING, QUEUE_INCOMING_SENDING};

    private static final ResultLogger logger = new ResultLogger();

    public static void main(String[] args) {
        GkHostDeterminer hostDeterminer = new GkHostDeterminer();
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(hostDeterminer);
        ShopsSource shopsSource = new ShopsSource();
        ResultData resultData = new ResultData(HEADERS);

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        List<Callable<Object>> tasks = new ArrayList<>(shopsSource.get().size());
        for (String shop : shopsSource.get()) {
            tasks.add(Executors.callable(() -> runTask(shop, jmsConnectionFactory, resultData)));
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

    private static void runTask(String shop, JmsConnectionFactory jmsConnectionFactory, ResultData resultData) {
        MBeanServerConnection connection = jmsConnectionFactory.getConnection(shop);
        if (connection == null) {
            ResultDataRow dataRow = resultData.newRow();
            dataRow.addColValue(HEADER_SHOP, shop);
            logger.log(dataRow.toDebugString());
            return;
        }
        for (String queue : QUEUES) {
            QueueHealthInformer queueHealthInformer = new QueueHealthInformer(shop, connection, queue);
            queueHealthInformer.addInfoToResultData(resultData);
        }
    }

    @RequiredArgsConstructor
    private static class QueueHealthInformer {
        private final String shop;
        private final MBeanServerConnection connection;
        private final String queueName;

        public void addInfoToResultData(ResultData resultData) {
            try {
                ResultDataRow dataRow = resultData.newRow();
                dataRow.addColValue(HEADER_SHOP, shop);
                dataRow.addColValue(HEADER_QUEUE, queueName);
                if (isQueueExist()) {
                    ObjectName queueMBeanName = new ObjectName(
                            "org.apache.activemq:type=Broker,"
                                    + "brokerName=brokerActiveMQ,destinationType=Queue,"
                                    + "destinationName=" + queueName);
                    DestinationViewMBean viewMBean = MBeanServerInvocationHandler.newProxyInstance(connection,
                            queueMBeanName, DestinationViewMBean.class, true);
                    dataRow.addColValue(HEADER_QUEUE_SIZE, viewMBean.getQueueSize());
                    dataRow.addColValue(HEADER_DEQUEUE_COUNT, viewMBean.getDequeueCount());
                    dataRow.addColValue(HEADER_IN_FLIGHT_COUNT, viewMBean.getInFlightCount());
                    dataRow.addColValue(HEADER_CONSUMER_COUNT, viewMBean.getConsumerCount());
                }
                logger.log(dataRow.toDebugString());
            } catch (MalformedObjectNameException e) {
                e.printStackTrace(System.err);
            }
        }

        private boolean isQueueExist() throws MalformedObjectNameException {
            ObjectName brokerMBeanName = new ObjectName("org.apache.activemq:type=Broker,brokerName=brokerActiveMQ");
            BrokerViewMBean brokerMBean = MBeanServerInvocationHandler.newProxyInstance(connection, brokerMBeanName, BrokerViewMBean.class, true);
            return Arrays.stream(brokerMBean.getQueues()).anyMatch(objectName -> queueName.equals(objectName.getKeyPropertyList().get("destinationName")));
        }
    }
}
