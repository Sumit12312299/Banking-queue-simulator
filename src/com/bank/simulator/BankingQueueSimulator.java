package com.bank.simulator;

import com.bank.simulator.model.Customer;
import com.bank.simulator.model.CustomerType;
import com.bank.simulator.model.VipTier;
import com.bank.simulator.service.QueueManager;
import com.bank.simulator.service.Teller;

import java.util.*;

/**
 * Main application for the Banking Queue Simulator.
 * Provides a text-based user interface for manual interactions and an automated simulation mode.
 */
public class BankingQueueSimulator {
    // ANSI Colors for console formatting
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    private final QueueManager queueManager;
    private final List<Teller> manualTellers;
    private final Scanner scanner;

    public BankingQueueSimulator() {
        this.queueManager = new QueueManager();
        this.manualTellers = new ArrayList<>();
        // Initialize two default tellers for manual mode
        this.manualTellers.add(new Teller(1, "Teller A"));
        this.manualTellers.add(new Teller(2, "Teller B"));
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        BankingQueueSimulator simulator = new BankingQueueSimulator();
        simulator.runMenuLoop();
    }

    /**
     * Main interactive menu loop.
     */
    public void runMenuLoop() {
        boolean exit = false;
        while (!exit) {
            printHeader();
            printMenu();
            System.out.print(ANSI_BOLD + "Choose an option: " + ANSI_RESET);
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    addCustomerManually();
                    break;
                case "2":
                    serveCustomerManually();
                    break;
                case "3":
                    viewQueueStatus();
                    break;
                case "4":
                    runAutomatedSimulationMenu();
                    break;
                case "5":
                    printStatisticsReport();
                    break;
                case "6":
                    configureStarvePrevention();
                    break;
                case "7":
                    resetSimulator();
                    break;
                case "8":
                    exit = true;
                    System.out.println(ANSI_CYAN + "\nThank you for using Banking Queue Simulator. Goodbye!" + ANSI_RESET);
                    break;
                default:
                    System.out.println(ANSI_RED + "Invalid option. Please enter a number from 1 to 8." + ANSI_RESET);
                    waitForEnter();
            }
        }
    }

    private void printHeader() {
        System.out.print("\033[H\033[2J"); // Clear screen (ANSI escape code)
        System.out.flush();
        System.out.println(ANSI_CYAN + ANSI_BOLD + "==========================================================" + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + "               BANKING TOKEN & QUEUE SIMULATOR            " + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + "==========================================================" + ANSI_RESET);
        System.out.printf("  Pending: VIP: %s%d%s | Regular: %s%d%s | Total Served: %s%d%s\n",
                ANSI_YELLOW, queueManager.getVipQueueSize(), ANSI_RESET,
                ANSI_GREEN, queueManager.getRegularQueueSize(), ANSI_RESET,
                ANSI_BLUE, queueManager.getServedCustomers().size(), ANSI_RESET);
        System.out.println(ANSI_CYAN + "----------------------------------------------------------" + ANSI_RESET);
    }

    private void printMenu() {
        System.out.println(ANSI_BOLD + "1." + ANSI_RESET + " Issue Token / Add Customer (Manual)");
        System.out.println(ANSI_BOLD + "2." + ANSI_RESET + " Serve Next Customer (Manual Tellers)");
        System.out.println(ANSI_BOLD + "3." + ANSI_RESET + " View Active Queues (Details)");
        System.out.println(ANSI_BOLD + "4." + ANSI_RESET + " Run Real-time Automated Simulation");
        System.out.println(ANSI_BOLD + "5." + ANSI_RESET + " View Performance & Statistics Report");
        System.out.println(ANSI_BOLD + "6." + ANSI_RESET + " Configure Starvation Prevention Policy");
        System.out.println(ANSI_BOLD + "7." + ANSI_RESET + " Reset / Clear All Queues");
        System.out.println(ANSI_BOLD + "8." + ANSI_RESET + " Exit");
        System.out.println(ANSI_CYAN + "----------------------------------------------------------" + ANSI_RESET);
    }

    private void addCustomerManually() {
        System.out.println(ANSI_BOLD + "\n--- ISSUE NEW TOKEN ---" + ANSI_RESET);
        System.out.print("Enter Customer Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            name = "Guest Customer";
        }

        System.out.println("\nSelect Customer Category:");
        System.out.println("1. Regular Customer");
        System.out.println("2. VIP Customer");
        System.out.print("Select (1-2): ");
        String catChoice = scanner.nextLine().trim();

        Customer customer;
        if (catChoice.equals("2")) {
            System.out.println("\nSelect VIP Tier:");
            System.out.println("1. VVIP (Highest Priority)");
            System.out.println("2. VIP (Medium Priority)");
            System.out.println("3. Preferred (Low Priority)");
            System.out.print("Select (1-3): ");
            String tierChoice = scanner.nextLine().trim();

            VipTier tier = VipTier.VIP; // default
            if (tierChoice.equals("1")) {
                tier = VipTier.VVIP;
            } else if (tierChoice.equals("3")) {
                tier = VipTier.PREFERRED;
            }

            String token = queueManager.generateNextToken(CustomerType.VIP);
            customer = new Customer(name, token, tier);
            queueManager.enqueueCustomer(customer);
            System.out.println(ANSI_GREEN + "\nSUCCESS: Token " + token + " issued to VIP (" + tier.name() + ") Customer: " + name + ANSI_RESET);
        } else {
            String token = queueManager.generateNextToken(CustomerType.REGULAR);
            customer = new Customer(name, token);
            queueManager.enqueueCustomer(customer);
            System.out.println(ANSI_GREEN + "\nSUCCESS: Token " + token + " issued to Regular Customer: " + name + ANSI_RESET);
        }
        waitForEnter();
    }

    private void serveCustomerManually() {
        System.out.println(ANSI_BOLD + "\n--- SERVE CUSTOMER ---" + ANSI_RESET);
        System.out.println("Select a Teller counter to handle the service:");
        for (int i = 0; i < manualTellers.size(); i++) {
            System.out.printf("%d. %s\n", i + 1, manualTellers.get(i));
        }
        System.out.print("Select Teller (1-" + manualTellers.size() + "): ");
        String tellerChoice = scanner.nextLine().trim();

        int tellerIdx;
        try {
            tellerIdx = Integer.parseInt(tellerChoice) - 1;
            if (tellerIdx < 0 || tellerIdx >= manualTellers.size()) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.out.println(ANSI_RED + "Invalid Teller Selection." + ANSI_RESET);
            waitForEnter();
            return;
        }

        Teller teller = manualTellers.get(tellerIdx);
        if (teller.isBusy()) {
            System.out.println(ANSI_YELLOW + "Teller is currently busy serving. Finish their current transaction first?" + ANSI_RESET);
            System.out.print("Complete transaction now? (y/n): ");
            String completeChoice = scanner.nextLine().trim().toLowerCase();
            if (completeChoice.equals("y") || completeChoice.equals("yes")) {
                long duration = 1000 + (long)(Math.random() * 2000); // Random service time between 1 and 3s
                Customer finished = teller.getCurrentCustomer();
                teller.finishServing(queueManager, duration);
                System.out.printf("%sCompleted serving %s (%s). Service time: %.2fs. Waiting time: %.2fs.%s\n",
                        ANSI_GREEN, finished.getName(), finished.getTokenNumber(), duration / 1000.0, finished.getWaitTime() / 1000.0, ANSI_RESET);
            }
            waitForEnter();
            return;
        }

        // Teller is free, pull the next customer from queue
        Customer nextCustomer = queueManager.dequeueNextCustomer();
        if (nextCustomer == null) {
            System.out.println(ANSI_YELLOW + "No customers waiting in either the VIP or Regular queue." + ANSI_RESET);
        } else {
            teller.startServing(nextCustomer);
            System.out.printf("%s%s started serving %s (Token: %s, Category: %s). Wait time was: %.2fs.%s\n",
                    ANSI_BLUE, teller.getName(), nextCustomer.getName(), nextCustomer.getTokenNumber(),
                    nextCustomer.getType() == CustomerType.VIP ? nextCustomer.getVipTier().name() : "Regular",
                    nextCustomer.getWaitTime() / 1000.0, ANSI_RESET);
            
            System.out.print("\nPress Enter to complete service immediately, or type 'hold' to keep teller busy: ");
            String response = scanner.nextLine().trim().toLowerCase();
            if (!response.equals("hold")) {
                long duration = 1500 + (long)(Math.random() * 1500); // Simulated service duration
                teller.finishServing(queueManager, duration);
                System.out.printf("%sCompleted serving %s (%s). Service time: %.2fs.%s\n",
                        ANSI_GREEN, nextCustomer.getName(), nextCustomer.getTokenNumber(), duration / 1000.0, ANSI_RESET);
            } else {
                System.out.println(ANSI_YELLOW + "Teller remains busy. You must select option 2 again to finish serving this customer later." + ANSI_RESET);
            }
        }
        waitForEnter();
    }

    private void viewQueueStatus() {
        System.out.println(ANSI_BOLD + "\n--- ACTIVE QUEUES STATUS ---" + ANSI_RESET);

        List<Customer> vipList = queueManager.getVipQueueSnapshot();
        List<Customer> regList = queueManager.getRegularQueueSnapshot();

        System.out.println(ANSI_YELLOW + ANSI_BOLD + "VIP Priority Queue (" + vipList.size() + " customers waiting):" + ANSI_RESET);
        if (vipList.isEmpty()) {
            System.out.println("  [Empty]");
        } else {
            int position = 1;
            for (Customer c : vipList) {
                System.out.printf("  %d. Token: %s | Name: %-15s | Tier: %-10s | Priority Level: %d | Waiting: %.2fs\n",
                        position++, c.getTokenNumber(), c.getName(), c.getVipTier().name(), c.getPriorityLevel(), c.getWaitTime() / 1000.0);
            }
        }

        System.out.println(ANSI_GREEN + ANSI_BOLD + "\nRegular Queue (" + regList.size() + " customers waiting - FIFO):" + ANSI_RESET);
        if (regList.isEmpty()) {
            System.out.println("  [Empty]");
        } else {
            int position = 1;
            for (Customer c : regList) {
                System.out.printf("  %d. Token: %s | Name: %-15s | Waiting: %.2fs\n",
                        position++, c.getTokenNumber(), c.getName(), c.getWaitTime() / 1000.0);
            }
        }

        System.out.println(ANSI_BLUE + ANSI_BOLD + "\nTellers Status:" + ANSI_RESET);
        for (Teller t : manualTellers) {
            System.out.println("  " + t.toString());
        }

        waitForEnter();
    }

    private void runAutomatedSimulationMenu() {
        System.out.println(ANSI_BOLD + "\n--- AUTOMATED REAL-TIME SIMULATION CONFIG ---" + ANSI_RESET);
        System.out.print("Enter number of Tellers (1-5, default 2): ");
        String numTellersStr = scanner.nextLine().trim();
        int numTellers = 2;
        if (!numTellersStr.isEmpty()) {
            try {
                numTellers = Integer.parseInt(numTellersStr);
                if (numTellers < 1 || numTellers > 5) {
                    System.out.println(ANSI_YELLOW + "Out of bounds. Defaulting to 2 tellers." + ANSI_RESET);
                    numTellers = 2;
                }
            } catch (NumberFormatException e) {
                System.out.println(ANSI_YELLOW + "Invalid number. Defaulting to 2 tellers." + ANSI_RESET);
            }
        }

        System.out.print("Enter simulation duration in seconds (10-60, default 20): ");
        String durationStr = scanner.nextLine().trim();
        int duration = 20;
        if (!durationStr.isEmpty()) {
            try {
                duration = Integer.parseInt(durationStr);
                if (duration < 5 || duration > 300) {
                    System.out.println(ANSI_YELLOW + "Out of bounds. Defaulting to 20 seconds." + ANSI_RESET);
                    duration = 20;
                }
            } catch (NumberFormatException e) {
                System.out.println(ANSI_YELLOW + "Invalid number. Defaulting to 20 seconds." + ANSI_RESET);
            }
        }

        System.out.println("\nResetting queues before starting simulation...");
        queueManager.reset();
        for (Teller t : manualTellers) {
            // reset manual tellers
            t.finishServing(queueManager, 0);
        }

        runAutoSimulation(numTellers, duration);
        waitForEnter();
    }

    /**
     * Executes the background simulation using threads.
     */
    private void runAutoSimulation(int numTellers, int durationSeconds) {
        System.out.println("\n" + ANSI_CYAN + ANSI_BOLD + "==========================================================" + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + "            STARTING REAL-TIME BANK SIMULATION            " + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + "==========================================================" + ANSI_RESET);
        System.out.printf(" Tellers active: %d | Runtime: %d seconds\n", numTellers, durationSeconds);
        System.out.println(" Logging live arrivals and service events...\n");

        List<Teller> simulationTellers = new ArrayList<>();
        for (int i = 1; i <= numTellers; i++) {
            simulationTellers.add(new Teller(i, "Sim-Teller " + (char)('A' + i - 1)));
        }

        final boolean[] running = {true};

        // Customer arrival thread
        Thread arrivalThread = new Thread(() -> {
            Random rand = new Random();
            String[] firstNames = {"Sumit", "Amit", "Rahul", "Priya", "Anjali", "Vikram", "Sneha", "Karan", "Pooja", "Rohit", "Deepak", "Neha", "Aarav", "Kabir", "Meera"};
            String[] lastNames = {"Kumar", "Singh", "Sharma", "Verma", "Gupta", "Patel", "Mehta", "Joshi", "Das", "Sen", "Rao", "Reddy"};

            while (running[0]) {
                try {
                    // Random customer arrival every 800ms - 1800ms
                    Thread.sleep(800 + rand.nextInt(1000));
                    if (!running[0]) break;

                    String name = firstNames[rand.nextInt(firstNames.length)] + " " + lastNames[rand.nextInt(lastNames.length)];
                    boolean isVip = rand.nextDouble() < 0.35; // 35% chance of VIP customer

                    if (isVip) {
                        VipTier tier = VipTier.values()[rand.nextInt(VipTier.values().length)];
                        String token = queueManager.generateNextToken(CustomerType.VIP);
                        Customer customer = new Customer(name, token, tier);
                        queueManager.enqueueCustomer(customer);
                        System.out.printf("%s[Arrival] %s (%s) joined the VIP Queue. (VIP Queue: %d, Regular Queue: %d)%s\n",
                                ANSI_YELLOW, token, name, tier.getDisplayName(), queueManager.getVipQueueSize(), queueManager.getRegularQueueSize(), ANSI_RESET);
                    } else {
                        String token = queueManager.generateNextToken(CustomerType.REGULAR);
                        Customer customer = new Customer(name, token);
                        queueManager.enqueueCustomer(customer);
                        System.out.printf("%s[Arrival] %s (Regular - %s) joined standard Queue. (VIP Queue: %d, Regular Queue: %d)%s\n",
                                ANSI_GREEN, token, name, queueManager.getVipQueueSize(), queueManager.getRegularQueueSize(), ANSI_RESET);
                    }
                } catch (InterruptedException e) {
                    // Thread interrupted, exit loop
                    break;
                }
            }
        });

        // Teller service threads
        List<Thread> tellerThreads = new ArrayList<>();
        for (Teller teller : simulationTellers) {
            Thread tThread = new Thread(() -> {
                Random rand = new Random();
                while (running[0]) {
                    Customer customer = null;
                    synchronized (queueManager) {
                        customer = queueManager.dequeueNextCustomer();
                    }

                    if (customer != null) {
                        teller.startServing(customer);
                        System.out.printf("%s[Service Start] %s serving %s (%s) | Wait: %.1fs%s\n",
                                ANSI_BLUE, teller.getName(), customer.getName(), customer.getTokenNumber(),
                                customer.getWaitTime() / 1000.0, ANSI_RESET);

                        try {
                            // Service duration between 1.5 and 3.5 seconds
                            int serviceTimeMs = 1500 + rand.nextInt(2000);
                            Thread.sleep(serviceTimeMs);

                            long waitTime = customer.getWaitTime();
                            teller.finishServing(queueManager, serviceTimeMs);

                            System.out.printf("%s[Service Complete] %s finished serving %s (%s) | Service: %.1fs, Wait: %.1fs%s\n",
                                    ANSI_PURPLE, teller.getName(), customer.getName(), customer.getTokenNumber(),
                                    serviceTimeMs / 1000.0, waitTime / 1000.0, ANSI_RESET);

                        } catch (InterruptedException e) {
                            // Thread interrupted, finish customer and exit loop
                            teller.finishServing(queueManager, 0);
                            break;
                        }
                    } else {
                        try {
                            // Wait a short time before checking the queues again
                            Thread.sleep(150);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            });
            tellerThreads.add(tThread);
        }

        // Start execution
        arrivalThread.start();
        for (Thread t : tellerThreads) {
            t.start();
        }

        // Run duration timer
        int elapsed = 0;
        while (elapsed < durationSeconds && running[0]) {
            try {
                Thread.sleep(1000);
                elapsed++;
            } catch (InterruptedException e) {
                break;
            }
        }

        // Signal threads to stop
        running[0] = false;
        arrivalThread.interrupt();
        for (Thread t : tellerThreads) {
            t.interrupt();
        }

        // Join threads to ensure cleanup before returning to menu
        try {
            arrivalThread.join(500);
            for (Thread t : tellerThreads) {
                t.join(500);
            }
        } catch (InterruptedException e) {
            // Ignored
        }

        System.out.println("\n" + ANSI_CYAN + ANSI_BOLD + "==========================================================" + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + "            SIMULATION TIME ELAPSED: COMPLETE             " + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + "==========================================================" + ANSI_RESET);
    }

    private void printStatisticsReport() {
        System.out.println(ANSI_BOLD + "\n--- BANK PERFORMANCE REPORT ---" + ANSI_RESET);
        Map<String, Object> stats = queueManager.getStatistics();
        if (stats.get("Total Customers Served").equals(0)) {
            System.out.println(ANSI_YELLOW + "No statistics available yet. Serve some customers first!" + ANSI_RESET);
            waitForEnter();
            return;
        }

        System.out.println(ANSI_CYAN + "----------------------------------------------------------" + ANSI_RESET);
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            System.out.printf("  %-35s: %s%s%s\n",
                    entry.getKey(), ANSI_BOLD, entry.getValue(), ANSI_RESET);
        }
        System.out.println(ANSI_CYAN + "----------------------------------------------------------" + ANSI_RESET);

        // List details of last 10 served customers
        List<Customer> served = queueManager.getServedCustomers();
        System.out.println(ANSI_BOLD + "\nLast Served Customers (Audit Trail):" + ANSI_RESET);
        int start = Math.max(0, served.size() - 10);
        for (int i = start; i < served.size(); i++) {
            Customer c = served.get(i);
            System.out.printf("  %d. Token: %s | %-12s | Service Time: %.2fs | Wait Time: %.2fs\n",
                    i + 1, c.getTokenNumber(), c.getName(), c.getServiceDuration() / 1000.0, c.getWaitTime() / 1000.0);
        }

        waitForEnter();
    }

    private void configureStarvePrevention() {
        System.out.println(ANSI_BOLD + "\n--- STARVATION PREVENTION POLICY ---" + ANSI_RESET);
        System.out.println("If many VIP customers arrive, regular customers might never be served.");
        System.out.println("Starvation Prevention ensures that after serving a number of consecutive VIPs,");
        System.out.println("at least one Regular customer is served if they are waiting.");
        System.out.printf("\nCurrent Status: %s\n",
                queueManager.isStarvePreventionEnabled() ? ANSI_GREEN + "ENABLED" + ANSI_RESET : ANSI_RED + "DISABLED" + ANSI_RESET);
        System.out.printf("Current Limit: %d consecutive VIPs\n", queueManager.getMaxConsecutiveVips());

        System.out.print("\nEnable Starvation Prevention? (y/n, default unchanged): ");
        String toggle = scanner.nextLine().trim().toLowerCase();
        if (toggle.equals("y") || toggle.equals("yes")) {
            queueManager.setStarvePreventionEnabled(true);
            System.out.println(ANSI_GREEN + "Starvation Prevention is now ENABLED." + ANSI_RESET);
        } else if (toggle.equals("n") || toggle.equals("no")) {
            queueManager.setStarvePreventionEnabled(false);
            System.out.println(ANSI_RED + "Starvation Prevention is now DISABLED." + ANSI_RESET);
        }

        System.out.printf("Enter max consecutive VIP customers to serve (1-10, default %d): ", queueManager.getMaxConsecutiveVips());
        String limitStr = scanner.nextLine().trim();
        if (!limitStr.isEmpty()) {
            try {
                int limit = Integer.parseInt(limitStr);
                if (limit >= 1 && limit <= 10) {
                    queueManager.setMaxConsecutiveVips(limit);
                    System.out.println(ANSI_GREEN + "Limit updated successfully to " + limit + "." + ANSI_RESET);
                } else {
                    System.out.println(ANSI_RED + "Out of range. Limit not changed." + ANSI_RESET);
                }
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "Invalid input. Limit not changed." + ANSI_RESET);
            }
        }
        waitForEnter();
    }

    private void resetSimulator() {
        System.out.print(ANSI_RED + ANSI_BOLD + "\nWARNING: This will clear all active queues and reset counters. Continue? (y/n): " + ANSI_RESET);
        String choice = scanner.nextLine().trim().toLowerCase();
        if (choice.equals("y") || choice.equals("yes")) {
            queueManager.reset();
            for (Teller t : manualTellers) {
                // reset manual tellers
                t.finishServing(queueManager, 0);
            }
            System.out.println(ANSI_GREEN + "Queues and counters have been reset successfully!" + ANSI_RESET);
        } else {
            System.out.println("Reset cancelled.");
        }
        waitForEnter();
    }

    private void waitForEnter() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
