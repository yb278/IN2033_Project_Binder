package models;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Sale
 *
 * Represents a completed sales transaction in the pharmacy.
 * Maps to the {@code sales} table in the ipos_ca database.
 *
 * Covers use cases: CA-13 (Record Sale), CA-14 (Accept Payment),
 * CA-15 (Cash Payment), CA-16 (Card Payment), CA-17 (Generate Receipt/Invoice)
 *
 * A sale is either to:
 *   - An account holder (holderId set; payment method may be ACCOUNT, CASH, or CARD)
 *   - An occasional customer (holderId = 0; payment method CASH or CARD only)
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class Sale {

    /** Payment method matching the database ENUM */
    public enum PaymentMethod { CASH, CARD, ACCOUNT }

    private int           saleId;
    private int           servedBy;           // user_id of the pharmacist
    private int           holderId;           // 0 if occasional customer
    private String        occasionalName;     // name for one-off customers
    private Timestamp     saleTimestamp;
    private BigDecimal    subtotal;           // before VAT
    private BigDecimal    vatAmount;
    private BigDecimal    discountAmount;
    private BigDecimal    totalAmount;        // final amount due

    // Payment details
    private PaymentMethod paymentMethod;
    private BigDecimal    paymentReceived;   // cash tendered
    private BigDecimal    changeGiven;

    // Card details (CA-16)
    private String        cardType;          // e.g. VISA
    private String        cardFirstFour;
    private String        cardLastFour;
    private String        cardExpiry;        // MM/YY

    private String        invoiceNumber;     // generated ref e.g. "INV-00001"

    /** Line items in this sale */
    private List<SaleItem> items = new ArrayList<>();

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /** Full constructor for loading from DB */
    public Sale(int saleId, int servedBy, int holderId, String occasionalName,
                Timestamp saleTimestamp, BigDecimal subtotal, BigDecimal vatAmount,
                BigDecimal discountAmount, BigDecimal totalAmount,
                PaymentMethod paymentMethod, BigDecimal paymentReceived, BigDecimal changeGiven,
                String cardType, String cardFirstFour, String cardLastFour, String cardExpiry,
                String invoiceNumber) {
        this.saleId          = saleId;
        this.servedBy        = servedBy;
        this.holderId        = holderId;
        this.occasionalName  = occasionalName;
        this.saleTimestamp   = saleTimestamp;
        this.subtotal        = subtotal;
        this.vatAmount       = vatAmount;
        this.discountAmount  = discountAmount;
        this.totalAmount     = totalAmount;
        this.paymentMethod   = paymentMethod;
        this.paymentReceived = paymentReceived;
        this.changeGiven     = changeGiven;
        this.cardType        = cardType;
        this.cardFirstFour   = cardFirstFour;
        this.cardLastFour    = cardLastFour;
        this.cardExpiry      = cardExpiry;
        this.invoiceNumber   = invoiceNumber;
    }

    /** Constructor for creating a new sale */
    public Sale(int servedBy, int holderId, String occasionalName, PaymentMethod paymentMethod) {
        this.servedBy       = servedBy;
        this.holderId       = holderId;
        this.occasionalName = occasionalName;
        this.paymentMethod  = paymentMethod;
        this.subtotal        = BigDecimal.ZERO;
        this.vatAmount       = BigDecimal.ZERO;
        this.discountAmount  = BigDecimal.ZERO;
        this.totalAmount     = BigDecimal.ZERO;
        this.saleTimestamp   = new Timestamp(System.currentTimeMillis());
    }

    // ---------------------------------------------------------------
    // Getters & Setters
    // ---------------------------------------------------------------

    public int           getSaleId()                          { return saleId; }
    public void          setSaleId(int id)                    { this.saleId = id; }

    public int           getServedBy()                        { return servedBy; }
    public void          setServedBy(int uid)                 { this.servedBy = uid; }

    public int           getHolderId()                        { return holderId; }
    public void          setHolderId(int hid)                 { this.holderId = hid; }

    public String        getOccasionalName()                  { return occasionalName; }
    public void          setOccasionalName(String name)       { this.occasionalName = name; }

    public Timestamp     getSaleTimestamp()                   { return saleTimestamp; }
    public void          setSaleTimestamp(Timestamp ts)       { this.saleTimestamp = ts; }

    public BigDecimal    getSubtotal()                        { return subtotal; }
    public void          setSubtotal(BigDecimal st)           { this.subtotal = st; }

    public BigDecimal    getVatAmount()                       { return vatAmount; }
    public void          setVatAmount(BigDecimal vat)         { this.vatAmount = vat; }

    public BigDecimal    getDiscountAmount()                  { return discountAmount; }
    public void          setDiscountAmount(BigDecimal da)     { this.discountAmount = da; }

    public BigDecimal    getTotalAmount()                     { return totalAmount; }
    public void          setTotalAmount(BigDecimal ta)        { this.totalAmount = ta; }

    public PaymentMethod getPaymentMethod()                   { return paymentMethod; }
    public void          setPaymentMethod(PaymentMethod pm)   { this.paymentMethod = pm; }

    public BigDecimal    getPaymentReceived()                 { return paymentReceived; }
    public void          setPaymentReceived(BigDecimal pr)    { this.paymentReceived = pr; }

    public BigDecimal    getChangeGiven()                     { return changeGiven; }
    public void          setChangeGiven(BigDecimal cg)        { this.changeGiven = cg; }

    public String        getCardType()                        { return cardType; }
    public void          setCardType(String ct)               { this.cardType = ct; }

    public String        getCardFirstFour()                   { return cardFirstFour; }
    public void          setCardFirstFour(String cf)          { this.cardFirstFour = cf; }

    public String        getCardLastFour()                    { return cardLastFour; }
    public void          setCardLastFour(String cl)           { this.cardLastFour = cl; }

    public String        getCardExpiry()                      { return cardExpiry; }
    public void          setCardExpiry(String ce)             { this.cardExpiry = ce; }

    public String        getInvoiceNumber()                   { return invoiceNumber; }
    public void          setInvoiceNumber(String inv)         { this.invoiceNumber = inv; }

    public List<SaleItem> getItems()                          { return items; }
    public void          setItems(List<SaleItem> items)       { this.items = items; }
    public void          addItem(SaleItem item)               { this.items.add(item); }

    /** Returns true if this sale is to an occasional (non-account) customer */
    public boolean       isOccasionalCustomer()               { return holderId == 0; }

    @Override
    public String toString() {
        return "Sale{id=" + saleId + ", invoice='" + invoiceNumber
               + "', total=£" + totalAmount + ", method=" + paymentMethod + "}";
    }
}
