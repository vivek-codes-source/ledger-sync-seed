\# Task 1 — Simplify Money App Teardown



\## Objective

Review the Simplify Money app's transaction tracking and identify cases where bank transaction data was not reflected in the app's transaction list.



\## Observations



\### September

The Transactions screen showed 3 transactions totaling ₹4,783. The app displayed:

\- Loans \& EMI: 2 transactions

\- Others: 1 transaction

\- "No source was found for your spends yet"



\### July

The app showed 6 transactions with a displayed total of ₹1,16,377.



The visible transactions included amounts such as ₹23, ₹4,000, ₹44,000, ₹472, ₹372 and ₹67,511.



A July SMS showed:

\- ₹1,19,000 CREDITED to account ending 3079 by BRANCH on 24-Jul-2026.



This credit was not visible in the July Simplify Money transaction list.



\### August

The app showed 7 transactions totaling ₹3,309.



An SMS showed:

\- ₹50 CREDITED to account ending 3079 by Mob Bk on 11-Aug-2026.



This credit was not visible in the August Simplify Money transaction list.



\## Key Finding

The teardown identified bank credits present in SMS data but absent from the corresponding Simplify Money transaction lists.



The July ₹1,19,000 credit and August ₹50 credit are concrete examples.



\## Evidence

Screenshots of the relevant Simplify Money transaction screens and SMS messages are included with the submission.



