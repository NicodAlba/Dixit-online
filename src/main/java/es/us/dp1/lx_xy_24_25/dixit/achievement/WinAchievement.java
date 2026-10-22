package es.us.dp1.lx_xy_24_25.dixit.achievement;

import java.util.Date;

import jakarta.persistence.Entity;

@Entity
public class WinAchievement extends Achievement{
   
    public void unlockWinAchievements(Integer totalWins){
        if (getDateObtained() == null && totalWins >= getThreshold()){
            setDateObtained(new Date());
        }
    }

}
