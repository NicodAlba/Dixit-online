/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.us.dp1.lx_xy_24_25.dixit.user;

import es.us.dp1.lx_xy_24_25.dixit.achievement.Achievement;
import es.us.dp1.lx_xy_24_25.dixit.achievement.AchievementService;
import es.us.dp1.lx_xy_24_25.dixit.auth.payload.response.MessageResponse;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.InvalidRequestException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.util.RestPreconditions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User")
@SecurityRequirement(name = "bearerAuth")
class UserRestController {

	private final UserService userService;
	private final AuthoritiesService authService;
	private final AchievementService achievementService;

	@Autowired
	public UserRestController(UserService userService, AuthoritiesService authService, AchievementService achievementService) {
		this.userService = userService;
		this.authService = authService;
		this.achievementService = achievementService;
	}

    private UserView view(User user) {
        User actor = userService.findCurrentUser();
        return UserView.of(user, actor.hasAuthority("ADMIN") || Objects.equals(actor.getId(), user.getId()));
    }

	@GetMapping
    @Operation(summary = "Retrieves all users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200")
    })
	public ResponseEntity<List<UserView>> findAll(@RequestParam(required = false) String auth) {
		List<User> res;
		if (auth != null) {
			res = (List<User>) userService.findAllByAuthority(auth);
		} else
			res = (List<User>) userService.findAll();
		return new ResponseEntity<>(res.stream().map(u -> UserView.of(u, true)).toList(), HttpStatus.OK);
	}

    @GetMapping("/players")
    public ResponseEntity<Page<UserView>> findPlayers(@RequestParam(required = false) String username, Pageable pageable) {
        if (username == null || username.isBlank()) {
            throw new InvalidRequestException("Username cannot be empty");
        }
        return new ResponseEntity<>(userService.findPlayersByUsername(username, pageable).map(u -> UserView.of(u, false)), HttpStatus.OK);
    }

	@GetMapping("/{userId}/achievements")
	public ResponseEntity<List<Achievement>> findAchievementsById(@PathVariable("userId") Integer id) {
		return new ResponseEntity<>(achievementService.findAllAchievementsByUserId(id), HttpStatus.OK);
	}

	@GetMapping("authorities")
    @Operation(summary = "Retrieves all authorities")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200")
    })
	public ResponseEntity<List<Authorities>> findAllAuths() {
		List<Authorities> res = (List<Authorities>) authService.findAll();
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@GetMapping(value = "{id}")
    @Operation(summary = "Gets an user by its id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404")
    })
	public ResponseEntity<UserView> findById(@PathVariable("id") Integer id) {
        User user;
        try {
            user = userService.findById(id);
            return new ResponseEntity<>(view(user), HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
	}

	@PostMapping
	@Operation(summary = "Saves the given user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200")
    })
	public ResponseEntity<UserView> create(@RequestBody @Valid User user) {
		User savedUser = userService.createAccount(user);
		return new ResponseEntity<>(view(savedUser), HttpStatus.CREATED);
	}

	@PutMapping(value = "{userId}")
	@Operation(summary = "Updates the given user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404")
    })
	public ResponseEntity<UserView> update(@PathVariable("userId") Integer id, @RequestBody @Valid UserDTO user) {
        try {
            RestPreconditions.checkNotNull(userService.findById(id), "User", "ID", id);
            return new ResponseEntity<>(view(this.userService.updateUser(user, id)), HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

	@DeleteMapping(value = "{userId}")
    @Operation(summary = "Deletes the given user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404")
    })
	public ResponseEntity<MessageResponse> delete(@PathVariable("userId") int id) {
		RestPreconditions.checkNotNull(userService.findById(id), "User", "ID", id);
        User currentUser = userService.findCurrentUser();
		if (currentUser.getId() != id) {
			userService.deleteUser(id);
			return new ResponseEntity<>(new MessageResponse("User deleted!"), HttpStatus.OK);
		} else
			throw new AccessDeniedException("You can't delete yourself!");
	}

}
