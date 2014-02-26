//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A filter for an {@link EventSearch}.
 */
public class EventFilter
    implements IsSerializable
{
    /** Types of filter. */
    public enum Type
    {
        TYPE_IS, TYPE_IS_IN, STATUS_IS, HAS_NOTE, CREATED_BETWEEN, UPDATED_BETWEEN, OWNER_IS,
        OWNER_ID_IS, GAME_NAME_IS, ACCOUNT_NAME_IS, SUBJECT_MATCHES, CHAT_HISTORY_MATCHES,
        NOTE_MATCHES, FIRST_RESPONSE_IS_MORE_THAN, IP_ADDRESS_IS, MACHINE_IDENT_IS,
        WAITING_FOR_PLAYER, LANGUAGE_IS;
    }

    public static EventFilter typeIs (Event.Type type)
    {
        return new EventFilter(Type.TYPE_IS, type.name());
    }

    public static EventFilter typeIsIn (Set<Event.Type> types)
    {
        return new EventFilter(Type.TYPE_IS_IN, flatten(types));
    }

    public static EventFilter statusIs (Event.Status status)
    {
        return new EventFilter(Type.STATUS_IS, status.name());
    }

    public static EventFilter hasNote ()
    {
        return new EventFilter(Type.HAS_NOTE, null);
    }

    public static EventFilter createdIn (TimeRange range)
    {
        return new EventFilter(Type.CREATED_BETWEEN, flatten(range));
    }

    public static EventFilter updatedIn (TimeRange range)
    {
        return new EventFilter(Type.UPDATED_BETWEEN, flatten(range));
    }

    public static EventFilter ownerIs (String owner)
    {
        return new EventFilter(Type.OWNER_IS, owner);
    }

    public static EventFilter ownerIdIs (int ownerId)
    {
        return new EventFilter(Type.OWNER_ID_IS, String.valueOf(ownerId));
    }

    public static EventFilter gameNameIs (String gameName)
    {
        return new EventFilter(Type.GAME_NAME_IS, gameName);
    }

    public static EventFilter accountNameIs (String accountName)
    {
        return new EventFilter(Type.ACCOUNT_NAME_IS, accountName);
    }

    public static EventFilter subjectMatches (String terms)
    {
        return new EventFilter(Type.SUBJECT_MATCHES, terms);
    }

    public static EventFilter chatHistoryMatches (String terms)
    {
        return new EventFilter(Type.CHAT_HISTORY_MATCHES, terms);
    }

    public static EventFilter noteMatches (String terms)
    {
        return new EventFilter(Type.NOTE_MATCHES, terms);
    }

    public static EventFilter firstResponseIsMoreThan (long millis)
    {
        return new EventFilter(Type.FIRST_RESPONSE_IS_MORE_THAN, String.valueOf(millis));
    }

    public static EventFilter ipAddressIs(String ip)
    {
        return new EventFilter(Type.IP_ADDRESS_IS, ip);
    }

    public static EventFilter machineIdentIs(String ident)
    {
        return new EventFilter(Type.MACHINE_IDENT_IS, ident);
    }

    public static EventFilter waitingForPlayer(boolean value)
    {
        return new EventFilter(Type.WAITING_FOR_PLAYER, value ? "t" : "f");
    }

    public static EventFilter languageIs (String lang)
    {
        return new EventFilter(Type.LANGUAGE_IS, lang);
    }

    /** The type of filter. */
    public Type type;

    public EventFilter ()
    {
    }

    /** For custom deserialization. Prefer factories above. */
    public EventFilter (Type type, String query)
    {
        this.type = type;
        _query = query;
    }

    public Event.Type getEventType ()
    {
        requireType(Type.TYPE_IS);
        return Event.Type.valueOf(_query);
    }

    public Set<Event.Type> getEventTypes ()
    {
        requireType(Type.TYPE_IS_IN);
        return toSet(_query);
    }

    public Event.Status getEventStatus ()
    {
        requireType(Type.STATUS_IS);
        return Event.Status.valueOf(_query);
    }

    public TimeRange getTimeRange ()
    {
        requireType(Type.CREATED_BETWEEN, Type.UPDATED_BETWEEN);
        int slashPos = _query.indexOf("/");
        return new TimeRange(
            Long.valueOf(_query.substring(0, slashPos)),
            Long.valueOf(_query.substring(slashPos + 1)));
    }

    public String getOwner ()
    {
        requireType(Type.OWNER_IS);
        return _query;
    }

    public int getOwnerId ()
    {
        requireType(Type.OWNER_ID_IS);
        return Integer.valueOf(_query);
    }

    public String getGameName ()
    {
        requireType(Type.GAME_NAME_IS);
        return _query;
    }

    public String getAccountName ()
    {
        requireType(Type.ACCOUNT_NAME_IS);
        return _query;
    }

    public String getSearchTerms ()
    {
        requireType(Type.SUBJECT_MATCHES, Type.CHAT_HISTORY_MATCHES, Type.NOTE_MATCHES);
        return _query;
    }

    public long getMillis ()
    {
        requireType(Type.FIRST_RESPONSE_IS_MORE_THAN);
        return Long.valueOf(_query);
    }

    public String getIpAddress ()
    {
        requireType(Type.IP_ADDRESS_IS);
        return _query;
    }

    public String getMachineIdent ()
    {
        requireType(Type.MACHINE_IDENT_IS);
        return _query;
    }

    public String getLanguage ()
    {
        requireType(Type.LANGUAGE_IS);
        return _query;
    }

    public boolean getBoolean ()
    {
        requireType(Type.WAITING_FOR_PLAYER);
        if (_query.equals("t")) {
            return true;
        } else if (_query.equals("f")) {
            return false;
        }
        throw new IllegalStateException("Eventfilter " + this + " does not have a boolean value.");
    }

    /**
     * For custom serialization.
     */
    public String exposeRawQuery ()
    {
        return _query;
    }

    @Override public String toString ()
    {
        return "EventFilter [type=" + type + ", match=" + _query + "]";
    }

    protected void requireType (Type... types)
    {
        for (Type type : types) {
            if (this.type == type) {
                return;
            }
        }
        throw new IllegalStateException("Eventfilter " + this + " does not support operation.");
    }

    protected static String flatten (TimeRange range)
    {
        return range.from + "/" + range.to;
    }

    protected static String flatten (Set<Event.Type> types)
    {
        StringBuilder buff = new StringBuilder();
        for (Event.Type type : types) {
            if (buff.length() > 0) {
                buff.append(",");
            }
            buff.append(type.name());
        }
        return buff.toString();
    }

    protected static Set<Event.Type> toSet (String str)
    {
        Set<Event.Type> set = new HashSet<Event.Type>();
        for (String name : str.split(",")) {
            set.add(Event.Type.valueOf(name));
        }
        return set;
    }

    /** The value the event field should match for field filters. The interpretation of the query
     * depends on the type of filter. */
    protected String _query;
}
