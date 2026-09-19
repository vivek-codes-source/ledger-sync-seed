\# Task 0 — Ledger Sync One-Pager



\## Objective

Build a reliable transaction ledger from bank messages while avoiding duplicate transactions and keeping spending, income, micro-transactions, and transfers distinct.



\## Input

\- `fixtures/corpus-a.jsonl`

\- 522 bank messages

\- Two accounts: 4821 and 9075



\## Processing Flow

Bank messages

→ format-specific parsers

→ normalized transactions

→ deduplication

→ category classification

→ SQL ledger

→ MongoDB document store

→ reports and consistency checks



\## Key Decisions

\- Used bank-stated transaction time.

\- Normalized amounts using `BigDecimal`.

\- Deduplicated repeated message evidence using source message IDs.

\- Classified UPI debits ≤ ₹100 as `MICRO`.

\- Kept `TRANSFER` separate from spending and income.

\- Used MongoDB with indexes for the required access patterns.

\- Made backfill idempotent.

\- Added transaction-level SQL/Mongo consistency checking.



\## Reliability

\- Added regression coverage for whole-rupee amount parsing.

\- Re-running ingest does not create duplicate transactions.

\- Re-running backfill is idempotent.

\- Consistency checker detects deliberate document changes.



\## Current Status

The core ingestion, ledger generation, MongoDB document store, backfill, consistency checking, and report generation are implemented.



Known limitations are documented in the README, including reconciliation and transfer-classification uncertainty.



