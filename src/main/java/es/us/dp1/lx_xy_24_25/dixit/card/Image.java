package es.us.dp1.lx_xy_24_25.dixit.card;

import es.us.dp1.lx_xy_24_25.dixit.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "images")
public class Image extends BaseEntity {

    @Column(name = "url")
    private String url;
}
