package ru.x5.gk.visitor.jms;

import java.io.IOException;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import lombok.RequiredArgsConstructor;
import ru.x5.gk.visitor.GkHostDeterminer;

@RequiredArgsConstructor
public class JmsConnectionFactory {

    private final GkHostDeterminer hostDeterminer;

    public MBeanServerConnection getConnection(String shop) {
        try {
            String url = "service:jmx:rmi:///jndi/rmi://" + hostDeterminer.determineHost(shop) + ":6422/jmxrmi";
            JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url));
            return connector.getMBeanServerConnection();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }
}
