package models;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Order
 *
 * Represents an order the pharmacy places with InfoPharma (IPOS-SA).
 * Maps to the {@code orders_to_sa} table.
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class Order {

    private int        orderId;
    private String     iposSaOrderRef;   // reference returned by IPOS-SA
    private int        placedBy;         // user_id of the pharmacist who placed it
    private Timestamp  orderDate;
    private BigDecimal totalAmount;
    private String     status;           // PENDING/ACCEPTED/PROCESSING/DISPATCHED/DELIVERED
    private Timestamp  dispatchDate;
    private Timestamp  deliveryDate;
    private String     courier;
    private String     courierRef;
    private Date       expectedDelivery;
    private String     paymentStatus;    // PENDING/PAID
    private Timestamp  paymentDate;

    private List<OrderItem> items = new ArrayList<>();

    // ---------------------------------------------------------------
    // Full constructor (loading from DB)
    // ---------------------------------------------------------------
    public Order(int orderId, String iposSaOrderRef, int placedBy,
                 Timestamp orderDate, BigDecimal totalAmount, String status,
                 Timestamp dispatchDate, Timestamp deliveryDate,
                 String courier, String courierRef, Date expectedDelivery,
                 String paymentStatus, Timestamp paymentDate) {
        this.orderId         = orderId;
        this.iposSaOrderRef  = iposSaOrderRef;
        this.placedBy        = placedBy;
        this.orderDate       = orderDate;
        this.totalAmount     = totalAmount;
        this.status          = status;
        this.dispatchDate    = dispatchDate;
        this.deliveryDate    = deliveryDate;
        this.courier         = courier;
        this.courierRef      = courierRef;
        this.expectedDelivery = expectedDelivery;
        this.paymentStatus   = paymentStatus;
        this.paymentDate     = paymentDate;
    }

    /** Minimal constructor for creating a new order */
    public Order(int placedBy, BigDecimal totalAmount) {
        this.placedBy    = placedBy;
        this.totalAmount = totalAmount;
        this.status      = "PENDING";
        this.paymentStatus = "PENDING";
        this.orderDate   = new Timestamp(System.currentTimeMillis());
    }

    // ---------------------------------------------------------------
    // Getters & Setters
    // ---------------------------------------------------------------
    public int        getOrderId()                         { return orderId; }
    public void       setOrderId(int id)                   { this.orderId = id; }
    public String     getIposSaOrderRef()                  { return iposSaOrderRef; }
    public void       setIposSaOrderRef(String ref)        { this.iposSaOrderRef = ref; }
    public int        getPlacedBy()                        { return placedBy; }
    public Timestamp  getOrderDate()                       { return orderDate; }
    public BigDecimal getTotalAmount()                     { return totalAmount; }
    public void       setTotalAmount(BigDecimal t)         { this.totalAmount = t; }
    public String     getStatus()                          { return status; }
    public void       setStatus(String s)                  { this.status = s; }
    public Timestamp  getDispatchDate()                    { return dispatchDate; }
    public Timestamp  getDeliveryDate()                    { return deliveryDate; }
    public String     getCourier()                         { return courier; }
    public String     getCourierRef()                      { return courierRef; }
    public Date       getExpectedDelivery()                { return expectedDelivery; }
    public String     getPaymentStatus()                   { return paymentStatus; }
    public void       setPaymentStatus(String ps)          { this.paymentStatus = ps; }
    public Timestamp  getPaymentDate()                     { return paymentDate; }
    public List<OrderItem> getItems()                      { return items; }
    public void       setItems(List<OrderItem> items)      { this.items = items; }
    public void       addItem(OrderItem item)              { this.items.add(item); }
}
