//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.slink.client;

import com.samskivert.util.ServiceWaiter.TimeoutException;
import com.samskivert.util.ServiceWaiter;

import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.client.InvocationService.ResultListener;

import com.threerings.sling.web.client.SlingException;

/**
 * Bridges the gap between a game service for implementing support methods and the jetty servlet
 * that returns synchronous results. Maybe someday jetty will be async or will support something
 * like fibers.
 */
public class SlinkWaiter<R>
{
    /**
     * Simple factory to help avoid double generic argument listing in the caller.
     */
    public static <T> SlinkWaiter<T> create (SlinkLoginManager mgr)
    {
        return new SlinkWaiter<T>(mgr);
    }

    /**
     * Creates a new waiter.
     */
    public SlinkWaiter (SlinkLoginManager mgr)
    {
        _mgr = mgr;
    }

    /**
     * Gets the service on which the slink method will be called.
     * @throws SlingException if the client is not logged in or the service is not configured
     */
    public <T> T getService (Class<T> sclass)
        throws SlingException
    {
        if (!_mgr.getClient().isLoggedOn()) {
            throw new SlingException("m.game_link_down");
        }
        try {
            return _mgr.getClient().requireService(sclass);
        } catch (RuntimeException e) {
            throw new SlingException("m.internal_error");
        }
    }

    /**
     * Gets the listener that may be passed into the service method.
     */
    public ResultListener getResultListener ()
    {
        return new ResultListener() {
            @Override public void requestProcessed (Object result) {
                @SuppressWarnings("unchecked")
                R casted = (R)result;
                _waiter.postSuccess(casted);
            }

            @Override public void requestFailed (String cause) {
                _waiter.postFailure(new Exception(cause));
            }
        };
    }

    /**
     * Gets the listener that may be passed into the service method.
     */
    public ConfirmListener getConfirmListener ()
    {
        return new ConfirmListener() {
            @Override public void requestProcessed () {
                _waiter.postSuccess(null);
            }

            @Override public void requestFailed (String cause) {
                _waiter.postFailure(new Exception(cause));
            }
        };
    }

    /**
     * Waits for the service method result to come back, and then returns the result.
     * @throws SlingException if there is a timeout or if the service method failed.
     */
    public R waitForResponse ()
        throws SlingException
    {
        _waiter = new ServiceWaiter<R>();
        try {
            if (!_waiter.waitForResponse()) {
                throw new SlingException(_waiter.getError().getMessage());
            }
        } catch (TimeoutException e) {
            throw new SlingException("m.game_link_rpc_failed");
        }
        return _waiter.getArgument();
    }

    protected SlinkLoginManager _mgr;
    protected ServiceWaiter<R> _waiter;
}
