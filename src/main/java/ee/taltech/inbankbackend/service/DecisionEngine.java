package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

import static ee.taltech.inbankbackend.config.DecisionEngineConstants.MAX_AGE_MONTHS;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier = 0; // Use a local variable instead of a shared field.


    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount   Requested loan amount
     * @param loanPeriod   Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException   If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException   If the requested loan period is invalid
     * @throws NoValidLoanException         If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException, InvalidAgeException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }

        int outputLoanAmount;
        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }


        while (highestValidLoanAmount(loanPeriod) < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            loanPeriod++;
        }

        validateAgeForLoan(personalCode, loanPeriod);

        if (loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            outputLoanAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidLoanAmount(loanPeriod));
            Double creditScore = calculateCreditScore(outputLoanAmount, loanPeriod);
            if (creditScore < 0.1) {
                throw new NoValidLoanException("No valid loan found!");
            }
        } else {
            throw new NoValidLoanException("No valid loan found!");
        }

        return new Decision(outputLoanAmount, loanPeriod, null);
    }

    private void validateAgeForLoan(String personalCode, int loanPeriodMonths) throws InvalidPersonalCodeException, InvalidAgeException {
        int ageMonths = calculateAgeFromPersonalCode(personalCode);
        int maxAcceptableAgeMonths = MAX_AGE_MONTHS - loanPeriodMonths;
        int ageYears = ageMonths/12;

        if (ageYears < 18 || ageMonths > maxAcceptableAgeMonths) {
            throw new InvalidAgeException("Customer age is not within the approved range for loan.");
        }
    }

    private int calculateAgeFromPersonalCode(String personalCode) throws InvalidPersonalCodeException {
        String millennia = personalCode.substring(0, 1);
        String birthdateStr = personalCode.substring(1, 7);

        int yearPrefix;
        int yearSuffix = Integer.parseInt(birthdateStr.substring(0, 2));
        int month = Integer.parseInt(birthdateStr.substring(2, 4));
        int day = Integer.parseInt(birthdateStr.substring(4, 6));

        yearPrefix = switch (millennia) {
            case "1", "2" -> 1800;
            case "3", "4" -> 1900;
            case "5", "6" -> 2000;
            default -> throw new InvalidPersonalCodeException("Invalid year of birth on  ID code!");
        };

        int year = yearPrefix + yearSuffix;

        LocalDate birthDate = LocalDate.of(year, month, day);
        LocalDate currentDate = LocalDate.now();

        Period period = Period.between(birthDate, currentDate);
        return period.getYears() * 12 + period.getMonths();

    }

    private Double calculateCreditScore(int loanAmount, int loanPeriod) {
        return (((double) creditModifier / (double) loanAmount) * (double) loanPeriod) / 10.00;
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan period.
     *
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(int loanPeriod) {

        return creditModifier * loanPeriod;
    }

    private int getCreditModifier(String personalCode) {
        int lastFourDigits = Integer.parseInt(personalCode.substring(personalCode.length() - 4));
        if (lastFourDigits % 5 == 0) {
            return 0;
        }

        int lastDigit = lastFourDigits % 10;
        if (lastDigit == 6 || lastDigit == 1) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (lastDigit == 7 || lastDigit == 2) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        } else {
            return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
        }
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount   Requested loan amount
     * @param loanPeriod   Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException   If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException   If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }

        if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

    }
}
