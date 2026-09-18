package in.simplifymoney.ledgersync.ingest;

import in.simplifymoney.ledgersync.json.Json;
import in.simplifymoney.ledgersync.model.Category;
import in.simplifymoney.ledgersync.model.Direction;
import in.simplifymoney.ledgersync.model.NormalizedTxn;
import in.simplifymoney.ledgersync.model.RawMessage;
import in.simplifymoney.ledgersync.parse.ParsedTxn;
import in.simplifymoney.ledgersync.parse.Parsers;
import in.simplifymoney.ledgersync.store.LedgerStore;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class IngestService {

    private final Parsers parsers;
    private final LedgerStore store;

    public IngestService(Parsers parsers, LedgerStore store) {
        this.parsers = parsers;
        this.store = store;
    }

    public Stats ingestFile(Path corpus) throws IOException {

        List<RawMessage> messages = readCorpus(corpus);

        /*
         * A single real transaction can appear in multiple messages
         * such as SMS + email or duplicate SMS.
         *
         * Deduplicate messages belonging to the same real transaction.
         */
        Map<TransactionKey, NormalizedTxn> transactions =
                new LinkedHashMap<>();

        int skipped = 0;

        for (RawMessage m : messages) {

            Optional<ParsedTxn> parsed = parsers.parse(m);

            if (parsed.isEmpty()) {
                skipped++;
                continue;
            }

            ParsedTxn p = parsed.get();

            TransactionKey key = new TransactionKey(
                    p.accountLast4(),
                    p.occurredAt(),
                    p.direction(),
                    p.amount(),
                    p.merchant()
            );

            NormalizedTxn existing = transactions.get(key);

            if (existing == null) {

                transactions.put(
                        key,
                        toTransaction(p)
                );

            } else {

                /*
                 * Same real transaction with another evidence message.
                 * Merge all source message IDs.
                 */
                List<String> sourceIds =
                        new ArrayList<>(existing.sourceMessageIds());

                if (!sourceIds.contains(p.sourceMessageId())) {
                    sourceIds.add(p.sourceMessageId());
                }

                transactions.put(
                        key,
                        new NormalizedTxn(
                                existing.accountLast4(),
                                existing.occurredAt(),
                                existing.direction(),
                                existing.amount(),
                                existing.category(),
                                existing.merchant(),
                                sourceIds
                        )
                );
            }
        }

        /*
         * Load transactions already present in the ledger.
         * This makes repeated ingestion idempotent.
         */
        Map<TransactionKey, NormalizedTxn> existingTransactions =
                new LinkedHashMap<>();

        for (NormalizedTxn txn : store.all()) {

            TransactionKey key = new TransactionKey(
                    txn.accountLast4(),
                    txn.occurredAt(),
                    txn.direction(),
                    txn.amount(),
                    txn.merchant()
            );

            existingTransactions.put(key, txn);
        }

        /*
         * Save only transactions that are not already in the ledger.
         */
        int written = 0;

        for (Map.Entry<TransactionKey, NormalizedTxn> entry
                : transactions.entrySet()) {

            if (existingTransactions.containsKey(entry.getKey())) {
                continue;
            }

            store.save(entry.getValue());
            written++;
        }

        return new Stats(
                messages.size(),
                written,
                skipped
        );
    }

    public static List<RawMessage> readCorpus(Path corpus) throws IOException {

        List<RawMessage> messages = new ArrayList<>();

        try (Stream<String> lines = Files.lines(corpus)) {

            lines
                    .filter(line -> !line.isBlank())
                    .forEach(line -> {

                        Map<String, Object> obj =
                                Json.parseObject(line);

                        RawMessage message = new RawMessage(
                                (String) obj.get("message_id"),
                                (String) obj.get("channel"),
                                (String) obj.get("sender"),
                                OffsetDateTime.parse(
                                        (String) obj.get("received_at")
                                ),
                                (String) obj.get("device_id"),
                                (String) obj.get("body")
                        );

                        messages.add(message);
                    });
        }

        return messages;
    }

    private NormalizedTxn toTransaction(ParsedTxn p) {

        Category category;

        if (p.direction() == Direction.DEBIT
                && p.merchant() != null
                && p.merchant().toUpperCase().startsWith("UPI/")
                && p.amount().compareTo(new BigDecimal("100.00")) <= 0) {

            category = Category.MICRO;

        } else if (p.direction() == Direction.DEBIT) {

            category = Category.SPEND;

        } else {

            category = Category.INCOME;
        }

        return new NormalizedTxn(
                p.accountLast4(),
                p.occurredAt(),
                p.direction(),
                p.amount(),
                category,
                p.merchant(),
                List.of(p.sourceMessageId())
        );
    }
    private record TransactionKey(
            String accountLast4,
            OffsetDateTime occurredAt,
            Direction direction,
            java.math.BigDecimal amount,
            String merchant
    ) {
    }

    public record Stats(
            int messagesRead,
            int transactionsWritten,
            int messagesSkipped
    ) {
    }
}