package com.bank.simulator.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a bank customer/token in the queue system.
 * Implements Comparable to allow priority-based sorting in PriorityQueue.
 */
public class Customer implements Comparable<Customer> {
    private final String name;
    private final String tokenNumber;
    private final CustomerType type;
    private final VipTier vipTier; // Null for Regular customers
    private final int priorityLevel; // Lower number means higher priority
    private final long arrivalTime; // System time in milliseconds
    private long serviceStartTime; // When serving begins
    private long serviceDuration; // Time taken to serve in milliseconds

    /**
     * Constructor for Regular Customer.
     */
    public Customer(String name, String tokenNumber) {
        this.name = name;
        this.tokenNumber = tokenNumber;
        this.type = CustomerType.REGULAR;
        this.vipTier = null;
        this.priorityLevel = Integer.MAX_VALUE; // Lowest possible priority for PQ
        this.arrivalTime = System.currentTimeMillis();
    }

    /**
     * Constructor for VIP Customer.
     */
    public Customer(String name, String tokenNumber, VipTier vipTier) {
        this.name = name;
        this.tokenNumber = tokenNumber;
        this.type = CustomerType.VIP;
        this.vipTier = vipTier;
        this.priorityLevel = vipTier.getLevel();
        this.arrivalTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public CustomerType getType() {
        return type;
    }

    public VipTier getVipTier() {
        return vipTier;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public long getServiceStartTime() {
        return serviceStartTime;
    }

    public void setServiceStartTime(long serviceStartTime) {
        this.serviceStartTime = serviceStartTime;
    }

    public long getServiceDuration() {
        return serviceDuration;
    }

    public void setServiceDuration(long serviceDuration) {
        this.serviceDuration = serviceDuration;
    }

    /**
     * Calculates wait time in milliseconds.
     */
    public long getWaitTime() {
        if (serviceStartTime == 0) {
            return System.currentTimeMillis() - arrivalTime;
        }
        return serviceStartTime - arrivalTime;
    }

    /**
     * Natural ordering for PriorityQueue.
     * VIPs are ordered first by VIP level (VVIP > VIP > PREFERRED).
     * If VIP levels are the same, order by arrival time (FIFO).
     */
    @Override
    public int compareTo(Customer other) {
        if (this.priorityLevel != other.priorityLevel) {
            return Integer.compare(this.priorityLevel, other.priorityLevel);
        }
        return Long.compare(this.arrivalTime, other.arrivalTime);
    }

    @Override
    public String toString() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        String arrivalStr = LocalDateTime.now().format(dtf); // Simplified representation
        if (type == CustomerType.VIP) {
            return String.format("[%s] VIP (%s) - %s (Priority Lvl: %d, Arrived: %s)", 
                    tokenNumber, vipTier.name(), name, priorityLevel, arrivalStr);
        } else {
            return String.format("[%s] Regular - %s (Arrived: %s)", 
                    tokenNumber, name, arrivalStr);
        }
    }
}
