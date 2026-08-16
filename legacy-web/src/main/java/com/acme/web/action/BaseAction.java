package com.acme.web.action;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Every Action in this application extends BaseAction, never Action directly.
 *
 * BaseAction owns the two things every screen needs and nothing else: uncaught-exception
 * handling, and a helper for adding a user-visible error. Subclasses implement
 * {@link #doExecute} and stay short.
 */
public abstract class BaseAction extends Action {

    protected static final String SUCCESS = "success";
    protected static final String FAILURE = "failure";

    @Override
    public final ActionForward execute(ActionMapping mapping,
                                       ActionForm form,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {
        try {
            return doExecute(mapping, form, request, response);
        } catch (RuntimeException e) {
            // Never let a runtime exception reach the container: the user gets a stack trace.
            addError(request, "error.unexpected");
            return mapping.findForward(FAILURE);
        }
    }

    /**
     * Read the form, call one service, put results in request scope, return a forward.
     * No business logic. No SQL. No DAO construction.
     */
    protected abstract ActionForward doExecute(ActionMapping mapping,
                                               ActionForm form,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws Exception;

    /** Adds a message-bundle key as a user-visible error. Never pass raw text. */
    protected void addError(HttpServletRequest request, String messageKey) {
        ActionMessages errors = new ActionMessages();
        errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(messageKey));
        saveErrors(request, errors);
    }

    /**
     * Adds an error attached to a named form field, so the JSP can render it next to the input
     * rather than in the global block. Added for issue #1.
     */
    protected void addFieldError(HttpServletRequest request, String property, String messageKey) {
        ActionMessages errors = new ActionMessages();
        errors.add(property, new ActionMessage(messageKey));
        saveErrors(request, errors);
    }

    protected void addError(HttpServletRequest request, String messageKey, Object arg) {
        ActionMessages errors = new ActionMessages();
        errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(messageKey, arg));
        saveErrors(request, errors);
    }
}
