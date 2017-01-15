import exceptions.UnqueuedWorkException;
import network.RequestTemplate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import work.CelsiusToFahrenheit;
import work.FahrenheitToCelsius;
import workers.Worker;
import workers.WorkerState;
import workers.implementations.TemperatureConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author KPentaris - 13/1/2017.
 */
@RunWith(value = BlockJUnit4ClassRunner.class)
public class IntegrationTests {

    @Test
    public void conversionServiceResponseTest() throws Exception {
        RequestTemplate template = new RequestTemplate("http://www.w3schools.com/xml/tempconvert.asmx/CelsiusToFahrenheit", "POST");
        template.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        template.addRequestParam("Celsius", "40");

        String response = template.performRequest();
        int endOfOpeningXMLTag = response.indexOf(">", response.indexOf(">") + 1);
        int startOfEndingXMLTag = response.indexOf("<", endOfOpeningXMLTag);
        response = response.substring(endOfOpeningXMLTag + 1, startOfEndingXMLTag);

        Assert.assertEquals(response, "104");
    }

    @Test(expected = UnqueuedWorkException.class)
    public void shouldNotAllowWorkToBeEnqueuedWhenInInitialState() throws UnqueuedWorkException {
        Worker<CelsiusToFahrenheit> worker = new TemperatureConverter<>(10, 3);

        CelsiusToFahrenheit celsiusConversion = new CelsiusToFahrenheit();
        celsiusConversion.setValue(40);
        worker.enqueueWork(celsiusConversion);
    }

    @Test(expected = UnqueuedWorkException.class)
    public void shouldNotAllowWorkToBeEnqueuedWhenInStoppedState() throws UnqueuedWorkException {
        Worker<CelsiusToFahrenheit> worker = new TemperatureConverter<>(10, 3);
        worker.startWorker();

        Future<CelsiusToFahrenheit> promise = worker.enqueueWork((CelsiusToFahrenheit) new CelsiusToFahrenheit().setValue(40));

        Assert.assertNotNull(promise);

        worker.stopWorker();

        worker.enqueueWork((CelsiusToFahrenheit) new CelsiusToFahrenheit().setValue(30));
    }

    @Test
    public void shouldNotAllowWorkToBeEnqueuedWhenQueueIsFull() {
        Worker<CelsiusToFahrenheit> worker = new TemperatureConverter<>(3, 3);
        worker.startWorker();
        for (int index = 0; index < 10; index++) {
            CelsiusToFahrenheit celsiusConversion = new CelsiusToFahrenheit();
            celsiusConversion.setValue(10 * index);
            try {
                Future<CelsiusToFahrenheit> promise = worker.enqueueWork(celsiusConversion);
            } catch (UnqueuedWorkException e) {
                Assert.assertTrue(index >= 6);
                Assert.assertEquals(e.getMessage(), "Maximum queue size reached. Work has been discarded.");
            }
        }
        worker.stopWorker();
    }

    @Test
    public void shouldSuccessfullyConvertFromCelsius40ToFahrenheit104() throws Exception {
        Worker<CelsiusToFahrenheit> worker = new TemperatureConverter<>(3, 3);
        worker.startWorker();

        CelsiusToFahrenheit celsiusConversion = new CelsiusToFahrenheit();
        celsiusConversion.setValue(40);
        Future<CelsiusToFahrenheit> promise = worker.enqueueWork(celsiusConversion);
        Assert.assertEquals(104, promise.get().getValue().longValue());
        worker.stopWorker();
    }

    @Test
    public void shouldSuccessfullyConvertFromFahrenheit104ToCelsius40() throws Exception {
        Worker<FahrenheitToCelsius> worker = new TemperatureConverter<>(3, 3);
        worker.startWorker();

        FahrenheitToCelsius fahrenheitConversion = new FahrenheitToCelsius();
        fahrenheitConversion.setValue(104);
        Future<FahrenheitToCelsius> promise = worker.enqueueWork(fahrenheitConversion);
        Assert.assertEquals(40, promise.get().getValue().longValue());
        worker.stopWorker();
    }

