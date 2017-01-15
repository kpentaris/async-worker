package workers;

import exceptions.UnqueuedWorkException;
import network.RequestTemplate;
import utils.Constants;
import work.TemperatureWork;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * @author KPentaris - 13/1/2017.
 */
public class TemperatureConverter<W extends TemperatureWork<Integer>> implements workers.Worker<W> {

    private final Logger log = Logger.getLogger(TemperatureConverter.class.getName());

    private static final String BASE_URL = "http://www.w3schools.com/xml/tempconvert.asmx/";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private WorkerState state;
    private int maximumQueueSize;
    private int threadPoolSize;
    private ThreadPoolExecutor pool;

    public TemperatureConverter(int maximumQueueSize, int threadPoolSize) {
        this.maximumQueueSize = maximumQueueSize;
        this.threadPoolSize = threadPoolSize;
        this.state = WorkerState.INITIAL;
    }

    @Override
    public void startWorker() {
        state = WorkerState.OPERATIONAL;
        this.pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolSize);
        log.info(String.format("Worker started and is operational with %d threads ready", threadPoolSize));
    }

    @Override
    public void stopWorker() {
        state = WorkerState.STOPPED;
        pool.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    log.severe("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
        }

        if (pool.isTerminated())
            log.info("Worker stopped successfully");
    }

    @Override
    public Future<W> enqueueWork(W work) throws UnqueuedWorkException {
        if (state != WorkerState.OPERATIONAL || pool.isShutdown())
            throw new UnqueuedWorkException("Worker is not operational. Work has been discarded.");

        if (pool.getQueue().size() >= maximumQueueSize)
            throw new UnqueuedWorkException("Maximum queue size reached. Work has been discarded.");

        Callable<W> callable = () -> {
            String conversionType = work.getConversionType();

            RequestTemplate template = getTemplate(conversionType);
            template.addRequestHeader("Content-Length", String.valueOf(work.toString().length() + 1));
            template.addRequestParam(conversionType, String.valueOf(work.getValue()));

            log.info(String.format("Converting %d degrees %s", work.getValue(), conversionType));
            String response = template.performRequest();
            response = trimXMLResponse(response);

            return (W) work.setValue(Integer.valueOf(response));
        };

        return pool.submit(callable);
    }

    private RequestTemplate getTemplate(String conversionType) {
        RequestTemplate template;
        try {
            String url = BASE_URL;
            if (Constants.CELSIUS.equals(conversionType))
                url += Constants.CELSIUS_TO_FAHRENHEIT;
            else if (Constants.FAHRENHEIT.equals(conversionType))
                url += Constants.FAHRENHEIT_TO_CELSIUS;

            template = new RequestTemplate(url, "POST");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        template.addRequestHeader("Content-Type", CONTENT_TYPE);
        return template;
    }

    private String trimXMLResponse(String xml) {
        String response = xml;
        int endOfOpeningXMLTag = response.indexOf(">", response.indexOf(">") + 1);
        int startOfEndingXMLTag = response.indexOf("<", endOfOpeningXMLTag);
        return response.substring(endOfOpeningXMLTag + 1, startOfEndingXMLTag);
    }

    public WorkerState getState() {
        return state;
    }

    public int getMaximumQueueSize() {
        return maximumQueueSize;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public ThreadPoolExecutor getPool() {
        return pool;
    }
}
