package xyz.nardone.aide.largescale.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.nardone.aide.largescale.constant.ERole;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {
    private ERole name;
}
