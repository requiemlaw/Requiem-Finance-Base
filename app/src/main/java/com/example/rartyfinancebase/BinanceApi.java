package com.example.rartyfinancebase;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BinanceApi {
    @GET("api/v3/ticker/price")
    Call<BinanceTickerResponse> getPrice(@Query("symbol") String symbol);

    @GET("api/v3/klines")
    Call<List<List<Object>>> getKlines(
            @Query("symbol") String symbol,
            @Query("interval") String interval,
            @Query("limit") int limit
    );
}