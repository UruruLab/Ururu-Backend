package com.ururulab.ururu.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.recommendation")
public class AiRecommendationProperties {

    private int defaultTopK;
    private double defaultMinSimilarity;
    private boolean defaultUsePriceFilter;

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(final int defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public double getDefaultMinSimilarity() {
        return defaultMinSimilarity;
    }

    public void setDefaultMinSimilarity(final double defaultMinSimilarity) {
        this.defaultMinSimilarity = defaultMinSimilarity;
    }

    public boolean isDefaultUsePriceFilter() {
        return defaultUsePriceFilter;
    }

    public void setDefaultUsePriceFilter(final boolean defaultUsePriceFilter) {
        this.defaultUsePriceFilter = defaultUsePriceFilter;
    }
}
