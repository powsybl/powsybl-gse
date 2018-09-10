/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
                .doOnEvent((response, throwable) -> LOGGER.info("Loading tile {} (status={})", url, response.getStatusCode()))
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
