package ziggy.bank.rest;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import ziggy.bank.service.AccountService;
import ziggy.bank.service.MoneyTransferService;

/**
 * Created by Dmitry Tsigelnik on 3/8/19.
 */
public class RestContext extends ResourceConfig {


    public RestContext(MoneyTransferService moneyTransferService,
                       AccountService accountService) {

        register(MoneyTransferResource.class);
        register(AccountResource.class);
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(moneyTransferService).to(MoneyTransferService.class);
                bind(accountService).to(AccountService.class);
            }
        });
    }

}
