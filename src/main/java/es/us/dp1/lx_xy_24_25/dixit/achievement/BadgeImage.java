package es.us.dp1.lx_xy_24_25.dixit.achievement;

import es.us.dp1.lx_xy_24_25.dixit.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "badge_images")
public class BadgeImage extends BaseEntity{
    @Column(name = "url")
    private String url;

    @Column
    @Enumerated(EnumType.STRING)
    AchievementType type;
}
