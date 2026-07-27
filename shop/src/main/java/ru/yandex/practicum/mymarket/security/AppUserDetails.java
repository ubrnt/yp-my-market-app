package ru.yandex.practicum.mymarket.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.yandex.practicum.mymarket.domain.User;

public class AppUserDetails implements UserDetails {

    private final User user;

    public AppUserDetails(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return user.getId();
    }

    public Long getAccountId() {
        return user.getAccountId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }
}
