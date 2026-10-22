package es.us.dp1.lx_xy_24_25.dixit.social;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import es.us.dp1.lx_xy_24_25.dixit.configuration.SecurityConfiguration;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.user.Authorities;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;

@Epic("Friends module")
@Feature("Friends Management")
@WebMvcTest(controllers = FriendRestController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebSecurityConfigurer.class), excludeAutoConfiguration = SecurityConfiguration.class)
public class FriendRestControllerTest {

    private static final String BASE_URL = "/api/v1/friends";
    private static final Integer senderId = 1;
    private static final Integer receiverId = 2;
    private static final Integer friendRequestId = 3;

    @Autowired
    private FriendRestController friendRestController;
    @MockBean
    private UserService userService;
    @MockBean
    private FriendRequestService friendRequestService;

	@Autowired
	private MockMvc mockMvc;

    private User sender;
    private User receiver;
    private FriendRequest friendRequest;

    @BeforeEach
    void setUp() {
        Authorities auth = new Authorities();
		auth.setId(1);
		auth.setAuthority("PLAYER");

        sender = new User();
        sender.setUsername("sender");
        sender.setPassword("test");
        sender.setAuthority(auth);
        sender.setId(senderId);

        receiver = new User();
        receiver.setUsername("receiver");
        receiver.setPassword("test");
        receiver.setAuthority(auth);
        receiver.setId(receiverId);

        friendRequest = new FriendRequest();
        friendRequest.setId(friendRequestId);
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);

    }

    @Test
    @WithMockUser("test")
    void testAcceptFriend() throws Exception{
        when(userService.findCurrentUser()).thenReturn(sender);
        when(friendRequestService.findById(friendRequestId)).thenReturn(friendRequest);

        mockMvc.perform(patch(BASE_URL + "/{requestId}/accept", friendRequestId).with(csrf())).andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Friend request accepted!"));
    }

    @Test
    @WithMockUser("test")
    void testAddFriend() throws Exception{
		when(userService.findCurrentUser()).thenReturn(sender);
        when(userService.findById(receiverId)).thenReturn(receiver);

		mockMvc.perform(post(BASE_URL + "/addFriend/{userId}", receiverId).with(csrf())).andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Friend request sent!"));
    }

    @Test
    @WithMockUser("test")
    void testDenyFriend() throws Exception{
        when(userService.findCurrentUser()).thenReturn(sender);
        when(friendRequestService.findById(friendRequestId)).thenReturn(friendRequest);

        mockMvc.perform(delete(BASE_URL + "/{requestId}/deny", friendRequestId).with(csrf())).andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Friend request denied!"));
    }

    @Test
    @WithMockUser("test")
    void testGetFriends() throws Exception{
        when(userService.findCurrentUser()).thenReturn(receiver);
        when(friendRequestService.getFriends(receiver)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL)).andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    @WithMockUser("test")
    void testGetPendingFriendRequests() throws Exception{
        when(userService.findCurrentUser()).thenReturn(receiver);
        when(friendRequestService.findPendingRequests(receiver)).thenReturn(List.of(friendRequest));

        mockMvc.perform(get(BASE_URL + "/pending")).andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(1)).andExpect(jsonPath("$[?(@.id == 3)].receiver.username").value("receiver"));
    }

    @Test
    @WithMockUser("test")
    void testRemoveFriend() throws Exception{
        when(userService.findCurrentUser()).thenReturn(sender);
        when(userService.findById(receiverId)).thenReturn(receiver);
        when(friendRequestService.findRequestByUsers(sender, receiver)).thenReturn(friendRequest);

        mockMvc.perform(delete(BASE_URL + "/{userId}/remove", receiverId).with(csrf())).andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("You are no longer friends"));
    }

    @Test
    @WithMockUser("test")
    void shouldNotFindUser() throws Exception{
        when(userService.findCurrentUser()).thenThrow(ResourceNotFoundException.class);
        when(userService.findById(receiverId)).thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(delete(BASE_URL + "/{userId}/remove", receiverId).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(post(BASE_URL + "/addFriend/{userId}", receiverId).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE_URL + "/pending")).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE_URL)).andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE_URL + "/{requestId}/deny", friendRequestId).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(patch(BASE_URL + "/{requestId}/accept", friendRequestId).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("test")
    void shouldNotFindRequest() throws Exception{
        when(userService.findCurrentUser()).thenReturn(sender);
        when(userService.findById(receiverId)).thenReturn(receiver);
        when(friendRequestService.findById(friendRequestId)).thenThrow(ResourceNotFoundException.class);
        when(friendRequestService.findRequestByUsers(sender, receiver)).thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(delete(BASE_URL + "/{requestId}/deny", friendRequestId).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(patch(BASE_URL + "/{requestId}/accept", friendRequestId).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE_URL + "/{userId}/remove", receiverId).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("test")
    void shouldAlreadyBeFriends() throws Exception{
        when(userService.findCurrentUser()).thenReturn(sender);
        when(userService.findById(receiverId)).thenReturn(receiver);
        when(friendRequestService.findById(friendRequestId)).thenReturn(friendRequest);

        doThrow(new IllegalStateException()).when(friendRequestService).acceptFriendRequest(sender, friendRequest);
        doThrow(new IllegalStateException()).when(friendRequestService).sendFriendRequest(sender, receiver);
        doThrow(new IllegalStateException()).when(friendRequestService).deleteFriendRequest(sender, friendRequest);

        mockMvc.perform(patch(BASE_URL + "/{requestId}/accept", friendRequestId).with(csrf())).andExpect(status().isConflict());
        mockMvc.perform(post(BASE_URL + "/addFriend/{userId}", receiverId).with(csrf())).andExpect(status().isConflict());
        mockMvc.perform(delete(BASE_URL + "/{requestId}/deny", friendRequestId).with(csrf())).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser("test")
    void shouldBeForbidden() throws Exception{
        when(userService.findCurrentUser()).thenReturn(receiver);
        when(friendRequestService.findById(friendRequestId)).thenReturn(friendRequest);
        

        doThrow(new AccessDeniedException()).when(friendRequestService).acceptFriendRequest(receiver, friendRequest);
        doThrow(new AccessDeniedException()).when(friendRequestService).deleteFriendRequest(receiver, friendRequest);

        mockMvc.perform(patch(BASE_URL + "/{requestId}/accept", friendRequestId).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE_URL + "/{requestId}/deny", friendRequestId).with(csrf())).andExpect(status().isForbidden());

    }

    @Test
    @WithMockUser("test")
    void shouldBeForbiddenRemove() throws Exception{
        when(userService.findCurrentUser()).thenReturn(sender);
        when(userService.findById(receiverId)).thenReturn(receiver);
        when(friendRequestService.findRequestByUsers(sender, receiver)).thenReturn(friendRequest);

        doThrow(new AccessDeniedException()).when(friendRequestService).removeFriend(sender, friendRequest);

        mockMvc.perform(delete(BASE_URL + "/{userId}/remove", receiverId).with(csrf())).andExpect(status().isForbidden());

    }
}

