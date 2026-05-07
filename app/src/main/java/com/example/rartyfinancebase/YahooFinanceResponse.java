package com.example.rartyfinancebase;

import java.util.List;

public class YahooFinanceResponse {
    public Chart chart;

    public static class Chart {
        public List<Result> result;
    }

    public static class Result {
        public Meta meta;
        public Indicators indicators;
        public List<Long> timestamp;
    }

    public static class Meta {
        public double regularMarketPrice;
        public double chartPreviousClose;
        public double previousClose;
        // İSTATİSTİKLER İÇİN EKLENENLER:
        public double regularMarketDayHigh;
        public double regularMarketDayLow;
        public double fiftyTwoWeekHigh;
        public double fiftyTwoWeekLow;
        public long regularMarketVolume;
    }

    public static class Indicators {
        public List<Quote> quote;
    }

    // İŞTE SİHRİN GERÇEKLEŞTİĞİ YER (MUM GRAFİĞİ VERİLERİ)
    public static class Quote {
        public List<Double> open;
        public List<Double> high;
        public List<Double> low;
        public List<Double> close;
    }
}