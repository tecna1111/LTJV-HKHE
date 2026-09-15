local st = require 'util.stanza';
local util = module:require 'util';

local function is_organizer(session)
    local user = session and session.auth_token and session.jitsi_meet_context_user;
    return user and user.affiliation == 'owner' and user.moderator == true;
end

local function deny(event)
    event.origin.send(st.error_reply(event.stanza, 'auth', 'forbidden',
        'Only the meeting organizer may perform this operation.'));
    return true;
end

local base = assert(module:get_option_string('muc_mapper_domain_base'));
util.process_host_module('endconference.' .. base, function(host_module)
    module:context(host_module.host):hook('message/host', function(event)
        if event.stanza.attr.type ~= 'error'
            and event.stanza:get_child('end_conference')
            and not is_organizer(event.origin) then
            return deny(event);
        end
    end, 100);
end);

module:hook('iq-set/bare/http://jabber.org/protocol/muc#owner:query', function(event)
    local query = event.stanza:get_child('query', 'http://jabber.org/protocol/muc#owner');
    if query and query:get_child('destroy') and not is_organizer(event.origin) then
        return deny(event);
    end
end, 100);

module:hook('iq-set/bare/http://jabber.org/protocol/muc#admin:query', function(event)
    local query = event.stanza:get_child('query', 'http://jabber.org/protocol/muc#admin');
    if not query then return; end
    for item in query:childtags('item') do
        if item.attr.affiliation == 'owner' or item.attr.affiliation == 'admin'
            or item.attr.role == 'moderator' then
            return deny(event);
        end
    end
end, 100);
