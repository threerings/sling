//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.server;

import java.util.Properties;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Config;

import com.samskivert.servlet.JDBCTableSiteIdentifier;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;

import com.threerings.sling.server.DummyGameActionHandler;
import com.threerings.sling.server.DummyGameInfoProvider;
import com.threerings.sling.server.SlingEnvironment;
import com.threerings.sling.server.UserLogic;
import com.threerings.sling.server.persist.SlingRepository;

/**
 * A concrete SlingServlet implementation used for testing.
 */
public class TestSlingServlet extends SlingServlet
{
    @Override // from SlingServlet
    protected SlingEnvironment createEnv ()
    {
        Properties authprops = new Config("test").getSubProperties("oooauth");
        try {
            return new SlingEnvironment(new JDBCTableSiteIdentifier(_conprov),
                                    new DummyGameInfoProvider(),
                                    new DummyGameActionHandler(),
                                    UserLogic.newOOOUserLogic(_conprov, authprops),
                                    new SlingRepository(_conprov));
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    protected ConnectionProvider _conprov =
        new StaticConnectionProvider(new Config("test").getSubProperties("db"));
}
