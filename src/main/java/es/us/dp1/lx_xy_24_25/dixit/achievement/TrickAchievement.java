package es.us.dp1.lx_xy_24_25.dixit.achievement;

import java.util.Date;

import jakarta.persistence.Entity;

@Entity
public class TrickAchievement extends Achievement{
    public void unlockTrickAchievements(Integer votes){
        if (getDateObtained() == null && votes >= getThreshold()){
            setDateObtained(new Date());
        }
    }
}
