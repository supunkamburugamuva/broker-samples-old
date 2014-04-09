import com.rabbitmq.client.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RabbitMQSend {
    private Channel channel;

    private Connection conn;

    private String exchangeName = "test";

    private String routingKey = "test";

    private String queueName = "send";

    private Address[]addresses;

    private String url = "amqp://localhost:5672";

    private ExecutorService executorService;

    private byte[]input;

    private long time;

    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            String fileName = args[0];
            byte []content = readFile(fileName);
            RabbitMQSend rabbitMQSend = new RabbitMQSend(content);
            rabbitMQSend.start();
        } else if (args.length == 2) {
            String fileName = args[0];
            byte []content = readFile(fileName);
            RabbitMQSend rabbitMQSend = new RabbitMQSend(content, Integer.parseInt(args[1]));
            rabbitMQSend.start();
        } else {
            RabbitMQSend rabbitMQSend = new RabbitMQSend("hello world".getBytes());
            rabbitMQSend.start();
        }
    }

    public RabbitMQSend(byte[] input) {
        this(input, 1);
    }

    public RabbitMQSend(byte[] input, long time) {
        this.input = input;
        this.time = time;
        executorService = Executors.newScheduledThreadPool(10);
    }

    public void start() {
        ConnectionFactory factory = new ConnectionFactory();
        try {
            if (addresses == null) {
                factory.setUri(url);
                if (executorService != null) {
                    conn = factory.newConnection(executorService);
                } else {
                    conn = factory.newConnection();
                }
            } else {
                if (executorService != null) {
                    conn = factory.newConnection(executorService, addresses);
                } else {
                    conn = factory.newConnection(addresses);
                }
            }

            channel = conn.createChannel();
            channel.exchangeDeclare(exchangeName, "direct", true);
            channel.queueDeclare(this.queueName, false, false, false, null).getQueue();
            channel.queueBind(queueName, exchangeName, routingKey);

            Thread t = new Thread(new Worker());
            t.start();
        } catch (IOException e) {
            String msg = "Error creating the RabbitMQ channel";
            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            String msg = "Error creating the RabbitMQ channel";
            throw new RuntimeException(msg, e);
        }
    }

    public void stop() {
        try {
            channel.close();
            conn.close();
        } catch (IOException e) {
            System.out.println("Error closing the rabbit MQ connection" + e);
        }
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            boolean run = true;
            int errorCount = 0;
            while (run) {
                try {
                    channel.basicPublish(exchangeName, routingKey,
                            new AMQP.BasicProperties.Builder().timestamp(new Date(System.currentTimeMillis())).build(),
                            input);
                    Thread.sleep(time);
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
    }

    public static byte[] readFile(String file) throws IOException {
        return readFile(new File(file));
    }

    public static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }
}
