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
        if (historicalDailyDemands == null || historicalDailyDemands.isEmpty() || forecastDays <= 0) {
            return 0.0;
        }

        double alpha = DEFAULT_ALPHA;
        Double first = historicalDailyDemands.get(0);
        double smoothed = first != null ? first : 0.0;

        for (int i = 1; i < historicalDailyDemands.size(); i++) {
            double current = historicalDailyDemands.get(i) != null ? historicalDailyDemands.get(i) : 0.0;
            smoothed = alpha * current + (1.0 - alpha) * smoothed;
        }

        double result = Math.max(0.0, smoothed * forecastDays);
        return Math.round(result * 100.0) / 100.0;
    }
}
