package de.fungate.translate.core.services;

import fj.data.Either;

import java.io.InputStream;


/**
 * Implementing classes provide means to fetch content from a URL. The name "Curler" thereby resembles the popular
 * Unix cURL tool.
 */
public interface Curler {

    /**
     * Issues a get request to url, thereby respecting a fixed timeout of one second.
     * @param url to where to issue the get request.
     * @return either a String with the resulting file content at that url or an Exception explaining the error.
     */
	Either<String, Exception> get(String url);

    /**
     * Issues a get request to url, thereby respecting a timeout of timeoutMillis.
     * @param url to where to issue the get request.
     * @param timeoutMillis timeout in milliseconds.
     * @return either a String with the resulting file content at that url or an Exception explaining the error.
     */
    Either<String, Exception> get(String url, int timeoutMillis);

    /**
     * Issues a get request to url, thereby respecting a fixed timeout of one second.
     * @param url to where to issue the get request.
     * @return either an InputStream of the resulting file content at that url or an Exception explaining the error.
     */
	Either<InputStream, Exception> getStream(String url);

}
