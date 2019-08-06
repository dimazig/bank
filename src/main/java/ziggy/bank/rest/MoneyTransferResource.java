package ziggy.bank.rest;

import ziggy.bank.model.Transfer;
import ziggy.bank.service.MoneyTransferService;
import ziggy.bank.utils.Constants;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by Dmitry Tsigelnik on 3/8/19.
 */
@Path("/transfers")
public class MoneyTransferResource {

    private final MoneyTransferService moneyTransferService;

    @Inject
    public MoneyTransferResource(MoneyTransferService moneyTransferService) {
        this.moneyTransferService = moneyTransferService;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Transfer submitTransfer(Transfer transfer) {
        validate(transfer);
        final UUID id = moneyTransferService.submitTransfer(transfer);
        return moneyTransferService.getTransfer(id);
    }

    private void validate(Transfer transfer) {
        final BigDecimal amount = transfer.getAmount();
        if (amount == null || amount.scale() > Constants.SCALE || amount.compareTo(BigDecimal.ZERO) <= 0
                || transfer.getAccountFrom() == null || transfer.getAccountTo() == null) {
            throw new WebApplicationException(422);
        }


    }


    @GET
    @Path("/{id}")
    @Produces(APPLICATION_JSON)
    public Transfer getTransfer(@PathParam("id") UUID id) {
        final Transfer transfer = moneyTransferService.getTransfer(id);
        if (transfer == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return transfer;
    }

    @GET
    @Path("/by-account/{id}")
    @Produces(APPLICATION_JSON)
    public List<Transfer> getTransfers(@PathParam("id") UUID id) {
        return moneyTransferService.getTransfersByAccount(id);
    }

}
