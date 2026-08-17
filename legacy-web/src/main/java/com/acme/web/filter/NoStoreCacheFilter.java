package com.acme.web.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Stops vehicle screens from being replayed out of the browser cache.
 *
 * Issue #5: pages rendering VIN lists carried nothing telling the browser not to keep them,
 * so on a shared dealership terminal the list came back with the back button after logout.
 *
 * The headers are set before the chain runs, so they are on the response before Struts
 * forwards to a JSP and the response commits. setHeader, not addHeader: a second pass over
 * the same response replaces the value instead of emitting a duplicate header.
 *
 * Registered in web.xml against /vehicle/*, deliberately not with @WebFilter - a filter
 * covering the vehicle screens has to be visible to whoever reviews the descriptor.
 */
public final class NoStoreCacheFilter implements Filter {

    /** no-store is the one that matters; the rest are for HTTP/1.0-era clients and proxies. */
    static final String CACHE_CONTROL = "no-store, no-cache, must-revalidate, max-age=0";
    static final String PRAGMA = "no-cache";
    static final String EXPIRES = "0";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Guarded rather than cast: the mapping is HTTP-only today, but a filter that throws
        // ClassCastException is a worse failure than one that quietly passes the request on.
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setHeader("Cache-Control", CACHE_CONTROL);
            httpResponse.setHeader("Pragma", PRAGMA);
            httpResponse.setHeader("Expires", EXPIRES);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
