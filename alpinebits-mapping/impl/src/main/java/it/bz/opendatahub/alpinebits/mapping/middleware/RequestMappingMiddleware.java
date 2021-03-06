/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package it.bz.opendatahub.alpinebits.mapping.middleware;

import it.bz.opendatahub.alpinebits.middleware.Context;
import it.bz.opendatahub.alpinebits.middleware.Key;
import it.bz.opendatahub.alpinebits.middleware.Middleware;
import it.bz.opendatahub.alpinebits.middleware.MiddlewareChain;

import java.util.function.BiFunction;

/**
 * This {@link Middleware} applies a given mapping function onto
 * a given source object, read from {@link Context}, and writes
 * the result back into the context.
 * <p>
 * The mapping is performed before the next middleware is invoked.
 * <p>
 * The context keys for the source and target objects and the
 * mapping function are specified in the constructor.
 *
 * @param <S> the source object type
 * @param <T> the target object type
 */
public class RequestMappingMiddleware<S, T> implements Middleware {

    private final Key<S> sourceKey;
    private final Key<T> targetKey;
    private final BiFunction<S, Context, T> mappingFunction;

    public RequestMappingMiddleware(Key<S> sourceKey, Key<T> targetKey, BiFunction<S, Context, T> mappingFunction) {
        this.sourceKey = sourceKey;
        this.targetKey = targetKey;
        this.mappingFunction = mappingFunction;
    }

    @Override
    public void handleContext(Context ctx, MiddlewareChain chain) {
        S sourceData = ctx.getOrThrow(this.sourceKey);
        T targetData = this.mappingFunction.apply(sourceData, ctx);
        ctx.put(this.targetKey, targetData);

        chain.next();
    }

}
