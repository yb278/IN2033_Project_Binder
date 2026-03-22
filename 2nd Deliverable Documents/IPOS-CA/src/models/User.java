package models;

/**
 * User
 *
 * Represents a staff member account in IPOS-CA.
 * Maps to the {@code users} table in the ipos_ca database.
 *
 * Roles:
 *   ADMIN      – full access; can create/remove user accounts (CA-03, CA-04, CA-08)
 *   PHARMACIST – catalogue, sales, stock, orders, customer accounts
 *   MANAGER    – reports, credit limits, templates
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class User {

    /** Role constants matching the database ENUM */
    public enum Role { ADMIN, PHARMACIST, MANAGER }

    private int    userId;
    private String username;
    private String passwordHash;   // never store plain text
    private String firstName;
    private String lastName;
    private Role   role;
    private boolean active;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /** Full constructor (used when loading from DB) */
    public User(int userId, String username, String passwordHash,
                String firstName, String lastName, Role role, boolean active) {
        this.userId       = userId;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.firstName    = firstName;
        this.lastName     = lastName;
        this.role         = role;
        this.active       = active;
    }

    /** Constructor for creating a new user (ID assigned by DB) */
    public User(String username, String passwordHash,
                String firstName, String lastName, Role role) {
        this(0, username, passwordHash, firstName, lastName, role, true);
    }

    // ---------------------------------------------------------------
    // Getters & Setters
    // ---------------------------------------------------------------

    public int     getUserId()       { return userId; }
    public void    setUserId(int id) { this.userId = id; }

    public String  getUsername()              { return username; }
    public void    setUsername(String u)      { this.username = u; }

    public String  getPasswordHash()          { return passwordHash; }
    public void    setPasswordHash(String ph) { this.passwordHash = ph; }

    public String  getFirstName()             { return firstName; }
    public void    setFirstName(String fn)    { this.firstName = fn; }

    public String  getLastName()              { return lastName; }
    public void    setLastName(String ln)     { this.lastName = ln; }

    public String  getFullName()              { return firstName + " " + lastName; }

    public Role    getRole()                  { return role; }
    public void    setRole(Role r)            { this.role = r; }

    public boolean isActive()                 { return active; }
    public void    setActive(boolean a)       { this.active = a; }

    // ---------------------------------------------------------------
    // Role helper methods
    // ---------------------------------------------------------------

    /** Returns true if this user has admin privileges. */
    public boolean isAdmin()       { return role == Role.ADMIN; }

    /** Returns true if this user has pharmacist or higher privileges. */
    public boolean isPharmacist()  { return role == Role.PHARMACIST || role == Role.ADMIN; }

    /** Returns true if this user has manager or admin privileges. */
    public boolean isManager()     { return role == Role.MANAGER || role == Role.ADMIN; }

    @Override
    public String toString() {
        return "User{id=" + userId + ", username='" + username
               + "', name='" + getFullName() + "', role=" + role + ", active=" + active + "}";
    }
}
