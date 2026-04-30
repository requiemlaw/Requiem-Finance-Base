package com.example.rartyfinancebase;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface YahooApi {
    @GET("v8/finance/chart/{symbol}?includePrePost=true")
    Call<YahooFinanceResponse> getChartData(
            @Path("symbol") String symbol,
            @Query("interval") String interval,
            @Query("range") String range
    );
}