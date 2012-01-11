//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.slink.server;

import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.ServiceAuthenticator;
import com.threerings.presents.server.SessionFactory;
import com.threerings.presents.server.net.PresentsConnectionManager;

import com.threerings.slink.Log;
import com.threerings.slink.data.SlinkAuthName;
import com.threerings.slink.data.SlinkCreds;

/**
 * Enables slink logins on a server by registering the necessary networking hooks for authentication
 * and session creation. Uses an injected properties instance.<p>The values used are:</p><ul>
 *     <li><em>shared_secret</em>:
 *         The value to use for hashing the client identifier. See
 *         {@link com.threerings.presents.net.ServiceCreds ServiceCreds}</li></ul>
 * <p>The application guice module is expected to bind a properties instance for an instance with
 * the "com.threerings.slink" name annotation. For example:<pre>
 *     bind(Properties.class)
 *         .annotatedWith(Names.named("com.threerings.slink"))
 *         .toInstance(config.getSubProperties("slink"));
 * </pre></p>
 *
 * TODO: what would be really nice would be if there were some way to detect if a connection was
 * coming in from an EC2 private IP address and use that as a secondary authentication check. The
 * allowed_clients flag is annoying because it may need updating whenever a support webapp machine
 * is added to the cluster or rebooted.
 */
@Singleton
public class SlinkRegistry
{
    @Inject public SlinkRegistry (PresentsConnectionManager conmgr, ClientManager clmgr,
        @Named("com.threerings.slink") Properties properties)
    {
        _sharedSecret = properties.getProperty("shared_secret");
        if (_sharedSecret == null) {
            Log.log.info("shared_secret property not found, logins disabled");
        }

        conmgr.addChainedAuthenticator(
            new ServiceAuthenticator<SlinkCreds>(SlinkCreds.class, SlinkAuthName.class) {
                @Override protected boolean areValid (SlinkCreds creds) {
                    return _sharedSecret != null && creds.areValid(_sharedSecret);
                }});

        clmgr.addSessionFactory(createSessionFactory());
    }

    protected SessionFactory createSessionFactory ()
    {
        return SessionFactory.newSessionFactory(
            SlinkCreds.class, PresentsSession.class, SlinkAuthName.class, ClientResolver.class);
    }

    protected String _sharedSecret;
}
