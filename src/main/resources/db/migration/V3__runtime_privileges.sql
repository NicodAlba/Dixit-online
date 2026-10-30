-- runtimeRole is validated as a simple identifier before Flyway is instantiated.
GRANT USAGE ON SCHEMA dixit TO "${runtimeRole}";
GRANT SELECT ON dixit.authorities, dixit.images, dixit.badge_images TO "${runtimeRole}";
GRANT SELECT, INSERT, UPDATE, DELETE ON
    dixit.appusers, dixit.achievements, dixit.games, dixit.players, dixit.turns,
    dixit.cards, dixit.friend_requests, dixit.game_invitations, dixit.game_spectators,
    dixit.game_winners, dixit.messages, dixit.player_votes TO "${runtimeRole}";
GRANT USAGE, SELECT ON SEQUENCE dixit.entity_sequence TO "${runtimeRole}";
-- Future migrations grant only the new privileges they need. No DDL or history-table grant.
