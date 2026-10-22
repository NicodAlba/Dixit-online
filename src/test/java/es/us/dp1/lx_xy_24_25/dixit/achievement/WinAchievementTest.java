package es.us.dp1.lx_xy_24_25.dixit.achievement;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Epic("Achievements")
@Feature("Win achievements")
public class WinAchievementTest {

    @Test
    @DisplayName("Should unlock achievement")
    @Description("Should unlock the achievement after the threshold set is reached")
    @Transactional
    public void unlockWinAchievements(){
        WinAchievement winAchievement = new WinAchievement();
        winAchievement.setName("WIN");
        winAchievement.setDescription("Win description");
        winAchievement.setThreshold(2);

        winAchievement.unlockWinAchievements(2);
        assertEquals(new Date(), winAchievement.getDateObtained());
    }

    @Test
    @DisplayName("Should not unlock achievement")
    @Transactional
    public void stillLockedWinAchievement(){
         WinAchievement winAchievement = new WinAchievement();
        winAchievement.setName("WIN");
        winAchievement.setDescription("Win description");
        winAchievement.setThreshold(2);

        winAchievement.unlockWinAchievements(1);
        assertEquals(null, winAchievement.getDateObtained());
    }
}
