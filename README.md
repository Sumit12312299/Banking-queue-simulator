# Banking Queue Simulator

A comprehensive banking token simulation system showcasing standard **FIFO Queues** (for regular customers) and **Priority Queues** (for VIP customers with different prioritization tiers), employing core **Object-Oriented Programming (OOP)** principles and the **Java Collections Framework**. 

This repository contains two complete implementations:
1. ☕ **Java Console Application**: A multi-threaded CLI application simulating teller counter service and randomized arrivals.
2. 🌐 **React Web Application**: A state-of-the-art, glassmorphic Single Page Application featuring interactive customer enqueuing, animated tellers with progress bars, starvation policy configuration, stats dashboard, and live logs.

---

## 🚀 Key Features

* **Dual-Queue Routing**:
  * A standard FIFO queue (`Queue` / `LinkedList` / Array) for Regular customers.
  * A min-heap-based priority queue (`PriorityQueue` / Array sorting) for VIP customers, supporting multi-level prioritization (VVIP, VIP, and Preferred).
* **Starvation Prevention Policy**: An adjustable scheduling policy preventing VIP customers from infinitely starving Regular customers. After a set number of consecutive VIP services (e.g., 3), the system forces the service of a regular customer.
* **Real-time Automated Simulation**: Spawns background tasks (threads in Java, event loops in React) to generate randomized customer arrivals and service times, displaying logs.
* **Interactive Counters**: Counters show teller status (Idle/Serving), current customer details, and service progress. Supports manual overriding (manual enqueuing and teller dispatching).
* **Auditing Ledger & Stats**: Real-time tracking of:
  * Total served customers (split by VIP and Regular).
  * Average wait time (overall and category-wise).
  * Maximum customer waiting time.
  * Complete audit trail / transaction history of served customers.

---

## 📁 Repository Structure

```
Banking_queue_simulator/
├── README.md               (This documentation file)
├── src/                    (Java Source Code)
│   └── com/bank/simulator/
│       ├── BankingQueueSimulator.java  (Java Main CLI runner)
│       ├── model/
│       │   ├── Customer.java            (Customer token class, implements Comparable)
│       │   ├── CustomerType.java        (Enum for VIP vs REGULAR)
│       │   └── VipTier.java             (Enum for VVIP, VIP, PREFERRED levels)
│       └── service/
│           ├── QueueManager.java        (Manages Queues, enforces policies)
│           └── Teller.java              (Simulates teller service counter)
└── web-app/                (React Web Application)
    ├── src/
    │   ├── App.jsx          (React simulator logic and UI layout)
    │   ├── App.css          (Glassmorphic Design System stylesheet)
    │   ├── main.jsx         (React entry point)
    │   └── index.css        (Global styles)
    ├── index.html           (HTML template with custom typography)
    └── package.json         (Dependencies and build scripts)
```

---

## ☕ Running the Java Console App

Navigate to the project root and execute:

### Step 1: Compile the Code
```bash
javac -d bin src/com/bank/simulator/model/*.java src/com/bank/simulator/service/*.java src/com/bank/simulator/BankingQueueSimulator.java
```

### Step 2: Run the Application
```bash
java -cp bin com.bank.simulator.BankingQueueSimulator
```

---

## 🌐 Running the React Web Application

Navigate to the `web-app` folder and install dependencies, then start the local Vite development server:

### Step 1: Install Dependencies
```bash
cd web-app
npm install
```

### Step 2: Run Local Dev Server
```bash
npm run dev
```
Open your browser and navigate to **`http://localhost:5173/`** to interact with the visual dashboard.

---

## ⚡ Demo Flow & Interactive Walkthrough

### Scenario A: Manual Testing (Web or CLI)
1. Add a Regular customer named `Sumit`.
2. Add a VIP customer (VIP tier) named `Amit`.
3. Add a VIP customer (VVIP tier) named `Karan`.
4. Observe the queue listings. `Karan (VVIP)` is positioned before `Amit (VIP)` due to priority tier ranking, and both are positioned before `Sumit (Regular)` due to customer category.
5. Trigger teller service. Watch `Karan` get served first, then `Amit`, and finally `Sumit`.

### Scenario B: Automated Simulation & Starvation Prevention
1. Turn on **Auto-Arrival & Serve** (Auto-Simulation Mode).
2. Tweak arrival speeds and teller service speeds using the settings sliders.
3. Keep **Starvation Prevention** checked and set to `3`. Note that even under heavy VIP arrival rates, the tellers will periodically serve Regular queue customers to avoid starvation.
4. Uncheck **Starvation Prevention** and observe how the Regular queue halts processing if VIPs keep arriving.
