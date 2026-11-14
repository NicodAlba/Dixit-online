package es.us.dp1.lx_xy_24_25.dixit.achievement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Epic("Achievements")
@Feature("Play achievements")
public class PlayAchievementTest {

    @Test
    @DisplayName("Should unlock achievement")
    @Description("Should unlock the achievement after the threshold set is reached")
    @Transactional
    public void unlockPlayAchievements(){
        PlayAchievement playAchievement = new PlayAchievement();
        playAchievement.setName("PLAY");
        playAchievement.setDescription("Play description");
        playAchievement.setThreshold(2);

        long beforeUnlock = System.currentTimeMillis();
        playAchievement.unlockPlayAchievements(2);
        long afterUnlock = System.currentTimeMillis();
        assertNotNull(playAchievement.getDateObtained());
        long obtained = playAchievement.getDateObtained().getTime();
        assertTrue(obtained >= beforeUnlock && obtained <= afterUnlock,
            "Achievement timestamp must fall within the unlock operation");
    }

    @Test
    @DisplayName("Should not unlock achievement")
    @Transactional
    public void stillLockedPlayAchievement(){
        PlayAchievement playAchievement = new PlayAchievement();
        playAchievement.setName("PLAY");
        playAchievement.setDescription("Play description");
        playAchievement.setThreshold(2);

        playAchievement.unlockPlayAchievements(1);
        assertEquals(null, playAchievement.getDateObtained());
    }
}
