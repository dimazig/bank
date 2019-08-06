package ziggy.bank;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.ignite.Ignite;
import ziggy.bank.ignite.IgniteInitializer;
import ziggy.bank.rest.RestContext;
import ziggy.bank.service.AccountService;
import ziggy.bank.service.MoneyTransferProcessor;
import ziggy.bank.service.MoneyTransferService;

import javax.ws.rs.ext.RuntimeDelegate;
import java.net.InetSocketAddress;

import static java.lang.Integer.getInteger;

/**
 * Created by Dmitry Tsigelnik on 3/8/19.
 */
public class Application {

    public static final int DEFAULT_PORT = 8080;

    private IgniteInitializer igniteInitializer;
    private MoneyTransferService moneyTransferService;
    private MoneyTransferProcessor moneyTransferProcessor;
    private AccountService accountService;
    private Ignite ignite;

    private HttpServer httpServer;
    private RestContext context;

    public void start() throws Exception {
        init();

        context = new RestContext(moneyTransferService, accountService);

        httpServer = HttpServer.create(new InetSocketAddress(getInteger("http.port", DEFAULT_PORT)), 0);
        httpServer.createContext("/api", RuntimeDelegate.getInstance().createEndpoint(context, HttpHandler.class));
        httpServer.start();
    }

    private void init() {
        igniteInitializer = new IgniteInitializer();
        ignite = igniteInitializer.init();

        accountService = new AccountService(ignite);
        moneyTransferService = new MoneyTransferService(ignite);
        moneyTransferProcessor = new MoneyTransferProcessor(moneyTransferService);

        moneyTransferProcessor.start();
    }

    public void stop() {
        httpServer.stop(0);
        moneyTransferProcessor.stop();
        ignite.close();
    }


}
