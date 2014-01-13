package de.fungate.translate.core.services.curlers;

import de.fungate.translate.core.services.Curler;
import fj.data.Either;
import org.apache.http.client.fluent.Request;
import org.apache.log4j.Logger;

import java.io.InputStream;

/**
 * Curler using the ApacheHTTPClient libraries to fulfill its duties.
 */
public class ApacheHTTPCurler implements Curler {

    private static final Logger LOG = Logger.getLogger(ApacheHTTPCurler.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public Either<String, Exception> get(String url) {
        return get(url, 1000);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Either<String, Exception> get(String url, int timeoutMillis) {
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("get from " + url);
            }
            return Either.left(Request.Get(url)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .connectTimeout(timeoutMillis)
                    .execute()
                    .returnContent()
                    .asString());
        } catch (Exception e) {
            return Either.right(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Either<InputStream, Exception> getStream(String url) {
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("getStream from " + url);
            }
            return Either.left(Request.Get(url)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .connectTimeout(1000)
                    .execute()
                    .returnContent()
                    .asStream());
        } catch (Exception e) {
            return Either.right(e);
        }
    }

}
