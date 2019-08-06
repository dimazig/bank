package ziggy.bank.rest;

import ziggy.bank.model.Account;
import ziggy.bank.service.AccountService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by Dmitry Tsigelnik on 5/8/19.
 */
@Path("/accounts")
public class AccountResource {

    private final AccountService accountService;

    @Inject
    public AccountResource(AccountService accountService) {
        this.accountService = accountService;
    }


    @GET
    @Path("/{id}")
    public Account getAccount(@PathParam("id") UUID id) {
        Account account = accountService.getAccount(id);
        if (account == null) {
            throw new WebApplicationException(404);
        }

        return account;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Account createAccount(Account account) {
        final UUID id = accountService.createAccount(account);
        return accountService.getAccount(id);
    }

}
