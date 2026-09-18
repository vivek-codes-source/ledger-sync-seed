package in.simplifymoney.ledgersync.parse;

import in.simplifymoney.ledgersync.model.Direction;
import in.simplifymoney.ledgersync.model.RawMessage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailParser implements MessageParser {

    private static final Pattern ACCOUNT =
            Pattern.compile("account ending (\\d{4})", Pattern.CASE_INSENSITIVE);

    private static final Pattern TRANSACTION =
            Pattern.compile(
                    "has been (credited|debited) with (?:INR|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]+)?)",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern MERCHANT =
            Pattern.compile("Merchant / Remarks:\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern DATE =
            Pattern.compile("Date:\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z");

    @Override
    public boolean supports(RawMessage m) {
        return "email".equalsIgnoreCase(m.channel());
    }

    @Override
    public Optional<ParsedTxn> parse(RawMessage m) {

        String body = m.body();

        Matcher accountMatcher = ACCOUNT.matcher(body);
        Matcher transactionMatcher = TRANSACTION.matcher(body);
        Matcher merchantMatcher = MERCHANT.matcher(body);
        Matcher dateMatcher = DATE.matcher(body);

        if (!accountMatcher.find()
                || !transactionMatcher.find()
                || !merchantMatcher.find()
                || !dateMatcher.find()) {
            return Optional.empty();
        }

        String accountLast4 = accountMatcher.group(1);

        Direction direction =
                transactionMatcher.group(1).equalsIgnoreCase("credited")
                        ? Direction.CREDIT
                        : Direction.DEBIT;

        BigDecimal amount =
                new BigDecimal(
                        transactionMatcher.group(2).replace(",", "")
                ).setScale(2);

        String merchant = merchantMatcher.group(1).trim();

        OffsetDateTime occurredAt =
                OffsetDateTime.parse(
                        dateMatcher.group(1).trim(),
                        DATE_FORMATTER
                );

        return Optional.of(
                new ParsedTxn(
                        accountLast4,
                        occurredAt,
                        direction,
                        amount,
                        merchant,
                        null,
                        m.messageId()
                )
        );
    }
}