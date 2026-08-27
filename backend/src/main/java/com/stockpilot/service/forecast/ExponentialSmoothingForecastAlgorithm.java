package com.stockpilot.service.forecast;

import org.springframework.stereotype.Component;

import java.util.List;

@Component("exponentialSmoothingForecastAlgorithm")
public class ExponentialSmoothingForecastAlgorithm implements ForecastAlgorithm {

    private static final double DEFAULT_ALPHA = 0.3;

    @Override
    public String getAlgorithmName() {
        return "EXPONENTIAL_SMOOTHING";
    }

    @Override
    public Double calculateForecast(List<Double> historicalDailyDemands, int forecastDays) {
        if (historicalDailyDemands == null || historicalDailyDemands.isEmpty()) {
            return 0.0;
        }

        double alpha = DEFAULT_ALPHA;
        double smoothed = historicalDailyDemands.get(0);

        for (int i = 1; i < historicalDailyDemands.size(); i++) {
            double current = historicalDailyDemands.get(i) != null ? historicalDailyDemands.get(i) : 0.0;
            smoothed = alpha * current + (1.0 - alpha) * smoothed;
        }

        return Math.round(smoothed * forecastDays * 100.0) / 100.0;
    }
}
