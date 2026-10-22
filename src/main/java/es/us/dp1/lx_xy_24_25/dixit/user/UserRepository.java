package es.us.dp1.lx_xy_24_25.dixit.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;


public interface UserRepository extends  CrudRepository<User, Integer>{


	Optional<User> findByUsername(String username);

	Boolean existsByUsername(String username);

	Optional<User> findById(Integer id);

	@Query("SELECT u FROM User u WHERE u.authority.authority = :auth")
	Iterable<User> findAllByAuthority(String auth);

@Query("SELECT u FROM User u WHERE (u.authority.authority = 'PLAYER' AND UPPER(u.username) LIKE UPPER(CONCAT('%', ?1, '%')) AND NOT u.id = ?2)")
    Page<User> findPlayersByUsernameLike(String username, int currentId, Pageable pageable);
}
