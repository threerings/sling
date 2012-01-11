//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server;

import java.net.InetAddress;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.samskivert.util.Lifecycle;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.Credentials;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.PresentsSession;

import com.threerings.sling.server.persist.EventRecord;
import com.threerings.sling.server.persist.SlingRepository;

/**
 * A server module for reporting information relevant to customer support that happens during the
 * game.
 */
@Singleton
public class SupportManager implements Lifecycle.InitComponent
{
    /**
     * Prepare this support manager for use.
     */
    @Override // from Lifecycle.InitComponent
    public void init ()
    {
    }

    public static void fillSessionInfo (
        PresentsSession source, PresentsSession target, EventRecord event)
    {
        if (source != null) {
            event.sourceIpAddress = getIpAddress(source);
            event.sourceMachineIdent = extractIdent(source);
            event.language = extractLanguage(source);
        }
        if (target != null) {
            event.targetIpAddress = getIpAddress(target);
            event.targetMachineIdent = extractIdent(target);
        }
    }

    protected void fillSessionInfo (ClientObject source, ClientObject target, EventRecord event)
    {
        fillSessionInfo(
            source == null ? null : _clmgr.getClient(source.username),
            target == null ? null : _clmgr.getClient(target.username), event);
    }

    /**
     * Returns a string representing the IP address of the specified session, or "0.0.0.0" if
     * disconnected.
     */
    protected static String getIpAddress (PresentsSession session)
    {
        InetAddress address = session.getInetAddress();
        return (address == null) ? "0.0.0.0" : address.toString();
    }

    protected static String extractIdent (PresentsSession session)
    {
        Credentials creds = session.getCredentials();
        if (creds instanceof Credentials.HasMachineIdent) {
            return ((Credentials.HasMachineIdent)creds).getMachineIdent();
        }
        return null;
    }

    protected static String extractLanguage (PresentsSession session)
    {
        Credentials creds = session.getCredentials();
        if (creds instanceof Credentials.HasLanguage) {
            return ((Credentials.HasLanguage)creds).getLanguage();
        }
        return null;
    }

    protected @Inject ClientManager _clmgr;
    protected @Inject @MainInvoker Invoker _invoker;
    protected @Inject SlingRepository _repo;
}
