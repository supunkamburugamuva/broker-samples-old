import com.rabbitmq.client.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RabbitMQConsumer {
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
            RabbitMQConsumer rabbitMQSend = new RabbitMQConsumer(content);
            rabbitMQSend.start();
        } else if (args.length == 2) {
            String fileName = args[0];
            byte []content = readFile(fileName);
            RabbitMQConsumer rabbitMQSend = new RabbitMQConsumer(content, Integer.parseInt(args[1]));
            rabbitMQSend.start();
        } else {
            RabbitMQConsumer rabbitMQSend = new RabbitMQConsumer("hello world".getBytes());
            rabbitMQSend.start();
        }
    }

    public RabbitMQConsumer(byte[] input) {
        this(input, 1);
    }

    public RabbitMQConsumer(byte[] input, long time) {
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
            channel.basicQos(1024);
            channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag,
                                           Envelope envelope,
                                           AMQP.BasicProperties properties,
                                           byte[] body)
                        throws IOException
                {
                    String routingKey = envelope.getRoutingKey();
                    Map<String, Object> headers = properties.getHeaders();
                    Long timeStamp = (Long) headers.get("time");
                    String time = new String(body);

                    long currentTime = System.currentTimeMillis();

                    System.out.println("latency: " + (currentTime - timeStamp) + " initial time: " + timeStamp + " current: " + currentTime);
                }
            });

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

    public static byte[] readFile(String file) throws IOException {
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
