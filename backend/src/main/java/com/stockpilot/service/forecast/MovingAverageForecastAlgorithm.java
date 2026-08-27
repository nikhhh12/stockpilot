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
        if (historicalDailyDemands == null || historicalDailyDemands.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (Double demand : historicalDailyDemands) {
            sum += (demand != null ? demand : 0.0);
        }

        double avgDaily = sum / historicalDailyDemands.size();
        return Math.round(avgDaily * forecastDays * 100.0) / 100.0;
    }
}
