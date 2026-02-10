//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import com.google.gwt.i18n.client.ConstantsWithLookup;

/**
 * Defines all translation messages returned by the server.
 */
public interface ServerMessages extends ConstantsWithLookup
{
    public String internal_error ();
    public String access_denied ();
    public String unknown_user ();
    public String invalid_password ();
    public String session_expired ();
    public String invalid_email ();
    public String invalid_search ();
    public String no_such_user ();
    public String player_not_authorized ();
    public String unknown_character ();
    public String game_link_down ();
    public String game_link_rpc_failed ();
    public String invalid_name ();
}
