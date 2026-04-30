package com.example.rartyfinancebase;

import com.google.gson.annotations.SerializedName;

public class BinanceTickerResponse {
    @SerializedName("symbol")
    private String symbol;

    @SerializedName("price")
    private String price;

    public String getSymbol() {
        return symbol;
    }

    public String getPrice() {
        return price;
    }
}