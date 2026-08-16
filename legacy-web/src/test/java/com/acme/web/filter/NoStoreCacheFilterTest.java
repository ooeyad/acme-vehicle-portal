package com.acme.web.filter;

import com.acme.web.support.MockActionSupport;

import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The filter's whole contract is three headers and an unbroken chain, so that is what this
 * asserts. The ordering test is the one that matters: headers set after the chain runs are
 * headers set after Struts has committed the response, which silently does nothing.
 *
 * Header values are asserted as LITERALS, never against NoStoreCacheFilter's own constants.
 * assertEquals(NoStoreCacheFilter.CACHE_CONTROL, actual) only proves the filter used its own
 * constant - editing that constant to drop no-store, which is the entire point of issue #5,
 * would leave such an assertion green. The literal is the requirement; the constant is an
 * implementation detail that has to keep matching it.
 */
public class NoStoreCacheFilterTest {

    /** The header values issue #5 requires. Duplicated from the filter on purpose - see above. */
    private static final String EXPECTED_CACHE_CONTROL =
            "no-store, no-cache, must-revalidate, max-age=0";
    private static final String EXPECTED_PRAGMA = "no-cache";
    private static final String EXPECTED_EXPIRES = "0";

    /** Records that the chain ran, and what the response looked like at that moment. */
    private static final class RecordingChain implements FilterChain {
        int calls;
        String cacheControlSeen;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            calls++;
            if (response instanceof HttpServletResponse) {
                cacheControlSeen = ((HttpServletResponse) response).getHeader("Cache-Control");
            }
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

        String cacheControl = state.header("Cache-Control");
        assertNotNull("no Cache-Control header was set", cacheControl);
        assertTrue("no-store is the directive that stops back-button replay: " + cacheControl,
                cacheControl.contains("no-store"));
        assertEquals(EXPECTED_CACHE_CONTROL, cacheControl);
    }

    @Test
    public void setsPragmaAndExpiresForOlderClients() throws Exception {
        MockActionSupport.ResponseState state = MockActionSupport.newResponseState();

        new NoStoreCacheFilter().doFilter(request(), MockActionSupport.response(state),
                new RecordingChain());

        assertEquals(EXPECTED_PRAGMA, state.header("Pragma"));
        assertEquals(EXPECTED_EXPIRES, state.header("Expires"));
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
        assertNotNull("Cache-Control was not on the response when the chain ran",
                chain.cacheControlSeen);
        assertTrue("no-store must be on the response before Struts can commit it: "
                        + chain.cacheControlSeen,
                chain.cacheControlSeen.contains("no-store"));
        assertEquals(EXPECTED_CACHE_CONTROL, chain.cacheControlSeen);
    }

    /**
     * setHeader, not addHeader. Two passes over the same response - a second registration of
     * the filter, or a switch to addHeader - would emit Cache-Control twice, and intermediaries
     * that resolve a duplicate by taking the first value can drop the no-store one.
     */
    @Test
    public void emitsExactlyOneCacheControlValueEvenOnASecondPass() throws Exception {
        MockActionSupport.ResponseState state = MockActionSupport.newResponseState();
        HttpServletResponse response = MockActionSupport.response(state);

        new NoStoreCacheFilter().doFilter(request(), response, new RecordingChain());
        new NoStoreCacheFilter().doFilter(request(), response, new RecordingChain());

        List<String> cacheControl = state.values("Cache-Control");
        assertEquals("a duplicate Cache-Control header was emitted: " + cacheControl,
                1, cacheControl.size());
        assertEquals(EXPECTED_CACHE_CONTROL, cacheControl.get(0));
    }

    /**
     * The filter must never be the reason a vehicle screen fails to render. A non-HTTP
     * response is not something the current mapping produces, but the request still passes.
     */
    @Test
    public void passesANonHttpResponseStraightThrough() throws Exception {
        RecordingChain chain = new RecordingChain();

        new NoStoreCacheFilter().doFilter(request(), MockActionSupport.plainResponse(), chain);

        assertEquals("the request must still reach the application", 1, chain.calls);
    }
}
