package com.powsybl.gse.map;

import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.asynchttpclient.extras.rxjava2.RxHttpClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.asynchttpclient.Dsl.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class TileHttpClient implements AutoCloseable {

    private final TileCache cache;

    private final AsyncHttpClient asyncHttpClient;

    private final RxHttpClient rxHttpClient;

    private final ExecutorService executorService;

    private final Scheduler scheduler;

    public TileHttpClient(TileCache cache) {
        this.cache = Objects.requireNonNull(cache);
        asyncHttpClient = asyncHttpClient(config()
                .setMaxConnections(500)
                .setMaxConnectionsPerHost(200)
                .setPooledConnectionIdleTimeout(100)
                .setConnectionTtl(500));
        rxHttpClient = RxHttpClient.create(asyncHttpClient);
        executorService = Executors.newFixedThreadPool(2);
        scheduler = Schedulers.from(executorService);
    }

    Maybe<Response> request(Tile tile) {
        String url = tile.getUrl();
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
