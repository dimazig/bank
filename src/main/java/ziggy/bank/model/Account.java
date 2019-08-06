package ziggy.bank.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Created by Dmitry Tsigelnik on 3/8/19.
 */
public class Account {
    private UUID id;
    private BigDecimal balance = BigDecimal.ZERO;

    public Account() {
    }

    public Account(BigDecimal balance) {
        this.balance = balance;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id) &&
                Objects.equals(balance, account.balance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, balance);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Account.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("balance=" + balance)
                .toString();
    }
}
