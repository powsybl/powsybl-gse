package com.powsybl.gse.map;

import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.asynchttpclient.extras.rxjava2.RxHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.asynchttpclient.Dsl.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class TileHttpClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TileHttpClient.class);

    private final AsyncHttpClient asyncHttpClient;

    private final RxHttpClient rxHttpClient;

    private final ExecutorService executorService;

    private final Scheduler scheduler;

    public TileHttpClient() {
        asyncHttpClient = asyncHttpClient(config()
                .setMaxConnections(500)
                .setMaxConnectionsPerHost(200)
                .setPooledConnectionIdleTimeout(100)
                .setConnectionTtl(500));
        rxHttpClient = RxHttpClient.create(asyncHttpClient);
        executorService = Executors.newFixedThreadPool(2);
        scheduler = Schedulers.from(executorService);
    }

    Maybe<Response> request(TilePoint tilePoint) {
        String url = tilePoint.getUrl();
        LOGGER.info("Loading tile {}", url);
        Request request = get(url)
                .addHeader("User-Agent", "powsybl")
                .build();
        return rxHttpClient.prepare(request)
                .subscribeOn(scheduler);
    }

    @Override
    public void close() {
        try {
            asyncHttpClient.close();
            executorService.shutdown();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
