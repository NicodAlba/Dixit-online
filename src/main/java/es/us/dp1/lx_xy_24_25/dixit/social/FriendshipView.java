package es.us.dp1.lx_xy_24_25.dixit.social;

/** Only a relationship of the authenticated account; no user entity graph. */
public record FriendshipView(Integer accountId, String username, String status, Integer requestId) {}
