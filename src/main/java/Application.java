import exceptions.UnqueuedWorkException;
import work.CelsiusToFahrenheit;
import workers.TemperatureConverter;
import workers.Worker;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * @author KPentaris - 13-Jan-17.
 */
public class Application {

    public static void main(String[] args) throws IOException, UnqueuedWorkException, ExecutionException, InterruptedException {
        Logger log = Logger.getLogger("Application");
        log.info("Async Worker application started");

        Worker<CelsiusToFahrenheit> worker = new TemperatureConverter<>(10, 3);
        worker.startWorker();

        CelsiusToFahrenheit celsiusConversion = new CelsiusToFahrenheit();
        celsiusConversion.setValue(40);
        Future<CelsiusToFahrenheit> promise = worker.enqueueWork(celsiusConversion);
        while (!promise.isDone()) {
            log.info("Awaiting promise");
        }
        log.info("40 celsius is " + promise.get().getValue() + " fahrenheit");

        worker.stopWorker();
        log.info("Async Worker application ended");
    }
}