    @Test
    public void shouldNotBlockMainThreadWhileExecutingWork() throws Exception {
        Worker<CelsiusToFahrenheit> worker = new TemperatureConverter<>(3, 3);
        worker.startWorker();

        List<Future<CelsiusToFahrenheit>> promises = new ArrayList<>();

        for (int index = 0; index < 6; index++) {
            CelsiusToFahrenheit celsiusConversion = new CelsiusToFahrenheit();
            celsiusConversion.setValue(10 * index);
            Future<CelsiusToFahrenheit> promise = worker.enqueueWork(celsiusConversion);
            promises.add(promise);
        }

        long millis = 0;
        while (!promises.stream().allMatch(Future::isDone)) {
            Thread.sleep(1);
            millis++;
        }

        Assert.assertTrue(millis > 0);
        System.out.println("Approximate milliseconds for all promises to complete: " + millis);

        promises.forEach(promise -> {
            try {
                System.out.println("Promise value: " + promise.get().getValue());
                Assert.assertTrue(promise.isDone());
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Failed promise");
            }
        });

        worker.stopWorker();
    }

    @Test
    public void shouldPerformUpTo5ConcurrentCallsAndQueueUpTo20UseCase() throws Exception {
        TemperatureConverter<CelsiusToFahrenheit> worker = new TemperatureConverter<>(20, 5);

        Assert.assertTrue(worker.getState() == WorkerState.INITIAL); //Assert that worker starts off in the INITIAL state after creation.

        worker.startWorker();

        Assert.assertTrue(worker.getState() == WorkerState.OPERATIONAL); //Assert that worker correctly transitions to the OPERATION state when started.
        Assert.assertTrue(worker.getPool().getCorePoolSize() == 5); //Assert that there are 5 threads ready to undertake work.
        Assert.assertTrue(worker.getPool().getPoolSize() == 0); //Assert that there are 0 threads working since no work has been submitted.

        for (int index = 0; index < 15; index++) { //Give work units to the worker. Some should end up in its queue.
            CelsiusToFahrenheit celsiusConversion = new CelsiusToFahrenheit();
            celsiusConversion.setValue(10 * index);
            Future<CelsiusToFahrenheit> promise = worker.enqueueWork(celsiusConversion);
        }
        Assert.assertTrue(worker.getPool().getActiveCount() == 5); //Assert that worker's thread pool is working to its fullest.
        Assert.assertTrue(worker.getPool().getQueue().size() > 0); //Assert that some work units are pending to be worked on.
        System.out.println("Queued work units: " + worker.getPool().getQueue().size());
        worker.stopWorker();

        Assert.assertTrue(worker.getPool().getActiveCount() == 0); //Assert that after stopping the worker, there is no work going on.
        Assert.assertTrue(worker.getPool().getTaskCount() == 15); //Assert that after stopping the worker all work has finished.

        worker.startWorker();
        int index;
        for (index = 0; index < 50; index++) { //Attempt to siege worker with lots of work.
            CelsiusToFahrenheit celsiusConversion = new CelsiusToFahrenheit();
            celsiusConversion.setValue(10 * index);
            try {
                Future<CelsiusToFahrenheit> promise = worker.enqueueWork(celsiusConversion);
            } catch (UnqueuedWorkException e) {
                break; //Worker throws exception after too much work has been submitted.
            }
        }
        Assert.assertTrue(index >= 20); //Assert that worker threw exception after at least 20 units of work were submitted (more than its queue size without counting its threads).

        worker.stopWorker();

        Assert.assertTrue(worker.getState() == WorkerState.STOPPED); //Assert that worker has transitioned into the STOPPED state correctly.
        Assert.assertTrue(worker.getPool().getActiveCount() == 0); //Assert that there is no work going on.
        Assert.assertTrue(worker.getPool().getTaskCount() >= 20); //Assert that after stopping the worker all work has finished.
    }

}
