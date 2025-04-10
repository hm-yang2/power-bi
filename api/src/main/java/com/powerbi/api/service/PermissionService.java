package com.powerbi.api.service;

import com.powerbi.api.model.User;
import com.powerbi.api.repository.ChannelAdminRepository;
import com.powerbi.api.repository.ChannelMemberRepository;
import com.powerbi.api.repository.ChannelOwnerRepository;
import com.powerbi.api.repository.SuperUserRepository;
import com.powerbi.api.model.ChannelRole;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for managing user permissions within the system.
 * Provides methods to check user roles and permissions for specific channels,
 * including roles such as MEMBER, ADMIN, OWNER, and SUPER_USER.
 * 
 * This service interacts with repositories to verify user roles and determine
 * access levels for various operations.
 */
@Service
public class PermissionService {
    @Autowired
    private ChannelMemberRepository channelMemberRepository;
    @Autowired
    private ChannelAdminRepository channelAdminRepository;
    @Autowired
    private ChannelOwnerRepository channelOwnerRepository;
    @Autowired
    private SuperUserRepository superUserRepository;

    /**
     * Checks if the user is a superuser.
     *
     * @param user The user to check
     * @return true if the user is a superuser, false otherwise
     */
    @Transactional
    public boolean hasSuperUserPermission(User user) {
        return superUserRepository.existsByUserId(user.getId());
    }

    /**
     * Checks if the user has the required role in the channel.
     *
     * @param user The user to check
     * @param channelId The channel ID
     * @param requiredRole The required role (member, admin, owner, superuser)
     * @return true if the user has the required role, false otherwise
     */
    @Transactional
    public boolean hasChannelPermission(User user, Long channelId, ChannelRole requiredRole) {
        return switch (requiredRole) {
            case MEMBER -> channelMemberRepository.existsByUserIdAndChannelId(user.getId(), channelId);
            case ADMIN -> channelAdminRepository.existsByUserIdAndChannelId(user.getId(), channelId);
            case OWNER -> channelOwnerRepository.existsByUserIdAndChannelId(user.getId(), channelId);
            case SUPER_USER ->
                    hasSuperUserPermission(user);  // You can also directly call the hasSuperUserPermission method here
            case NOT_ALLOWED -> !(
                    channelMemberRepository.existsByUserIdAndChannelId(user.getId(), channelId) ||
                    channelAdminRepository.existsByUserIdAndChannelId(user.getId(), channelId) ||
                    channelOwnerRepository.existsByUserIdAndChannelId(user.getId(), channelId) ||
                    hasSuperUserPermission(user)
            );
            default -> false;
        };
    }

    /**
     * Retrieves the role of a user in a specific channel.
     *
     * @param user      the user to check
     * @param channelId the ID of the channel
     * @return the role of the user in the specified channel
     */
    @Transactional
    public ChannelRole getUserRoleInChannel(User user, Long channelId) {
        if (superUserRepository.existsByUserId(user.getId())) {
            return ChannelRole.SUPER_USER;
        }

        if (channelOwnerRepository.existsByUserIdAndChannelId(user.getId(), channelId)) {
            return ChannelRole.OWNER;
        }

        if (channelAdminRepository.existsByUserIdAndChannelId(user.getId(), channelId)) {
            return ChannelRole.ADMIN;
        }

        if (channelMemberRepository.existsByUserIdAndChannelId(user.getId(), channelId)) {
            return ChannelRole.MEMBER;
        }

        return ChannelRole.NOT_ALLOWED;
    }
}
