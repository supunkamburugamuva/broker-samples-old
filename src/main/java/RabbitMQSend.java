import com.rabbitmq.client.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RabbitMQSend extends AbstractSender {
    public static void main(String[] args) throws IOException {
        RabbitMQSend rabbitMQSend = new RabbitMQSend();
        rabbitMQSend.setUp(args);

        rabbitMQSend.start();
    }

    public void start() {

    }

    private class Worker implements Runnable {
        private String exchangeName = "test";

        private String routingKey = "test";

        private String queueName = "send";

        private Channel channel;

        private Connection conn;

        boolean run = true;

        public Worker(String queueName) {
            ConnectionFactory factory = new ConnectionFactory();
            try {
                factory.setUri(url);
                conn = factory.newConnection();

                channel = conn.createChannel();
                channel.exchangeDeclare(exchangeName, "direct", false);
                channel.queueDeclare(this.queueName, false, false, false, null).getQueue();
                channel.queueBind(queueName, exchangeName, routingKey);
            } catch (IOException e) {
                String msg = "Error creating the RabbitMQ channel";
                throw new RuntimeException(msg, e);
            } catch (Exception e) {
                String msg = "Error creating the RabbitMQ channel";
                throw new RuntimeException(msg, e);
            }
        }

        @Override
        public void run() {
            
            int errorCount = 0;
            while (run) {
                try {
                    Map<String, Object> headers = new HashMap<String, Object>();
                    headers.put("time", System.currentTimeMillis());
                    channel.basicPublish(exchangeName, routingKey,
                            new AMQP.BasicProperties.Builder().headers(headers).build(),
                            Long.toString(System.currentTimeMillis()).getBytes());

                    Thread.sleep(interval);
                } catch (Throwable t) {
                    errorCount++;
                    if (errorCount <= 3) {
                        System.out.println("Error occurred " + errorCount + " times.. trying to continue the worker");
                    } else {
                        System.out.println("Error occurred " + errorCount + " times.. terminating the worker");
                        run = false;
                    }
                }
            }
            String message = "Unexpected notification type";
            System.out.println(message);
            throw new RuntimeException(message);
        }

        public void stop() {
            try {
                run = false;
                channel.close();
                conn.close();
            } catch (IOException e) {
                System.out.println("Error closing the rabbit MQ connection" + e);
            }
        }
    }
}
