

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.FileReader;
import java.io.IOException;

/**
 * Hello world!
 */
public class ActiveMQSend {
    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            String fileName = args[0];
            String content = readEntireFile(fileName);
            thread(new HelloWorldProducer(args[0], "Hello World", 10, "1"), false);
        } else if (args.length == 2) {
            String fileName = args[1];
            String content = readEntireFile(fileName);
            thread(new HelloWorldProducer(args[0], content, 10, "1"), false);
        } else if (args.length == 3) {
            String fileName = args[1];
            String content = readEntireFile(fileName);
            int time = Integer.parseInt(args[2]);
            thread(new HelloWorldProducer(args[0], content, time, "1"), false);
        } else if (args.length == 4) {
            String fileName = args[1];
            String content = readEntireFile(fileName);
            int time = Integer.parseInt(args[2]);
            for (int i = 0; i < Integer.parseInt(args[3]); i++) {
                thread(new HelloWorldProducer(args[0], content, time, "" + i), false);
            }
        }
    }

    public static void thread(Runnable runnable, boolean daemon) {
        Thread brokerThread = new Thread(runnable);
        brokerThread.setDaemon(daemon);
        brokerThread.start();
    }

    public static class HelloWorldProducer implements Runnable {
        String url = "tcp://localhost:61616";

        String content;

        long time = 100;

        String id = "1";

        public HelloWorldProducer(String url, String content, long time, String id) {
            this.content = content;
            this.time = time;
            this.id = id;
            this.url = url;
        }

        public void run() {
            try {
                // Create a ConnectionFactory
                ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
                connectionFactory.setAlwaysSessionAsync(false);
                connectionFactory.setOptimizeAcknowledge(true);

                // Create a Connection
                Connection connection = connectionFactory.createConnection();
                connection.start();

                // Create a Session
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // Create the destination (Topic or Queue)
                Destination destination = session.createQueue("send" + id);

                // Create a MessageProducer from the Session to the Topic or Queue
                MessageProducer producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                boolean run = true;
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
                    message.setLongProperty("time", System.currentTimeMillis());

                    // Tell the producer to send the message
                    System.out.println("Sent message: " + message.hashCode() + " : " + Thread.currentThread().getName());
                    producer.send(message);
                    Thread.sleep(time);
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

    public static class HelloWorldConsumer implements Runnable, ExceptionListener {
        public void run() {
            try {

                // Create a ConnectionFactory
                ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");

                // Create a Connection
                Connection connection = connectionFactory.createConnection();
                connection.start();

                connection.setExceptionListener(this);

                // Create a Session
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // Create the destination (Topic or Queue)
                Destination destination = session.createQueue("TEST.FOO");

                // Create a MessageConsumer from the Session to the Topic or Queue
                MessageConsumer consumer = session.createConsumer(destination);

                // Wait for a message
                Message message = consumer.receive(1000);

                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    String text = textMessage.getText();
                    System.out.println("Received: " + text);
                } else {
                    System.out.println("Received: " + message);
                }

                consumer.close();
                session.close();
                connection.close();
            } catch (Exception e) {
                System.out.println("Caught: " + e);
                e.printStackTrace();
            }
        }

        public synchronized void onException(JMSException ex) {
            System.out.println("JMS Exception occured.  Shutting down client.");
        }
    }

    private static String readEntireFile(String filename) throws IOException {
        FileReader in = new FileReader(filename);
        StringBuilder contents = new StringBuilder();
        char[] buffer = new char[4096];
        int read = 0;
        do {
            contents.append(buffer, 0, read);
            read = in.read(buffer);
        } while (read >= 0);
        return contents.toString();
    }
}
