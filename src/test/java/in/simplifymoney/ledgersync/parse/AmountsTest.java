package in.simplifymoney.ledgersync.parse;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmountsTest {

    @Test
    void waterCanMessageShouldReadTransactionAmountNotBalance() {

        String body =
                "Rs.5 debited from a/c **4821 on 11-09-26 at 10:15 " +
                        "to UPI/WATER CAN. Avl Bal: Rs.92,213.10.";

        BigDecimal amount = Amounts.first(body);

        System.out.println("Parsed amount = " + amount);

        assertEquals(
                new BigDecimal("5.00"),
                amount,
                "Parsed amount was: " + amount
        );
    }
}