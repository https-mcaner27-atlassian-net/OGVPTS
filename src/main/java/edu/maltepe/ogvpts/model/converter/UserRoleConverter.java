package edu.maltepe.ogvpts.model.converter;

import edu.maltepe.ogvpts.model.enums.UserRole;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserRoleConverter extends LowercaseEnumConverter<UserRole> {

    public UserRoleConverter() {
        super(UserRole.class);
    }
}
