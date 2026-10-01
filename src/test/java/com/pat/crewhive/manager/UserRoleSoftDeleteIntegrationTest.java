package com.pat.crewhive.manager;

import com.pat.crewhive.support.AbstractIntegrationTest;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-database test for the {@code @SQLDelete} of {@link UserRole}: Hibernate binds the parameters of a
 * composite id in alphabetical order of its attributes, so a mapping test on the SQL text is not enough.
 */
class UserRoleSoftDeleteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private RoleAssignmentService roleAssignmentService;
    @Autowired
    private TransactionTemplate tx;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.execute("ALTER TABLE users ALTER COLUMN user_user_id DROP NOT NULL");
        clean();
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    private void clean() {
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM event_users");
        jdbc.execute("DELETE FROM event");
        jdbc.execute("DELETE FROM user_role");
        jdbc.execute("DELETE FROM users");
        jdbc.execute("DELETE FROM role WHERE company_id IS NULL AND role_name = 'ROLE_SQLDELETE_TEST'");
    }

    @Test
    void resetToBaseRole_softDeletesTheRemovedRoleLinkAndKeepsTheBaseOne() {
        User user = userRepository.saveAndFlush(new User("sqldelete@example.test", "Mario", "Rossi", "hashed-pwd"));
        Role extra = roleRepository.saveAndFlush(new Role("ROLE_SQLDELETE_TEST", null));

        tx.executeWithoutResult(status -> {
            User managed = userRepository.findById(user.getUserId()).orElseThrow();
            managed.addRole(roleRepository.findById(extra.getRoleId()).orElseThrow());
        });
        assertThat(links(user)).hasSize(1);

        tx.executeWithoutResult(status -> {
            User managed = userRepository.findById(user.getUserId()).orElseThrow();
            roleAssignmentService.resetToBaseRole(managed);
        });

        List<Map<String, Object>> links = links(user);
        assertThat(links).hasSize(2);
        Map<String, Object> removed = links.stream()
                .filter(l -> l.get("role_name").equals("ROLE_SQLDELETE_TEST")).findFirst().orElseThrow();
        assertThat(removed.get("active")).isEqualTo(false);
        assertThat(removed.get("deleted_at")).isNotNull();
        Map<String, Object> base = links.stream()
                .filter(l -> !l.get("role_name").equals("ROLE_SQLDELETE_TEST")).findFirst().orElseThrow();
        assertThat(base.get("active")).isEqualTo(true);
    }

    private List<Map<String, Object>> links(User user) {
        return jdbc.queryForList(
                "SELECT r.role_name, ur.active, ur.deleted_at FROM user_role ur "
                        + "JOIN role r ON r.role_id = ur.role_id WHERE ur.user_id = ?", user.getUserId());
    }
}
