package lf.linguageflowpoc.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void calculatesPriceWithFormalityAndUrgency() {
        long totalPrice = calculator.calculateTotalPrice(1500, "formal", "rush");
        assertThat(totalPrice).isEqualTo(Math.round(0.8d * 1500 * 1.2d * 1.4d));
    }

    @Test
    void fallsBackToDefaultsForUnknownValues() {
        long totalPrice = calculator.calculateTotalPrice(500, "unknown", "unknown");
        assertThat(totalPrice).isEqualTo(Math.round(0.8d * 500));
    }
}
