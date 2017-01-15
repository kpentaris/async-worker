package workers;

import exceptions.UnqueuedWorkException;

import java.util.concurrent.Future;

/**
 * @author KPentaris - 13/1/2017.
 */
public interface Worker<W> {

    void startWorker();

    void stopWorker();

    Future<W> enqueueWork(W work) throws UnqueuedWorkException;

}
