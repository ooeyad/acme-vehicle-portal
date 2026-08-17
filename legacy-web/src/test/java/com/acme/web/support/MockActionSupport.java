package com.acme.web.support;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test doubles for driving a Struts Action without a servlet container.
 *
 * HttpServletRequest has ~70 methods and we need three of them, so it is created as a
 * dynamic proxy rather than a 400-line hand-written stub. Any method the test does not
 * teach it throws, which is what you want: an Action reaching for something unexpected
 * fails loudly instead of silently receiving null.
 *
 * USE THIS. Do not write a new mock helper per test - that is how the legacy test suite
 * ended up with six incompatible ones.
 */
public final class MockActionSupport {

    private MockActionSupport() { }

    /** Records attributes set by the Action so a test can assert on them. */
    public static final class RequestState {
        public final Map<String, Object> attributes = new HashMap<String, Object>();
        public final Map<String, Object> sessionAttributes = new HashMap<String, Object>();
    }

    public static RequestState newRequestState() {
        return new RequestState();
    }

    /**
     * Records headers set on the response so a test can assert on them. Added for issue #5.
     *
     * Every value is kept, not just the last one, because setHeader and addHeader differ in
     * exactly the way this records: setHeader replaces the list, addHeader appends to it. A
     * flat Map&lt;String, String&gt; collapsed the two, so a filter switching to addHeader - or
     * being registered twice - would emit a duplicate Cache-Control header that no test could
     * see. Some intermediaries resolve a duplicate by taking the first value, which is how a
     * response ends up without no-store while every assertion still passes.
     */
    public static final class ResponseState {
        public final Map<String, List<String>> headers = new HashMap<String, List<String>>();

        /** @return the last value set for the header, or null if it was never set. */
        public String header(String name) {
            List<String> values = headers.get(name);
            return values == null || values.isEmpty() ? null : values.get(values.size() - 1);
        }

        /** @return every value set for the header, oldest first; empty if it was never set. */
        public List<String> values(String name) {
            List<String> values = headers.get(name);
            return values == null ? Collections.<String>emptyList() : values;
        }
    }

    public static ResponseState newResponseState() {
        return new ResponseState();
    }

    /**
     * @return a request that supports getAttribute, setAttribute and removeAttribute,
     *         plus getParameter returning null. Everything else throws.
     */
    public static HttpServletRequest request(final RequestState state) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                MockActionSupport.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("setAttribute".equals(name)) {
                            state.attributes.put((String) args[0], args[1]);
                            return null;
                        }
                        if ("getAttribute".equals(name)) {
                            return state.attributes.get(args[0]);
                        }
                        if ("removeAttribute".equals(name)) {
                            state.attributes.remove(args[0]);
                            return null;
                        }
                        if ("getParameter".equals(name) || "getSession".equals(name)) {
                            return null;
                        }
                        if ("toString".equals(name)) {
                            return "MockRequest" + state.attributes;
                        }
                        if ("hashCode".equals(name)) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(name)) {
                            return proxy == args[0];
                        }
                        throw new UnsupportedOperationException(
                                "MockActionSupport does not implement HttpServletRequest." + name
                              + "() - add it deliberately if the Action really needs it.");
                    }
                });
    }

    /** A response that discards everything. Unchanged behaviour for the Action tests. */
    public static HttpServletResponse response() {
        return response(newResponseState());
    }

    /**
     * @return a response that records setHeader/addHeader into the given state and answers
     *         getHeader and containsHeader from it. Everything else returns null, so an
     *         Action or filter calling something else is not derailed by this double.
     */
    public static HttpServletResponse response(final ResponseState state) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                MockActionSupport.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("setHeader".equals(name)) {
                            List<String> replacement = new ArrayList<String>();
                            replacement.add((String) args[1]);
                            state.headers.put((String) args[0], replacement);
                            return null;
                        }
                        if ("addHeader".equals(name)) {
                            List<String> values = state.headers.get(args[0]);
                            if (values == null) {
                                values = new ArrayList<String>();
                                state.headers.put((String) args[0], values);
                            }
                            values.add((String) args[1]);
                            return null;
                        }
                        if ("getHeader".equals(name)) {
                            return state.header((String) args[0]);
                        }
                        if ("getHeaders".equals(name)) {
                            return state.values((String) args[0]);
                        }
                        if ("containsHeader".equals(name)) {
                            return Boolean.valueOf(state.headers.containsKey(args[0]));
                        }
                        if ("hashCode".equals(name)) { return System.identityHashCode(proxy); }
                        if ("equals".equals(name)) { return proxy == args[0]; }
                        if ("toString".equals(name)) { return "MockResponse" + state.headers; }
                        return null;
                    }
                });
    }

    /**
     * @return a bare ServletResponse - deliberately NOT an HttpServletResponse - answering null
     *         to everything. For asserting that a filter handed a non-HTTP response still passes
     *         the request down the chain rather than throwing ClassCastException.
     */
    public static ServletResponse plainResponse() {
        return (ServletResponse) Proxy.newProxyInstance(
                MockActionSupport.class.getClassLoader(),
                new Class<?>[]{ServletResponse.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("hashCode".equals(name)) { return System.identityHashCode(proxy); }
                        if ("equals".equals(name)) { return proxy == args[0]; }
                        if ("toString".equals(name)) { return "MockPlainResponse"; }
                        return null;
                    }
                });
    }

    /**
     * An ActionMapping whose findForward(name) returns a forward whose path is the name,
     * so a test asserts on which forward was chosen without loading struts-config.xml.
     */
    public static ActionMapping mapping() {
        return new ActionMapping() {
            private static final long serialVersionUID = 1L;

            @Override
            public ActionForward findForward(String forwardName) {
                return new ActionForward(forwardName, forwardName, false);
            }
        };
    }
}
