package com.powsybl.gse.map;

import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.asynchttpclient.extras.rxjava2.RxHttpClient;
import org.asynchttpclient.filter.ThrottleRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.asynchttpclient.Dsl.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class TileHttpClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TileHttpClient.class);

    private final AsyncHttpClient asyncHttpClient;

    private final RxHttpClient rxHttpClient;

    public TileHttpClient() {
        asyncHttpClient = asyncHttpClient(config()
                .addRequestFilter(new ThrottleRequestFilter(2)));
        rxHttpClient = RxHttpClient.create(asyncHttpClient);
    }

    Maybe<Response> request(Tile tile) {
        String url = tile.getUrl();
        Request request = get(url)
                .addHeader("User-Agent", "powsybl")
                .build();
        return rxHttpClient.prepare(request)
                .doOnSubscribe(disposable -> LOGGER.info("Loading tile {}", url))
                .subscribeOn(Schedulers.computation());
    }

    @Override
    public void close() {
        try {
            asyncHttpClient.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
