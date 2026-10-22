package es.us.dp1.lx_xy_24_25.dixit.achievement;

import static org.junit.Assert.assertEquals;

import java.util.List;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import es.us.dp1.lx_xy_24_25.dixit.user.AuthoritiesService;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;

@SpringBootTest
@Epic("Achievements")
public class AchievementRepositoryTest {
    @Autowired
    private AchievementRepository achievementRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthoritiesService authService;

    @Test
    @DisplayName("Should find the achievements of a user by their type")
    @Transactional
    public void findByUserAndType(){
        User user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("password");
        user1.setAuthority(authService.findByAuthority("PLAYER"));
        userService.saveUser(user1);

        WinAchievement wAchievement = new WinAchievement();
        wAchievement.setName("WIN");
        wAchievement.setDescription("Win description");
        wAchievement.setThreshold(2);
        wAchievement.setUser(user1);
        achievementRepository.save(wAchievement);

        PlayAchievement pAchievement = new PlayAchievement();
        pAchievement.setName("PLAY");
        pAchievement.setDescription("Play description");
        pAchievement.setThreshold(2);
        pAchievement.setUser(user1);
        achievementRepository.save(pAchievement);

        List<Achievement> wAchievements = achievementRepository.findByUserAndType(user1, WinAchievement.class);
        List<Achievement> pAchievements = achievementRepository.findByUserAndType(user1, PlayAchievement.class);

        assertEquals(1, wAchievements.size());
        assertEquals(1, pAchievements.size());
    }
}
