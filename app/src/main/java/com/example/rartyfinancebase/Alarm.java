package com.example.rartyfinancebase;

public class Alarm {
    public String symbol;
    public String displayName;
    public double targetPrice;

    public Alarm(String symbol, String displayName, double targetPrice) {
        this.symbol = symbol;
        this.displayName = displayName;
        this.targetPrice = targetPrice;
    }
}