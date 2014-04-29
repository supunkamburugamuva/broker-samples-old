

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 */
public class ActiveMQSend extends AbstractSender {
    private static List<HelloWorldProducer> producerList = new ArrayList<HelloWorldProducer>();

    public static void main(String[] args) throws Exception {
        ActiveMQSend send = new ActiveMQSend();
        send.setUp(args);
    }

    @Override
    public void start(String id) {
        Thread brokerThread = new Thread(new HelloWorldProducer(id));
        brokerThread.start();
    }

    public class HelloWorldProducer implements Runnable {
        boolean run = true;

        String id;

        public HelloWorldProducer(String id) {
            this.id = id;
        }

        public void stop() {
            run = false;
        }

        public void run() {
            try {
                // Create a ConnectionFactory
                ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ActiveMQSend.this.url);
                connectionFactory.setAlwaysSessionAsync(false);
                connectionFactory.setOptimizeAcknowledge(true);
                // Create a Connection
                Connection connection = connectionFactory.createConnection();
                connection.start();
                // Create a Session
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                // Create the destination (Topic or Queue)
                Destination destination = session.createQueue(id);
                // Create a MessageProducer from the Session to the Topic or Queue
                MessageProducer producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                int count = 0;
                long start = 0;
                while (run) {
                    if (System.currentTimeMillis() - start > 10000) {
                        System.out.println();
                        System.out.println("Through put **********:" + (((double)count) * 1000) /(System.currentTimeMillis() - start) + " count: " + count + " time:" + (System.currentTimeMillis() - start));
                        start = System.currentTimeMillis();
                        count = 0;
                    }
                    count++;
                // Create a messages
                    TextMessage message = session.createTextMessage(content);
                    if (!reset) {
                        message.setLongProperty("time", System.currentTimeMillis());
                    } else {
                        message.setLongProperty("time", System.currentTimeMillis() * 2);
                    }

                    // Tell the producer to send the message
                    System.out.println("Sent message: " + message.hashCode() + " : " + Thread.currentThread().getName());
                    producer.send(message);
                    Thread.sleep(ActiveMQSend.this.interval);
                }
                // Clean up
                session.close();
                connection.close();
            } catch (Exception e) {
                System.out.println("Caught: " + e);
                e.printStackTrace();
            }
        }
    }
}
