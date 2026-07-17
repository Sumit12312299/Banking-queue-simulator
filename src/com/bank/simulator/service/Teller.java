package com.bank.simulator.service;

import com.bank.simulator.model.Customer;

/**
 * Represents a bank teller serving customers.
 */
public class Teller {
    private final int id;
    private final String name;
    private boolean busy;
    private Customer currentCustomer;
    private int customersServedCount;

    public Teller(int id, String name) {
        this.id = id;
        this.name = name;
        this.busy = false;
        this.currentCustomer = null;
        this.customersServedCount = 0;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public synchronized boolean isBusy() {
        return busy;
    }

    public synchronized Customer getCurrentCustomer() {
        return currentCustomer;
    }

    public synchronized int getCustomersServedCount() {
        return customersServedCount;
    }

    /**
     * Start serving a customer.
     */
    public synchronized void startServing(Customer customer) {
        this.busy = true;
        this.currentCustomer = customer;
    }

    /**
     * Finish serving the current customer and record statistics.
     */
    public synchronized void finishServing(QueueManager queueManager, long serviceDurationMs) {
        if (currentCustomer != null) {
            queueManager.recordServedCustomer(currentCustomer, serviceDurationMs);
            customersServedCount++;
        }
        this.busy = false;
        this.currentCustomer = null;
    }

    @Override
    public String toString() {
        if (busy && currentCustomer != null) {
            return String.format("Teller %d (%s): SERVING %s (Token: %s)", 
                    id, name, currentCustomer.getName(), currentCustomer.getTokenNumber());
        } else {
            return String.format("Teller %d (%s): IDLE", id, name);
        }
    }
}
