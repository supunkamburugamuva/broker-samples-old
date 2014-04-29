import java.io.FileReader;
import java.io.IOException;

public abstract class AbstractSender {
    protected String content;

    protected String url;

    protected int interval;

    protected String id;

    protected boolean reset;

    protected String queue = "send";

    public void setUp(String []args) throws IOException {
        if (args.length == 1) {
            this.url= args[0];
            this.content = "Hello World";
            this.interval = 10;
            this.id = "1";
            this.reset = false;

            start(queue + id);
        } else if (args.length == 2) {
            String fileName = args[1];
            String content = readEntireFile(fileName);
            this.url= args[0];
            this.content = content;
            this.interval = 10;
            this.id = "1";
            this.reset = false;
            start(queue + id);
        } else if (args.length == 3) {
            String fileName = args[1];
            String content = readEntireFile(fileName);
            int time = Integer.parseInt(args[2]);
            this.url= args[0];
            this.content = content;
            this.interval = time;
            this.id = "1";
            this.reset = false;

            start(queue + id);
        } else if (args.length == 4) {
            String fileName = args[1];
            String content = readEntireFile(fileName);
            int time = Integer.parseInt(args[2]);
            this.url= args[0];
            this.content = content;
            this.interval = time;
            this.reset = false;
            for (int i = 0; i < Integer.parseInt(args[3]); i++) {
                start(queue + i);
            }
        } else if (args.length == 5) {
            String fileName = args[1];
            String content = readEntireFile(fileName);
            int time = Integer.parseInt(args[2]);
            for (int i = 0; i < Integer.parseInt(args[3]); i++) {
                start(queue + i);
            }
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

    public abstract void start(String id);
}
