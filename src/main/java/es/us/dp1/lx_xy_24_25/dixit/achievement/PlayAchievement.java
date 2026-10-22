package es.us.dp1.lx_xy_24_25.dixit.achievement;

import java.util.Date;

import jakarta.persistence.Entity;

@Entity
public class PlayAchievement extends Achievement{
    public void unlockPlayAchievements(Integer totalPlayed){   
        if (getDateObtained() == null && totalPlayed >= getThreshold()){
            setDateObtained(new Date());
        }
    }

}
