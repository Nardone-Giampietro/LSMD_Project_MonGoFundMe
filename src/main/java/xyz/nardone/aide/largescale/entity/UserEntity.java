package xyz.nardone.aide.largescale.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import xyz.nardone.aide.largescale.entity.embedded.LocationEntity;
import xyz.nardone.aide.largescale.entity.embedded.RoleEntity;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_account")
public abstract class UserEntity {

    @Id
    private String id;

    @Field("displayName")
    private String displayName;

    @Field("email")
    private String email;

    @Field("password")
    private String password;

    @Field("createdAt")
    private LocalDateTime createdAt;

    @Field("updatedAt")
    private LocalDateTime updatedAt;

    @Field("role")
    private RoleEntity role;

    @Field("location")
    private LocationEntity location;
}
