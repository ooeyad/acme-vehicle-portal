package com.acme.web.filter;

import com.acme.web.support.MockActionSupport;

import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The filter's whole contract is three headers and an unbroken chain, so that is what this
 * asserts. The ordering test is the one that matters: headers set after the chain runs are
 * headers set after Struts has committed the response, which silently does nothing.
 */
public class NoStoreCacheFilterTest {

    /** Records that the chain ran, and what the response looked like at that moment. */
    private static final class RecordingChain implements FilterChain {
        int calls;
        String cacheControlSeen;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            calls++;
            cacheControlSeen = ((HttpServletResponse) response).getHeader("Cache-Control");
        }
    }

    private static HttpServletRequest request() {
        return MockActionSupport.request(MockActionSupport.newRequestState());
    }

    @Test
    public void setsNoStoreSoTheBackButtonCannotReplayAVinList() throws Exception {
        MockActionSupport.ResponseState state = MockActionSupport.newResponseState();
        RecordingChain chain = new RecordingChain();

        new NoStoreCacheFilter().doFilter(request(), MockActionSupport.response(state), chain);

        String cacheControl = state.headers.get("Cache-Control");
        assertNotNull("no Cache-Control header was set", cacheControl);
        assertTrue("no-store is the directive that stops back-button replay: " + cacheControl,
                cacheControl.contains("no-store"));
        assertEquals(NoStoreCacheFilter.CACHE_CONTROL, cacheControl);
    }

    @Test
    public void setsPragmaAndExpiresForOlderClients() throws Exception {
        MockActionSupport.ResponseState state = MockActionSupport.newResponseState();

        new NoStoreCacheFilter().doFilter(request(), MockActionSupport.response(state),
                new RecordingChain());

        assertEquals(NoStoreCacheFilter.PRAGMA, state.headers.get("Pragma"));
        assertEquals(NoStoreCacheFilter.EXPIRES, state.headers.get("Expires"));
    }

    /**
     * Struts forwards to a JSP inside chain.doFilter and the response commits there. A header
     * set after that call is dropped by the container without an error, so pin the ordering.
     */
    @Test
    public void setsTheHeadersBeforeTheChainRuns() throws Exception {
        RecordingChain chain = new RecordingChain();

        new NoStoreCacheFilter().doFilter(request(),
                MockActionSupport.response(MockActionSupport.newResponseState()), chain);

        assertEquals("the chain must still run", 1, chain.calls);
        assertEquals("Cache-Control was not on the response when the chain ran",
                NoStoreCacheFilter.CACHE_CONTROL, chain.cacheControlSeen);
    }

    /**
     * The filter must never be the reason a vehicle screen fails to render. A non-HTTP
     * response is not something the current mapping produces, but the request still passes.
     */
    @Test
    public void passesANonHttpResponseStraightThrough() throws Exception {
        final int[] calls = new int[1];
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                calls[0]++;
            }
        };

        new NoStoreCacheFilter().doFilter(request(), plainResponse(), chain);

        assertEquals(1, calls[0]);
    }

    private static ServletResponse plainResponse() {
        return (ServletResponse) Proxy.newProxyInstance(
                NoStoreCacheFilterTest.class.getClassLoader(),
                new Class<?>[]{ServletResponse.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        return null;
                    }
                });
    }
}
