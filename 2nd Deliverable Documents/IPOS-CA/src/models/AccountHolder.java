package models;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * AccountHolder
 *
 * Represents a consumer (customer) with a credit account at the pharmacy.
 * Maps to the {@code account_holders} table in the ipos_ca database.
 *
 * Account status lifecycle (Student Brief §8.2, Fig.1):
 *   NORMAL -> SUSPENDED (15th of month after unpaid balance) -> IN_DEFAULT (end of that month)
 *   Returning to NORMAL from IN_DEFAULT requires explicit staff intervention.
 *
 * Reminder state (CA-11, CA-12):
 *   status_1stReminder / status_2ndReminder: no_need | due | sent
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class AccountHolder {

    /** Account status matching the database ENUM */
    public enum AccountStatus { NORMAL, SUSPENDED, IN_DEFAULT }

    /** Reminder state matching the database ENUM */
    public enum ReminderStatus { no_need, due, sent }

    private int           holderId;
    private String        firstName;
    private String        lastName;
    private Date          dateOfBirth;
    private String        email;
    private String        phone;
    private String        addressLine1;
    private String        addressLine2;
    private String        city;
    private String        postcode;

    // Account financials
    private AccountStatus accountStatus;
    private BigDecimal    creditLimit;
    private BigDecimal    outstandingBalance;
    private int           discountPlanId;
    private BigDecimal    monthlyOrderTotal;  // for flexible discount calculation

    // Reminder state (pseudo-code from Student Brief §8.2)
    private ReminderStatus status1stReminder;
    private ReminderStatus status2ndReminder;
    private Date           date1stReminder;
    private Date           date2ndReminder;

    // Timestamps
    private Date           paymentDueDate;
    private Timestamp      createdAt;
    private Timestamp      updatedAt;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    /** Full constructor for loading from database */
    public AccountHolder(int holderId, String firstName, String lastName,
                         Date dateOfBirth, String email, String phone,
                         String addressLine1, String addressLine2, String city, String postcode,
                         AccountStatus accountStatus, BigDecimal creditLimit,
                         BigDecimal outstandingBalance, int discountPlanId,
                         BigDecimal monthlyOrderTotal,
                         ReminderStatus status1stReminder, ReminderStatus status2ndReminder,
                         Date date1stReminder, Date date2ndReminder, Date paymentDueDate) {
        this.holderId           = holderId;
        this.firstName          = firstName;
        this.lastName           = lastName;
        this.dateOfBirth        = dateOfBirth;
        this.email              = email;
        this.phone              = phone;
        this.addressLine1       = addressLine1;
        this.addressLine2       = addressLine2;
        this.city               = city;
        this.postcode           = postcode;
        this.accountStatus      = accountStatus;
        this.creditLimit        = creditLimit;
        this.outstandingBalance = outstandingBalance;
        this.discountPlanId     = discountPlanId;
        this.monthlyOrderTotal  = monthlyOrderTotal;
        this.status1stReminder  = status1stReminder;
        this.status2ndReminder  = status2ndReminder;
        this.date1stReminder    = date1stReminder;
        this.date2ndReminder    = date2ndReminder;
        this.paymentDueDate     = paymentDueDate;
    }

    /** Minimal constructor for creating a new account holder */
    public AccountHolder(String firstName, String lastName, String email,
                         String phone, String addressLine1, String city, String postcode) {
        this.firstName          = firstName;
        this.lastName           = lastName;
        this.email              = email;
        this.phone              = phone;
        this.addressLine1       = addressLine1;
        this.city               = city;
        this.postcode           = postcode;
        this.accountStatus      = AccountStatus.NORMAL;
        this.creditLimit        = new BigDecimal("500.00");
        this.outstandingBalance = BigDecimal.ZERO;
        this.monthlyOrderTotal  = BigDecimal.ZERO;
        this.discountPlanId     = 1;  // default: No Discount
        this.status1stReminder  = ReminderStatus.no_need;
        this.status2ndReminder  = ReminderStatus.no_need;
    }

    // ---------------------------------------------------------------
    // Business logic helpers
    // ---------------------------------------------------------------

    /** Returns full name for display purposes */
    public String getFullName() { return firstName + " " + lastName; }

    /**
     * Returns whether this account holder can make new purchases.
     * SUSPENDED and IN_DEFAULT accounts cannot place new orders.
     */
    public boolean canMakePurchases() {
        return accountStatus == AccountStatus.NORMAL;
    }

    /**
     * Returns whether the outstanding balance exceeds the credit limit.
     * Used to block new purchases even for NORMAL accounts.
     */
    public boolean isOverCreditLimit() {
        return outstandingBalance.compareTo(creditLimit) >= 0;
    }

    // ---------------------------------------------------------------
    // Getters & Setters
    // ---------------------------------------------------------------

    public int            getHolderId()                               { return holderId; }
    public void           setHolderId(int id)                         { this.holderId = id; }

    public String         getFirstName()                              { return firstName; }
    public void           setFirstName(String fn)                     { this.firstName = fn; }

    public String         getLastName()                               { return lastName; }
    public void           setLastName(String ln)                      { this.lastName = ln; }

    public Date           getDateOfBirth()                            { return dateOfBirth; }
    public void           setDateOfBirth(Date dob)                    { this.dateOfBirth = dob; }

    public String         getEmail()                                  { return email; }
    public void           setEmail(String e)                          { this.email = e; }

    public String         getPhone()                                  { return phone; }
    public void           setPhone(String p)                          { this.phone = p; }

    public String         getAddressLine1()                           { return addressLine1; }
    public void           setAddressLine1(String a)                   { this.addressLine1 = a; }

    public String         getAddressLine2()                           { return addressLine2; }
    public void           setAddressLine2(String a)                   { this.addressLine2 = a; }

    public String         getCity()                                   { return city; }
    public void           setCity(String c)                           { this.city = c; }

    public String         getPostcode()                               { return postcode; }
    public void           setPostcode(String pc)                      { this.postcode = pc; }

    public AccountStatus  getAccountStatus()                          { return accountStatus; }
    public void           setAccountStatus(AccountStatus s)           { this.accountStatus = s; }

    public BigDecimal     getCreditLimit()                            { return creditLimit; }
    public void           setCreditLimit(BigDecimal cl)               { this.creditLimit = cl; }

    public BigDecimal     getOutstandingBalance()                     { return outstandingBalance; }
    public void           setOutstandingBalance(BigDecimal ob)        { this.outstandingBalance = ob; }

    public int            getDiscountPlanId()                         { return discountPlanId; }
    public void           setDiscountPlanId(int pid)                  { this.discountPlanId = pid; }

    public BigDecimal     getMonthlyOrderTotal()                      { return monthlyOrderTotal; }
    public void           setMonthlyOrderTotal(BigDecimal t)          { this.monthlyOrderTotal = t; }

    public ReminderStatus getStatus1stReminder()                      { return status1stReminder; }
    public void           setStatus1stReminder(ReminderStatus r)      { this.status1stReminder = r; }

    public ReminderStatus getStatus2ndReminder()                      { return status2ndReminder; }
    public void           setStatus2ndReminder(ReminderStatus r)      { this.status2ndReminder = r; }

    public Date           getDate1stReminder()                        { return date1stReminder; }
    public void           setDate1stReminder(Date d)                  { this.date1stReminder = d; }

    public Date           getDate2ndReminder()                        { return date2ndReminder; }
    public void           setDate2ndReminder(Date d)                  { this.date2ndReminder = d; }

    public Date           getPaymentDueDate()                         { return paymentDueDate; }
    public void           setPaymentDueDate(Date d)                   { this.paymentDueDate = d; }

    public Timestamp      getCreatedAt()                              { return createdAt; }
    public Timestamp      getUpdatedAt()                              { return updatedAt; }

    @Override
    public String toString() {
        return "AccountHolder{id=" + holderId + ", name='" + getFullName()
               + "', status=" + accountStatus + ", balance=£" + outstandingBalance + "}";
    }
}
