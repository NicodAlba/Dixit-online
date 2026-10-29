-- Initial PostgreSQL schema from the existing JPA mappings; reviewed, never auto-generated at startup.

    create sequence dixit.entity_sequence start with 100 increment by 50;

    create table dixit.achievements (
        badge_image_id integer,
        id integer not null,
        threshold integer check (threshold>=0),
        user_id integer not null,
        date_obtained timestamp(6),
        dtype varchar(31) not null,
        name varchar(50) not null,
        description varchar(255) not null,
        primary key (id)
    );

    create table dixit.appusers (
        authority_id integer not null,
        id integer not null,
        password varchar(255),
        username varchar(255) not null unique,
        primary key (id)
    );

    create table dixit.authorities (
        id integer not null,
        authority varchar(20),
        primary key (id)
    );

    create table dixit.badge_images (
        id integer not null,
        type varchar(255) check (type in ('WIN','PLAY','TRICK')),
        url varchar(255),
        primary key (id)
    );

    create table dixit.cards (
        game_id integer,
        id integer not null,
        image_id integer,
        player_id integer,
        turn_id integer,
        user_id integer,
        primary key (id)
    );

    create table dixit.friend_requests (
        id integer not null,
        receiver_id integer,
        sender_id integer,
        date_accepted timestamp(6),
        primary key (id),
        unique (sender_id, receiver_id)
    );

    create table dixit.game_invitations (
        game_id integer not null check (game_id>=0),
        id integer not null,
        invited_id integer not null,
        inviter varchar(255) not null,
        password varchar(255),
        primary key (id),
        unique (invited_id, game_id)
    );

    create table dixit.game_spectators (
        game_id integer not null,
        user_id integer not null
    );

    create table dixit.game_winners (
        game_id integer not null,
        player_id integer not null
    );

    create table dixit.games (
        cancelled boolean not null,
        id integer not null,
        leader_id integer,
        end_date timestamp(6),
        start_date timestamp(6),
        password varchar(255),
        primary key (id)
    );

    create table dixit.images (
        id integer not null,
        url varchar(255),
        primary key (id)
    );

    create table dixit.messages (
        game_id integer,
        id integer not null,
        player_id integer,
        date timestamp(6),
        text varchar(255),
        primary key (id)
    );

    create table dixit.player_votes (
        card_id integer not null,
        player_id integer not null
    );

    create table dixit.players (
        game_id integer,
        id integer not null,
        player_vote_id integer,
        points integer check (points>=0),
        user_id integer not null,
        primary key (id)
    );

    create table dixit.turns (
        answer_id integer,
        game_id integer not null,
        id integer not null,
        storyteller_id integer,
        table_order_seed varchar(64),
        clue varchar(255),
        state varchar(255) check (state in ('WAITING_STORYTELLER_CARD','WAITING_STORYTELLER_CLUE','WAITING_PLAYER_CARDS','SHOWING_CARDS','VOTING','SCORING','FINISHED')),
        primary key (id)
    );

    alter table if exists dixit.achievements
       add constraint FKdqa1c8qa8crtxuucpq5avyhog
       foreign key (badge_image_id)
       references dixit.badge_images;

    alter table if exists dixit.achievements
       add constraint FK23j3i1mf5gekhl0vt35handyj
       foreign key (user_id)
       references dixit.appusers;

    alter table if exists dixit.appusers
       add constraint FK1a70nwa4m8xf04o56w1r8rg4s
       foreign key (authority_id)
       references dixit.authorities;

    alter table if exists dixit.cards
       add constraint FK6lxk4ofsk2ls5e51k0qaouho3
       foreign key (game_id)
       references dixit.games;

    alter table if exists dixit.cards
       add constraint FKq3gn2eq33p652ldd6tcgn1mh9
       foreign key (image_id)
       references dixit.images;

    alter table if exists dixit.cards
       add constraint FKj33f134qbe2clkn90ukjps2jc
       foreign key (player_id)
       references dixit.players;

    alter table if exists dixit.cards
       add constraint FKqeykkqk955x096ulw7e3yc4ti
       foreign key (turn_id)
       references dixit.turns;

    alter table if exists dixit.cards
       add constraint FKl6swa9rswlht67uubh5f88a3k
       foreign key (user_id)
       references dixit.appusers;

    alter table if exists dixit.friend_requests
       add constraint FKhf3u1d61lr8ftaeat29ojvmpp
       foreign key (receiver_id)
       references dixit.appusers;

    alter table if exists dixit.friend_requests
       add constraint FK7w6xlonjd7j9dcvu5saipy3ii
       foreign key (sender_id)
       references dixit.appusers;

    alter table if exists dixit.game_invitations
       add constraint FKk70x9qivxckcx8qbx2msob6q
       foreign key (invited_id)
       references dixit.appusers;

    alter table if exists dixit.game_spectators
       add constraint FKqpd68bv7qkq8b93o8svafl6hl
       foreign key (user_id)
       references dixit.appusers;

    alter table if exists dixit.game_spectators
       add constraint FKqs55546b5nebbu40m32qjh2pc
       foreign key (game_id)
       references dixit.games;

    alter table if exists dixit.game_winners
       add constraint FKqv3q1uctf58qp6bg5p6wx9a21
       foreign key (player_id)
       references dixit.players;

    alter table if exists dixit.game_winners
       add constraint FK2xuvn1a46s6irlore5a2nhns8
       foreign key (game_id)
       references dixit.games;

    alter table if exists dixit.games
       add constraint FK5qlpl44cgftxpl670t6eyv3ym
       foreign key (leader_id)
       references dixit.appusers;

    alter table if exists dixit.messages
       add constraint FKjyn2vkqncfpr8t6tundl28f4u
       foreign key (game_id)
       references dixit.games;

    alter table if exists dixit.messages
       add constraint FK9vlwhnuf8x558rmdf84a829x2
       foreign key (player_id)
       references dixit.players;

    alter table if exists dixit.player_votes
       add constraint FKc27uwglxw160c0h3f53aw83ba
       foreign key (player_id)
       references dixit.players;

    alter table if exists dixit.player_votes
       add constraint FKl0sd2xx0tgbynt6p3x68wskaf
       foreign key (card_id)
       references dixit.cards;

    alter table if exists dixit.players
       add constraint FKchry7scrns4qyuyxe9dpod9k3
       foreign key (game_id)
       references dixit.games;

    alter table if exists dixit.players
       add constraint FKj02sg20p0qoxgfii1hwdbe8g4
       foreign key (player_vote_id)
       references dixit.cards;

    alter table if exists dixit.players
       add constraint FK32n4c98t5ojoxjcgdbt8qujm1
       foreign key (user_id)
       references dixit.appusers;

    alter table if exists dixit.turns
       add constraint FKlnfrawv0uy908dxrbanhg8cbo
       foreign key (answer_id)
       references dixit.cards;

    alter table if exists dixit.turns
       add constraint FKkgqw02xfrtutv4keq3lr4rqbn
       foreign key (game_id)
       references dixit.games;

    alter table if exists dixit.turns
       add constraint FKh9qs3igpe7cdq9rmvibi7oymj
       foreign key (storyteller_id)
       references dixit.players;

-- Index foreign-key lookups used by game snapshots, history, social and locking paths.
CREATE INDEX cards_game_idx ON dixit.cards(game_id);
CREATE INDEX cards_hand_idx ON dixit.cards(player_id, turn_id);
CREATE INDEX cards_turn_idx ON dixit.cards(turn_id);
CREATE INDEX cards_author_idx ON dixit.cards(user_id);
CREATE INDEX players_game_idx ON dixit.players(game_id);
CREATE INDEX players_user_idx ON dixit.players(user_id);
CREATE INDEX turns_game_idx ON dixit.turns(game_id);
CREATE INDEX messages_game_idx ON dixit.messages(game_id);
CREATE INDEX friend_requests_receiver_idx ON dixit.friend_requests(receiver_id);
CREATE INDEX game_invitations_game_idx ON dixit.game_invitations(game_id);
CREATE INDEX achievements_user_idx ON dixit.achievements(user_id);
CREATE INDEX game_spectators_user_idx ON dixit.game_spectators(user_id);
CREATE INDEX game_winners_player_idx ON dixit.game_winners(player_id);
CREATE INDEX player_votes_player_idx ON dixit.player_votes(player_id);
