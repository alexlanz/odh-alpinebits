/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package it.idm.alpinebits.http.middleware;

import it.idm.alpinebits.http.GzipUnsupportedException;
import it.idm.alpinebits.http.HttpContextKey;
import it.idm.alpinebits.middleware.Context;
import it.idm.alpinebits.middleware.Middleware;
import it.idm.alpinebits.middleware.RequiredContextKeyMissingException;
import it.idm.alpinebits.middleware.impl.SimpleContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;

/**
 * Tests cases for {@link GzipUnsupportedMiddleware} class.
 */
public class GzipUnsupportedMiddlewareTest {

    @DataProvider(name = "contentEncoding")
    public static Object[][] contentEncoding() {
        return new Object[][]{
                {null},
                {""},
                {"some value"},
        };
    }

    @Test(expectedExceptions = RequiredContextKeyMissingException.class)
    public void testHandleContext_RequestIsNull() {
        Middleware middleware = new AlpineBitsClientProtocolMiddleware();

        Context ctx = new SimpleContext();
        middleware.handleContext(ctx, null);
    }

    @Test(expectedExceptions = GzipUnsupportedException.class)
    public void testHandleContext_ContentEncodingIsGzip() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(GzipUnsupportedMiddleware.CONTENT_ENCODING_HEADER))
                .thenReturn(GzipUnsupportedMiddleware.GZIP);

        Context ctx = new SimpleContext();
        ctx.set(HttpContextKey.HTTP_REQUEST, request);

        Middleware middleware = new GzipUnsupportedMiddleware();
        middleware.handleContext(ctx, null);
    }

    @Test(dataProvider = "contentEncoding")
    public void testHandleContext_ContentEncodingIsNotGzip(String contentEncoding) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(GzipUnsupportedMiddleware.CONTENT_ENCODING_HEADER))
                .thenReturn(contentEncoding);

        Context ctx = new SimpleContext();
        ctx.set(HttpContextKey.HTTP_REQUEST, request);

        Middleware middleware = new GzipUnsupportedMiddleware();
        middleware.handleContext(ctx, () -> {});

        assertFalse(ctx.contains(GzipUnsupportedMiddleware.CONTENT_ENCODING_HEADER));
    }


}