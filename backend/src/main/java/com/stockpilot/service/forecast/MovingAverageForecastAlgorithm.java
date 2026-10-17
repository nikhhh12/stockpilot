package com.stockpilot.service.forecast;

import org.springframework.stereotype.Component;

import java.util.List;

@Component("movingAverageForecastAlgorithm")
public class MovingAverageForecastAlgorithm implements ForecastAlgorithm {

    @Override
    public String getAlgorithmName() {
        return "MOVING_AVERAGE";
    }

    @Override
    public Double calculateForecast(List<Double> historicalDailyDemands, int forecastDays) {
        if (historicalDailyDemands == null || historicalDailyDemands.isEmpty() || forecastDays <= 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (Double demand : historicalDailyDemands) {
            sum += (demand != null ? Math.max(0.0, demand) : 0.0);
        }

        double avgDaily = sum / historicalDailyDemands.size();
        double result = Math.max(0.0, avgDaily * forecastDays);
        return Math.round(result * 100.0) / 100.0;
    }
}
