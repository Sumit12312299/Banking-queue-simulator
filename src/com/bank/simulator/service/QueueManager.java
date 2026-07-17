package com.bank.simulator.service;

import com.bank.simulator.model.Customer;
import com.bank.simulator.model.CustomerType;

import java.util.*;

/**
 * Manages the double queue system: a PriorityQueue for VIP customers
 * and a standard Queue (LinkedList) for regular customers.
 */
public class QueueManager {
    private final PriorityQueue<Customer> vipQueue;
    private final Queue<Customer> regularQueue;
    private final List<Customer> servedCustomers;

    private int vipTokenCounter = 0;
    private int regularTokenCounter = 0;

    // Policy parameters
    private boolean starvePreventionEnabled = true;
    private int maxConsecutiveVips = 3;
    private int consecutiveVipsServed = 0;

    public QueueManager() {
        this.vipQueue = new PriorityQueue<>();
        this.regularQueue = new LinkedList<>();
        this.servedCustomers = new ArrayList<>();
    }

    /**
     * Toggles the starvation prevention policy.
     */
    public void setStarvePreventionEnabled(boolean enabled) {
        this.starvePreventionEnabled = enabled;
    }

    public boolean isStarvePreventionEnabled() {
        return starvePreventionEnabled;
    }

    public int getMaxConsecutiveVips() {
        return maxConsecutiveVips;
    }

    public void setMaxConsecutiveVips(int maxConsecutiveVips) {
        this.maxConsecutiveVips = maxConsecutiveVips;
    }

    /**
     * Generates a new unique token number based on customer type.
     */
    public synchronized String generateNextToken(CustomerType type) {
        if (type == CustomerType.VIP) {
            vipTokenCounter++;
            return String.format("VIP-%03d", vipTokenCounter);
        } else {
            regularTokenCounter++;
            return String.format("REG-%03d", regularTokenCounter);
        }
    }

    /**
     * Enqueues a customer into the appropriate queue.
     */
    public synchronized void enqueueCustomer(Customer customer) {
        if (customer.getType() == CustomerType.VIP) {
            vipQueue.add(customer);
        } else {
            regularQueue.add(customer);
        }
    }

    /**
     * Dequeues the next customer to be served based on the bank's queue policy.
     * Policy: VIP always first, unless Starvation Prevention triggers (e.g., after serving 3 VIPs).
     */
    public synchronized Customer dequeueNextCustomer() {
        if (vipQueue.isEmpty() && regularQueue.isEmpty()) {
            return null;
        }

        Customer nextCustomer = null;

        // If VIPs are waiting and (starvation prevention is off OR we haven't hit the consecutive VIP limit OR regular is empty)
        if (!vipQueue.isEmpty()) {
            boolean serveVip = true;

            if (starvePreventionEnabled && !regularQueue.isEmpty() && consecutiveVipsServed >= maxConsecutiveVips) {
                serveVip = false; // Force serving a regular customer to prevent starvation
            }

            if (serveVip) {
                nextCustomer = vipQueue.poll();
                consecutiveVipsServed++;
            } else {
                nextCustomer = regularQueue.poll();
                consecutiveVipsServed = 0; // Reset counter since we served a regular customer
            }
        } else {
            // VIP queue is empty, so we must serve from the regular queue
            nextCustomer = regularQueue.poll();
            consecutiveVipsServed = 0;
        }

        if (nextCustomer != null) {
            nextCustomer.setServiceStartTime(System.currentTimeMillis());
        }

        return nextCustomer;
    }

    /**
     * Records a served customer's details and duration.
     */
    public synchronized void recordServedCustomer(Customer customer, long serviceDurationMs) {
        customer.setServiceDuration(serviceDurationMs);
        servedCustomers.add(customer);
    }

    // Getters for queue states
    public synchronized List<Customer> getVipQueueSnapshot() {
        // Return a sorted list based on PriorityQueue ordering
        List<Customer> list = new ArrayList<>(vipQueue);
        Collections.sort(list);
        return list;
    }

    public synchronized List<Customer> getRegularQueueSnapshot() {
        return new ArrayList<>(regularQueue);
    }

    public synchronized int getVipQueueSize() {
        return vipQueue.size();
    }

    public synchronized int getRegularQueueSize() {
        return regularQueue.size();
    }

    public synchronized List<Customer> getServedCustomers() {
        return new ArrayList<>(servedCustomers);
    }

    /**
     * Reset the queues and counters.
     */
    public synchronized void reset() {
        vipQueue.clear();
        regularQueue.clear();
        servedCustomers.clear();
        vipTokenCounter = 0;
        regularTokenCounter = 0;
        consecutiveVipsServed = 0;
    }

    /**
     * Calculates current service statistics.
     */
    public synchronized Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        int totalServed = servedCustomers.size();
        int vipServed = 0;
        int regularServed = 0;
        long totalWaitTimeMs = 0;
        long maxWaitTimeMs = 0;
        long vipWaitTimeMs = 0;
        long regularWaitTimeMs = 0;

        for (Customer c : servedCustomers) {
            long wait = c.getWaitTime();
            totalWaitTimeMs += wait;
            if (wait > maxWaitTimeMs) {
                maxWaitTimeMs = wait;
            }

            if (c.getType() == CustomerType.VIP) {
                vipServed++;
                vipWaitTimeMs += wait;
            } else {
                regularServed++;
                regularWaitTimeMs += wait;
            }
        }

        double avgWaitSec = totalServed == 0 ? 0.0 : (totalWaitTimeMs / 1000.0) / totalServed;
        double avgVipWaitSec = vipServed == 0 ? 0.0 : (vipWaitTimeMs / 1000.0) / vipServed;
        double avgRegularWaitSec = regularServed == 0 ? 0.0 : (regularWaitTimeMs / 1000.0) / regularServed;

        stats.put("Total Customers Served", totalServed);
        stats.put("VIPs Served", vipServed);
        stats.put("Regulars Served", regularServed);
        stats.put("Average Wait Time (seconds)", String.format("%.2fs", avgWaitSec));
        stats.put("Average VIP Wait Time (seconds)", String.format("%.2fs", avgVipWaitSec));
        stats.put("Average Regular Wait Time (seconds)", String.format("%.2fs", avgRegularWaitSec));
        stats.put("Max Wait Time (seconds)", String.format("%.2fs", maxWaitTimeMs / 1000.0));
        stats.put("Pending VIPs", vipQueue.size());
        stats.put("Pending Regulars", regularQueue.size());
        stats.put("Starvation Prevention Status", starvePreventionEnabled ? "ENABLED (Max " + maxConsecutiveVips + " consecutive VIPs)" : "DISABLED");

        return stats;
    }
}
