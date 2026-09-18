package in.simplifymoney.ledgersync.parse;

import in.simplifymoney.ledgersync.model.Direction;
import in.simplifymoney.ledgersync.model.RawMessage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IciciSmsParser implements MessageParser {

    public static final String SENDER = "VM-ICICIB-T";

    // Format 1:
    // Dear Customer, Acct XX9075 is debited with INR 22.50
    // on 01/07/2026 10:22. Info: UPI/VEGETABLE VENDOR.
    private static final Pattern V1 = Pattern.compile(
            "Acct XX(?<acct>\\d{4}) is "
                    + "(?<dir>debited|credited) with "
                    + "(?:INR|Rs\\.?)\\s*(?<amount>[0-9,]+(?:\\.[0-9]+)?) "
                    + "on (?<when>\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2})\\. "
                    + "Info: (?<merchant>[^.]+)\\.",
            Pattern.CASE_INSENSITIVE
    );

    // Format 2:
    // ICICI Bank Acct XX9075 Dr INR 1250.33
    // on 23-Jul-2026 16:52; INTEREST CREDIT ref no ...
    private static final Pattern V2 = Pattern.compile(
            "ICICI Bank Acct XX(?<acct>\\d{4}) "
                    + "(?<dir>Cr|Dr) "
                    + "(?:INR|Rs\\.?)\\s*(?<amount>[0-9,]+(?:\\.[0-9]+)?) "
                    + "on (?<when>\\d{2}-\\w{3}-\\d{4} \\d{2}:\\d{2}); "
                    + "(?<merchant>.+?) ref no \\d+\\. "
                    + "BalAvl Rs\\s*[0-9,]+(?:\\.[0-9]+)?",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean supports(RawMessage m) {
        return SENDER.equalsIgnoreCase(m.sender());
    }

    @Override
    public Optional<ParsedTxn> parse(RawMessage m) {

        String body = m.body();

        // Try old ICICI format first
        Matcher matcher = V1.matcher(body);

        if (matcher.find()) {

            String accountLast4 = matcher.group("acct");

            Direction direction =
                    matcher.group("dir").equalsIgnoreCase("debited")
                            ? Direction.DEBIT
                            : Direction.CREDIT;

            BigDecimal amount =
                    new BigDecimal(
                            matcher.group("amount").replace(",", "")
                    ).setScale(2);

            OffsetDateTime occurredAt =
                    LocalDateTime.parse(
                            matcher.group("when"),
                            DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy HH:mm"
                            )
                    ).atOffset(ZoneOffset.ofHoursMinutes(5, 30));

            String merchant = matcher.group("merchant").trim();

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

        // Try new ICICI format
        matcher = V2.matcher(body);

        if (matcher.find()) {

            String accountLast4 = matcher.group("acct");

            Direction direction =
                    matcher.group("dir").equalsIgnoreCase("Dr")
                            ? Direction.DEBIT
                            : Direction.CREDIT;

            BigDecimal amount =
                    new BigDecimal(
                            matcher.group("amount").replace(",", "")
                    ).setScale(2);

            OffsetDateTime occurredAt =
                    LocalDateTime.parse(
                            matcher.group("when"),
                            DateTimeFormatter.ofPattern(
                                    "dd-MMM-yyyy HH:mm"
                            )
                    ).atOffset(ZoneOffset.ofHoursMinutes(5, 30));

            String merchant = matcher.group("merchant").trim();

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

        return Optional.empty();
    }
}