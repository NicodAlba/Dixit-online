package es.us.dp1.lx_xy_24_25.dixit.achievement;

import es.us.dp1.lx_xy_24_25.dixit.model.NamedEntity;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "achievements")
public class Achievement extends NamedEntity {

    @Column
    @NotBlank
    private String description;

    @Column
    private Date dateObtained;

    @ManyToOne
    @JoinColumn(name = "badge_image_id")
    private BadgeImage badgeImage;

    @Min(0)
    private Integer threshold;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    public String getActualDescription(){
        return description.replace("<THRESHOLD>",String.valueOf(threshold));
    }
}
