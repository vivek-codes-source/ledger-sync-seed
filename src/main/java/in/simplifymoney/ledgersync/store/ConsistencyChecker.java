package in.simplifymoney.ledgersync.store;

import in.simplifymoney.ledgersync.model.NormalizedTxn;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ConsistencyChecker {

    private final SqlLedgerStore sql;
    private final DocumentStore documents;

    public ConsistencyChecker(SqlLedgerStore sql, DocumentStore documents) {
        this.sql = sql;
        this.documents = documents;
    }

    public List<Divergence> check() {
        List<Divergence> divergences = new ArrayList<>();

        List<NormalizedTxn> sqlTransactions = sql.all();

        for (NormalizedTxn sqlTxn : sqlTransactions) {

            for (String messageId : sqlTxn.sourceMessageIds()) {

                var documentTxn = documents.byMessageId(messageId);

                if (documentTxn.isEmpty()) {
                    divergences.add(new Divergence(
                            "missing transaction for messageId " + messageId,
                            describe(sqlTxn),
                            "missing"
                    ));
                    continue;
                }

                NormalizedTxn docTxn = documentTxn.get();

                compare(
                        divergences,
                        messageId,
                        sqlTxn,
                        docTxn
                );
            }
        }

        return divergences;
    }

    private void compare(
            List<Divergence> divergences,
            String messageId,
            NormalizedTxn sqlTxn,
            NormalizedTxn docTxn
    ) {
        if (!Objects.equals(
                sqlTxn.accountLast4(),
                docTxn.accountLast4())) {

            divergences.add(new Divergence(
                    "account for messageId " + messageId,
                    sqlTxn.accountLast4(),
                    docTxn.accountLast4()
            ));
        }

        if (!Objects.equals(
                sqlTxn.occurredAt(),
                docTxn.occurredAt())) {

            divergences.add(new Divergence(
                    "occurredAt for messageId " + messageId,
                    sqlTxn.occurredAt().toString(),
                    docTxn.occurredAt().toString()
            ));
        }

        if (!Objects.equals(
                sqlTxn.direction(),
                docTxn.direction())) {

            divergences.add(new Divergence(
                    "direction for messageId " + messageId,
                    sqlTxn.direction().toString(),
                    docTxn.direction().toString()
            ));
        }

        if (!Objects.equals(
                sqlTxn.amount(),
                docTxn.amount())) {

            divergences.add(new Divergence(
                    "amount for messageId " + messageId,
                    sqlTxn.amount().toPlainString(),
                    docTxn.amount().toPlainString()
            ));
        }

        if (!Objects.equals(
                sqlTxn.category(),
                docTxn.category())) {

            divergences.add(new Divergence(
                    "category for messageId " + messageId,
                    sqlTxn.category().toString(),
                    docTxn.category().toString()
            ));
        }

        if (!Objects.equals(
                sqlTxn.merchant(),
                docTxn.merchant())) {

            divergences.add(new Divergence(
                    "merchant for messageId " + messageId,
                    String.valueOf(sqlTxn.merchant()),
                    String.valueOf(docTxn.merchant())
            ));
        }
    }

    private String describe(NormalizedTxn txn) {
        return "account=" + txn.accountLast4()
                + ", occurredAt=" + txn.occurredAt()
                + ", direction=" + txn.direction()
                + ", amount=" + txn.amount()
                + ", category=" + txn.category()
                + ", merchant=" + txn.merchant();
    }

    /** One place the two stores disagree. */
    public record Divergence(
            String what,
            String inSql,
            String inDocuments
    ) {}
}