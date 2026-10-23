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

import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.social.FriendRequestService;
import jakarta.validation.Valid;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthoritiesRepository authoritiesRepository;
	private final PasswordEncoder passwordEncoder;
    private final FriendRequestService friendRequestService;

    @Autowired
	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, FriendRequestService friendRequestService, AuthoritiesRepository authoritiesRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
        this.friendRequestService = friendRequestService;
        this.authoritiesRepository = authoritiesRepository;
    }

     @Transactional
     public User saveUser(User user) throws DataAccessException {
		if (user.getPassword() != null && !user.getPassword().isBlank()) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		}
		return userRepository.save(user);
     }

	@Transactional(readOnly = true)
	public User findByUsername(String username) {
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
	}

	@Transactional(readOnly = true)
	public User findById(Integer id) {
		return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
	}

	@Transactional(readOnly = true)
	public User findCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null)
			throw new ResourceNotFoundException("Nobody authenticated!");
		else
			return userRepository.findByUsername(auth.getName())
					.orElseThrow(() -> new ResourceNotFoundException("User", "Username", auth.getName()));
	}

	public Boolean existsUser(String username) {
		return userRepository.existsByUsername(username);
	}

	@Transactional(readOnly = true)
	public Iterable<User> findAll() {
		return userRepository.findAll();
	}

	public Iterable<User> findAllByAuthority(String auth) {
		return userRepository.findAllByAuthority(auth);
	}

    @Transactional
    public User createAccount(User input) {
        if (!findCurrentUser().hasAuthority("ADMIN")) throw new AccessDeniedException();
        if (input.getId() != null) throw new IllegalArgumentException("A new account cannot have an id.");
        if (input.getUsername() == null || input.getUsername().isBlank()
                || input.getPassword() == null || input.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username and password are required.");
        }
        User account = new User();
        account.setUsername(input.getUsername());
        account.setPassword(input.getPassword());
        account.setAuthority(resolveAuthority(input.getAuthority()));
        return saveUser(account);
    }

    private Authorities resolveAuthority(Authorities authority) {
        if (authority == null || authority.getId() == null) {
            throw new IllegalArgumentException("An existing authority is required.");
        }
        return authoritiesRepository.findById(authority.getId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown authority."));
    }

    @Transactional
    public User updateUser(@Valid UserDTO input, Integer idToUpdate) {
        User actor = findCurrentUser();
        boolean ownAccount = Objects.equals(actor.getId(), idToUpdate);
        if (!ownAccount && !actor.hasAuthority("ADMIN")) throw new AccessDeniedException();
        User target = findById(idToUpdate);
        Authorities authority = input.getAuthority() == null ? target.getAuthority() : resolveAuthority(input.getAuthority());
        if (!actor.hasAuthority("ADMIN") && !Objects.equals(authority.getId(), target.getAuthority().getId())) {
            throw new AccessDeniedException("You cannot change your authority.");
        }
        boolean changingPassword = input.getPassword() != null && !input.getPassword().isBlank();
        if (changingPassword && !ownAccount) {
            throw new AccessDeniedException("Only the account holder can change its password.");
        }
        if (input.getUsername() == null || input.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        target.setUsername(input.getUsername());
        target.setAuthority(authority);
        if (changingPassword) target.setPassword(passwordEncoder.encode(input.getPassword()));
        return userRepository.save(target);
    }

	@Transactional
	public void deleteUser(Integer id) {
		User toDelete = findById(id);
        friendRequestService.removeAllRequests(toDelete);
		this.userRepository.delete(toDelete);
	}


    @Transactional(readOnly = true)
    public Page<User> findPlayersByUsername(String username, Pageable pageable) {
        int currentId = findCurrentUser().getId();
        return userRepository.findPlayersByUsernameLike(username, currentId, pageable);
    }
}
