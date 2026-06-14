package xyz.nardone.aide.largescale.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import xyz.nardone.aide.largescale.constant.EOrganizationStatus;
import xyz.nardone.aide.largescale.entity.AdministratorEntity;
import xyz.nardone.aide.largescale.entity.DonorEntity;
import xyz.nardone.aide.largescale.entity.OrganizationEntity;
import xyz.nardone.aide.largescale.entity.UserEntity;
import xyz.nardone.aide.largescale.entity.embedded.LocationEntity;

import java.util.Collection;
import java.util.List;

public class UserDetailsImpl implements UserDetails {

    private final String id;
    private final LocationEntity location;
    private final String firstName;
    private final String lastName;
    private final String legalName;
    private final EOrganizationStatus organizationStatus;
    private final String displayName;
    private final String email;
    @JsonIgnore
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserDetailsImpl(
            String id,
            String displayName,
            String firstName,
            String lastName,
            String legalName,
            EOrganizationStatus organizationStatus,
            LocationEntity location,
            String email,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.displayName = displayName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.legalName = legalName;
        this.organizationStatus = organizationStatus;
        this.location = location;
        this.email = email;
        this.password = password;
        this.authorities = List.copyOf(authorities);
    }

    public static UserDetailsImpl NaturalPerson(
            String id,
            String displayName,
            String firstName,
            String lastName,
            LocationEntity location,
            String email,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {
        return new UserDetailsImpl(
                id,
                displayName,
                firstName,
                lastName,
                null,
                null,
                location,
                email,
                password,
                authorities
        );
    }

    public static UserDetailsImpl OrganizationAccount(
            String id,
            String displayName,
            String legalName,
            EOrganizationStatus organizationStatus,
            LocationEntity location,
            String email,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {
        return new UserDetailsImpl(
                id,
                displayName,
                null,
                null,
                legalName,
                organizationStatus,
                location,
                email,
                password,
                authorities
        );
    }

    public static UserDetailsImpl build(UserEntity user) {
        Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.getRole().getName().name()));

        // Preserve subtype-specific profile fields for controllers that read the principal.
        if (user instanceof OrganizationEntity organization) {
            return OrganizationAccount(
                    organization.getId(),
                    organization.getDisplayName(),
                    organization.getLegalName(),
                    organization.getStatus(),
                    organization.getLocation(),
                    organization.getEmail(),
                    organization.getPassword(),
                    authorities
            );
        }

        if (user instanceof DonorEntity donor) {
            return NaturalPerson(
                    donor.getId(),
                    donor.getDisplayName(),
                    donor.getFirstName(),
                    donor.getLastName(),
                    donor.getLocation(),
                    donor.getEmail(),
                    donor.getPassword(),
                    authorities
            );
        }

        if (user instanceof AdministratorEntity administrator) {
            return NaturalPerson(
                    administrator.getId(),
                    administrator.getDisplayName(),
                    administrator.getFirstName(),
                    administrator.getLastName(),
                    administrator.getLocation(),
                    administrator.getEmail(),
                    administrator.getPassword(),
                    authorities
            );
        }

        throw new IllegalArgumentException("Unsupported user type: " + user.getClass().getName());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getId() {
        return id;
    }

    public LocationEntity getLocation() {
        return location;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLegalName() {
        return legalName;
    }

    public EOrganizationStatus getOrganizationStatus() {
        return organizationStatus;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }
}
