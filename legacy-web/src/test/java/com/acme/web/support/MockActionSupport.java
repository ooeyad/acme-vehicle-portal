package com.acme.web.support;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
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

    public static HttpServletResponse response() {
        return (HttpServletResponse) Proxy.newProxyInstance(
                MockActionSupport.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("hashCode".equals(name)) { return System.identityHashCode(proxy); }
                        if ("equals".equals(name)) { return proxy == args[0]; }
                        if ("toString".equals(name)) { return "MockResponse"; }
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
