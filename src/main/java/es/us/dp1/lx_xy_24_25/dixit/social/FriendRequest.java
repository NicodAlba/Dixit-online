package es.us.dp1.lx_xy_24_25.dixit.social;

import es.us.dp1.lx_xy_24_25.dixit.model.BaseEntity;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "friend_requests", uniqueConstraints = {@UniqueConstraint(columnNames = {"sender_id", "receiver_id"})})
public class FriendRequest extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "sender_id")
    User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    User receiver;

    @Column
    Date dateAccepted;

    public void setSender(User sender) {
        if (this.getReceiver() != sender) {
            this.sender = sender;
        }
    }

    public void setReceiver(User receiver) {
        if (this.sender != receiver) {
            this.receiver = receiver;
        }
    }
}
