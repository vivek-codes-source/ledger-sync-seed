package in.simplifymoney.ledgersync.store;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import in.simplifymoney.ledgersync.model.Category;
import in.simplifymoney.ledgersync.model.Direction;
import in.simplifymoney.ledgersync.model.NormalizedTxn;
import org.bson.Document;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MongoDocumentStore implements DocumentStore, AutoCloseable {

    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoDocumentStore() {
        this(
                System.getenv().getOrDefault(
                        "MONGO_URI",
                        "mongodb://localhost:27017"
                )
        );
    }

    public MongoDocumentStore(String uri) {
        this.client = MongoClients.create(uri);

        MongoDatabase database = client.getDatabase("ledger_sync");

        this.collection = database.getCollection("transactions");

        createIndexes();
    }

    private void createIndexes() {
        collection.createIndex(
                Indexes.compoundIndex(
                        Indexes.ascending("accountLast4"),
                        Indexes.descending("occurredAt")
                )
        );

        collection.createIndex(
                Indexes.ascending("accountLast4")
        );

        collection.createIndex(
                Indexes.ascending("sourceMessageIds")
        );
    }

    @Override
    public List<NormalizedTxn> forAccountMonth(
            String accountLast4,
            YearMonth month
    ) {
        OffsetDateTime start =
                month.atDay(1).atStartOfDay()
                        .atOffset(java.time.ZoneOffset.ofHoursMinutes(5, 30));

        OffsetDateTime end =
                month.plusMonths(1).atDay(1).atStartOfDay()
                        .atOffset(java.time.ZoneOffset.ofHoursMinutes(5, 30));

        List<Document> documents = collection.find(
                Filters.and(
                        Filters.eq("accountLast4", accountLast4),
                        Filters.gte("occurredAt", start.toString()),
                        Filters.lt("occurredAt", end.toString())
                )
        ).sort(Indexes.descending("occurredAt")).into(new ArrayList<>());

        return documents.stream()
                .map(this::fromDocument)
                .toList();
    }

    @Override
    public Map<Category, BigDecimal> categoryTotals(String accountLast4) {

        Map<Category, BigDecimal> totals =
                new EnumMap<>(Category.class);

        for (Document document : collection.find(
                Filters.eq("accountLast4", accountLast4)
        )) {
            NormalizedTxn txn = fromDocument(document);

            totals.merge(
                    txn.category(),
                    txn.amount(),
                    BigDecimal::add
            );
        }

        return totals;
    }

    @Override
    public Optional<NormalizedTxn> byMessageId(String messageId) {

        Document document = collection.find(
                Filters.eq("sourceMessageIds", messageId)
        ).first();

        if (document == null) {
            return Optional.empty();
        }

        return Optional.of(fromDocument(document));
    }

    @Override
    public void save(NormalizedTxn txn) {

        Document document = new Document()
                .append("accountLast4", txn.accountLast4())
                .append("occurredAt", txn.occurredAt().toString())
                .append("direction", txn.direction().name())
                .append("amount", txn.amount().toPlainString())
                .append("category", txn.category().name())
                .append("merchant", txn.merchant())
                .append("sourceMessageIds", txn.sourceMessageIds());

        collection.insertOne(document);
    }

    private NormalizedTxn fromDocument(Document document) {

        return new NormalizedTxn(
                document.getString("accountLast4"),
                OffsetDateTime.parse(
                        document.getString("occurredAt")
                ),
                Direction.valueOf(
                        document.getString("direction")
                ),
                new BigDecimal(
                        document.getString("amount")
                ),
                Category.valueOf(
                        document.getString("category")
                ),
                document.getString("merchant"),
                document.getList(
                        "sourceMessageIds",
                        String.class
                )
        );
    }

    @Override
    public void close() {
        client.close();
    }
}