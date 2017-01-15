package work;

import utils.Constants;

/**
 * @author KPentaris - 13/1/2017.
 */
public class FahrenheitToCelsius implements TemperatureWork<Integer> {

    private int value;
    private final String conversionType = Constants.FAHRENHEIT;

    @Override
    public TemperatureWork setValue(Integer value) {
        this.value = value;
        return this;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public String getConversionType() {
        return conversionType;
    }
}
