package es.us.dp1.lx_xy_24_25.dixit.social;

import es.us.dp1.lx_xy_24_25.dixit.auth.payload.response.MessageResponse;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@Tag(name = "Friend")
public class FriendRestController {

    private final FriendRequestService friendRequestService;
    private final UserService userService;

    @Autowired
    FriendRestController(FriendRequestService friendRequestService, UserService userService) {
        this.friendRequestService = friendRequestService;
        this.userService = userService;
    }


    @PostMapping(value = "addFriend/{userId}")
    @Operation(summary = "Sends a friend request to the user with the given id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "404"),
        @ApiResponse(responseCode = "409")
    })
    public ResponseEntity<MessageResponse> addFriend(@PathVariable("userId") Integer userId) {
        try {
            User sender = userService.findCurrentUser();
            User receiver = userService.findById(userId);
            friendRequestService.sendFriendRequest(sender, receiver);
            return new ResponseEntity<>(new MessageResponse("Friend request sent!"), HttpStatus.OK);
        } catch (Exception e) {
            return switch (e) {
                case ResourceNotFoundException resourceNotFoundException ->
                    new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
                case IllegalStateException illegalStateException ->
                    new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.CONFLICT);
                default -> new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
            };
        }
    }

    @PatchMapping(value = "{requestId}/accept")
    @Operation(summary = "Accepts the given friend request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "403"),
        @ApiResponse(responseCode = "404"),
        @ApiResponse(responseCode = "409")
    })
    public ResponseEntity<MessageResponse> acceptFriend(@PathVariable("requestId") Integer requestId) {
        try {
            User receiver = userService.findCurrentUser();
            FriendRequest request = friendRequestService.findById(requestId);
            friendRequestService.acceptFriendRequest(receiver, request);
            return new ResponseEntity<>(new MessageResponse("Friend request accepted!"), HttpStatus.OK);
        } catch (Exception e) {
            return switch (e) {
                case ResourceNotFoundException resourceNotFoundException ->
                    new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
                case AccessDeniedException accessDeniedException ->
                    new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
                case IllegalStateException illegalStateException ->
                    new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.CONFLICT);
                default -> new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
            };
        }
    }

    @DeleteMapping(value = "{requestId}/deny")
    @Operation(summary = "Denies the given friend request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "403"),
        @ApiResponse(responseCode = "404"),
        @ApiResponse(responseCode = "409")
    })
    public ResponseEntity<MessageResponse> denyFriend(@PathVariable("requestId") Integer requestId) {
        try {
            User receiver = userService.findCurrentUser();
            FriendRequest request = friendRequestService.findById(requestId);
            friendRequestService.deleteFriendRequest(receiver, request);
            return new ResponseEntity<>(new MessageResponse("Friend request denied!"), HttpStatus.OK);
        } catch (Exception e) {
            return switch (e) {
                case ResourceNotFoundException resourceNotFoundException ->
                    new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
                case AccessDeniedException accessDeniedException ->
                    new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
                case IllegalStateException illegalStateException ->
                    new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.CONFLICT);
                default -> new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
            };
        }
    }

    @DeleteMapping(value = "{userId}/remove")
    @Operation(summary = "Removes a friend of the current user by the given id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "403"),
        @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<MessageResponse> removeFriend(@PathVariable("userId") Integer userId) {
        try {
            User user = userService.findCurrentUser();
            User friend = userService.findById(userId);
            FriendRequest request = friendRequestService.findRequestByUsers(user, friend);
            friendRequestService.removeFriend(user, request);
            return new ResponseEntity<>(new MessageResponse("You are no longer friends"), HttpStatus.OK);
        } catch (Exception e) {
            switch (e) {
                case ResourceNotFoundException resourceNotFoundException -> {
                    return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
                }
                case AccessDeniedException accessDeniedException -> {
                    return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
                }
                default -> {
                    return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    @GetMapping(value = "pending")
    @Operation(summary = "Retrieves all pending requests")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200")
    })
    public ResponseEntity<List<FriendRequest>> getPendingFriendRequests() {
        User receiver = userService.findCurrentUser();
        List<FriendRequest> requests = friendRequestService.findPendingRequests(receiver);
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Retrieves the list of friends of the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200")
    })
    public ResponseEntity<List<User>> getFriends() {
        User receiver = userService.findCurrentUser();
        List<User> friends = friendRequestService.getFriends(receiver);
        return new ResponseEntity<>(friends, HttpStatus.OK);
    }
}
