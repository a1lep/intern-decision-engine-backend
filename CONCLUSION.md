## Fixed Shortcomings in Ticket 101 

### Issues Fixed:
- `getModifier` method in [DecisionEngine](src/main/java/ee/taltech/inbankbackend/service/DecisionEngine.java) class
- Added missing credit score calculation
- Updated `MAXIMUM_LOAN_PERIOD` from 60 to 48
- Tests for `DecisionEngine` class

### `getModifier` Method Explanation:
The previous implementation had incorrect logic for determining the credit modifier. The fixed method follows these steps:

1. Extracts the last 4 digits of the personal code.
2. If the last 4 digits are divisible by 5, it returns `0`, indicating debt.
3. Otherwise, the last digit of these 4 digits is used to determine the credit segment:
    - Last digit is 6 or 1 → Segment 1 (Credit Modifier: `100`)
    - Last digit is 7 or 2 → Segment 2 (Credit Modifier: `300`)
    - Any other digit → Segment 3 (Credit Modifier: `1000`)

---

## Code Review

### Well-Implemented Aspects:
- Readable code
- Proper exception handling
- Clear method and object naming
- Good test coverage

### Improvements:

#### 1. Better API Error Handling
- Instead of returning an error code in the API response, a Global Exception Handler should be used.
- Implementing an `ErrorCode` ENUM would allow throwing specific exceptions without embedding error messages in the response object.

#### 2. Single Responsibility Principle (SRP)
- The `DecisionEngine` class should only handle loan calculations.
- Loan validation should be extracted into a separate `LoanValidator` class.

#### 3. Open/Closed Principle (OCP)
- Implementing interfaces would make the system open for extension but closed for modification, improving maintainability.

#### 4. Use of `final` for Immutability
- Variables that do not change should be declared `final` to improve readability and maintainability.

#### 5. Reduce Unnecessary Comments
- Methods should be self-explanatory through clear naming.
- Too many comments can become outdated and misleading when code changes.


