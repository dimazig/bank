package ziggy.bank;

/**
 * Created by Dmitry Tsigelnik on 5/8/19.
 */
public class Main {

    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    }

    public static void main(String[] args) throws Exception {
        Application application = new Application();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> application.stop()));
        application.start();

    }
}
