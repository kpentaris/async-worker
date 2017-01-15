package work;

/**
 * @author KPentaris - 13/1/2017.
 */
public interface TemperatureWork<T> {

    TemperatureWork setValue(T value);

    T getValue();

    String getConversionType();

}
