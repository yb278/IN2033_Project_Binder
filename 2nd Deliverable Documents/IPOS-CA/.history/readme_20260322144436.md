# IPOS-CA — Setup Guide
**IN2033 Team Project 2025-26 | Team B | Client Application**

---

## What you need to install

| Software | Version | Download |
|---|---|---|
| Java JDK | 11 or higher | https://adoptium.net |
| IntelliJ IDEA | Community (free) | https://www.jetbrains.com/idea |
| MySQL Server | 8.0+ | https://dev.mysql.com/downloads/installer |
| MySQL Workbench | Any | Comes with MySQL Installer |
| MySQL Connector/J | 8.0+ | Included in the repo |

---

## Step 1 — Install MySQL

1. Run the **MySQL Installer** from Oracle
2. Select **Developer Default** and install
3. During setup, set a **root password** — write it down, you'll need it
4. Make sure **MySQL Server** and **MySQL Workbench** are both installed
5. On Windows, MySQL runs as a service automatically after install

**Verify MySQL is running:**
```
# Windows
net start mysql80

```

---

## Step 2 — Set up the database

Open a terminal and log into MySQL:
```
mysql -u root -p
```
Enter your root password when prompted.

Then run both SQL files **in order**:
```sql
source /root/back-end-sql/ipos_ca_schema.sql
source /root/back-end-sql/ipos_ca_test_data.sql
```
> On Windows use forward slashes: `source C:/Users/YourName/Downloads/IPOS_CA/sql/ipos_ca_schema.sql`

Verify it worked:
```sql
USE ipos_ca;
SHOW TABLES;
```

You should see 12 tables listed. Then check data loaded:
```sql
SELECT username, role FROM users;
```
Should show: admin, jfaith, apetite, bsmith.

---

## Step 3 — Open the project in IntelliJ

1. Open IntelliJ IDEA
2. Click **Open** (not New Project)
3. Navigate to the `IPOS_CA` folder and select it
4. IntelliJ will open it — if it asks about the project SDK, select your installed JDK 11+

---

## Step 4 — Set up the project structure

IntelliJ needs to know which folder is your source root.

1. Right-click the `src` folder in the Project panel
2. Select **Mark Directory as → Sources Root**
3. The `src` folder should turn blue

---

## Step 5 — Add the MySQL JDBC driver

1. Use `mysql-connector-j-9.5.0jar` from the folder in root
2. In IntelliJ: **File → Project Structure → Libraries**
3. Click the **+** button → **Java**
4. Navigate to and select the `mysql-connector-j-9.5.0jar` file
5. Click **OK** → **Apply**

---

## Step 6 — Update the database password

Open `src/database/DatabaseConnection.java` and find this line:

```java
private static final String DB_PASSWORD = "your_password";
```

Change `your_password` to your MySQL root password. For example:
```java
private static final String DB_PASSWORD = "admin123";
```

If your MySQL has **no password**, leave it as empty quotes:
```java
private static final String DB_PASSWORD = "";
```

---

## Step 7 — Set the main class and run

1. In IntelliJ, open `src/Main.java`
2. Click the green **Run** arrow next to `public static void main`
3. Or go to **Run → Edit Configurations → + → Application**
   - Set **Main class** to `Main`
   - Click **OK** then press the green play button

The login screen should appear.

---

## Step 8 — Log in

Use any of these test accounts:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Admin — full access |
| `jfaith` | `pharma345` | Pharmacist |
| `apetite` | `manager123` | Manager |
| `bsmith` | `pharma123` | Pharmacist |

---

## Project structure reference

```
IPOS_CA/
│
├── back-end-sql/
│   ├── ipos_ca_schema.sql      ← Run first — creates all 12 tables
│   └── ipos_ca_test_data.sql   ← Run second — loads test data
│
└── src/
    ├── Main.java               ← Entry point — run this
    │
    ├── database/
    │   └── DatabaseConnection.java   ← Update DB_PASSWORD here
    │
    ├── models/                 ← Data objects (no SQL)
    │   ├── User.java
    │   ├── AccountHolder.java
    │   ├── StockItem.java
    │   ├── Sale.java
    │   ├── SaleItem.java
    │   ├── Order.java
    │   └── OrderItem.java
    │
    ├── dao/                    ← All database operations
    │   ├── UserDAO.java
    │   ├── AccountHolderDAO.java
    │   ├── StockDAO.java
    │   ├── SalesDAO.java
    │   ├── OrderDAO.java
    │   ├── ReminderDAO.java
    │   ├── ReportDAO.java
    │   ├── DiscountPlanDAO.java
    │   ├── TemplateDAO.java
    │   └── MerchantSettingsDAO.java
    │
    ├── service/
    │   └── AccountStatusService.java  ← Auto account status logic
    │
    └── gui/                    ← All Swing screens
        ├── LoginFrame.java
        ├── MainFrame.java
        ├── AccountHoldersPanel.java
        ├── StockPanel.java
        ├── PointOfSalePanel.java
        └── AllPanels.java      ← Orders, Statements, Reports,
                                   UserManagement, Templates,
                                   Settings, CreditDiscounts
```

---

## Common errors and fixes

**"Can't connect to MySQL server on localhost:3306"**
→ MySQL is not running. Run `net start mysql80` (Windows) or `sudo systemctl start mysql` (Mac/Linux)

**"Access denied for user root"**
→ Wrong password in `DatabaseConnection.java`. Double-check `DB_PASSWORD`.

**"MySQL JDBC driver not found"**
→ The `mysql-connector-j.jar` has not been added to the project libraries. Repeat Step 5.

**"Table ipos_ca.users doesn't exist"**
→ The schema SQL hasn't been run. Repeat Step 2.

**Compilation errors about classes not found**
→ Make sure `src` is marked as Sources Root (Step 4) and all packages are correct.

---

## Dependencies

The only external dependency is the MySQL JDBC driver:

| File | Purpose |
|---|---|
| `mysql-connector-j-9.5.0jar` | Allows Java to connect to MySQL |

Everything else uses standard Java SE libraries (`javax.swing`, `java.awt`, `java.sql`).

---

*IN2033 Team Project 2025-26 · City, St George's University of London · Team B (IPOS-CA)*