package ziggy.bank.model;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by Dmitry Tsigelnik on 3/8/19.
 */
public class Transfer {
    public enum FailReason {INSUFFICIENT_FUNDS, INCORRECT_AMOUNT, FROM_ACCOUNT_NOT_FOUND, TO_ACCOUNT_NOT_FOUND}

    public enum Status {PENDING, DONE, FAILED}


    @QuerySqlField(index = true)
    private UUID id;

    @QuerySqlField(index = true)
    private UUID accountFrom;
    @QuerySqlField(index = true)
    private UUID accountTo;
    private BigDecimal amount;
    @QuerySqlField(index = true)
    private Date date;

    private Status status = Status.PENDING;
    private List<FailReason> failReasons = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountFrom() {
        return accountFrom;
    }

    public void setAccountFrom(UUID accountFrom) {
        this.accountFrom = accountFrom;
    }

    public UUID getAccountTo() {
        return accountTo;
    }

    public void setAccountTo(UUID accountTo) {
        this.accountTo = accountTo;
    }

    public Date getDate() {
        return date;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<FailReason> getFailReasons() {
        return failReasons;
    }

    public void setFailReasons(List<FailReason> failReasons) {
        this.failReasons = failReasons;
    }

}
