package com.stockpilot.service.forecast;

import java.util.List;

public interface ForecastAlgorithm {
    String getAlgorithmName();
    Double calculateForecast(List<Double> historicalDailyDemands, int forecastDays);
}
