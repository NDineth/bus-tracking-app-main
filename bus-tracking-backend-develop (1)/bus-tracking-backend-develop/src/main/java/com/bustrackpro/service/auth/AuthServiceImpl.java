package com.bustrackpro.service.auth;

import com.bustrackpro.config.jwt.JwtTokenProvider;
import com.bustrackpro.dto.request.user.LoginRequest;
import com.bustrackpro.dto.request.user.SignUpRequest;
import com.bustrackpro.dto.response.user.JwtAuthenticationResponse;
import com.bustrackpro.modal.NotificationCategory;
import com.bustrackpro.modal.bus.BusDriver;
import com.bustrackpro.modal.user.UserRole;
import com.bustrackpro.modal.user.Users;
import com.bustrackpro.repository.schedule.BusDriverRepository;
import com.bustrackpro.repository.user.UserRepository;
import com.bustrackpro.service.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Auth service implementation.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final BusDriverRepository busDriverRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final NotificationService notificationService;

    /**
     * Instantiates a new Auth service impl.
     */
    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           BusDriverRepository busDriverRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           NotificationService notificationService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.busDriverRepository = busDriverRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.notificationService = notificationService;
    }

    @Override
    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        return tokenProvider.generateToken(authentication);
    }

    @Override
    @Transactional
    public boolean registerUser(SignUpRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return false;
        }

        Users user = new Users();
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setContactNumber(signUpRequest.getContactNumber());
        user.setRole(signUpRequest.getRole());

        Users savedUser = userRepository.save(user);

        // Auto initialize BusDriver entity if role is BUS_DRIVER
        if (signUpRequest.getRole() == UserRole.BUS_DRIVER) {
            BusDriver busDriver = new BusDriver();
            busDriver.setLicenseNumber(signUpRequest.getLicenseNumber());
            busDriver.setUsers(savedUser);
            busDriverRepository.save(busDriver);
        }

        // Send welcome notification
        String title = "Welcome to BusTrackPro!";
        String body = "Hi " + savedUser.getFirstName() + ", thank you for joining us.";
        notificationService.createNotification(
                title,
                body,
                NotificationCategory.SYSTEM_ALERT,
                "System",
                Collections.singletonList(savedUser.getId())
        );

        return true;
    }

    @Override
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }
}
