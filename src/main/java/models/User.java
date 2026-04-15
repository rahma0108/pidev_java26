package models;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class User {

    public static final String ROLE_PATIENT = "PATIENT";
    public static final String ROLE_MEDECIN = "MEDECIN";

    private int id;
    private String fullName;
    private String email;
    private List<String> roles;
    private LocalTime preferredTime;
    private Integer maxDaysAhead;

    public User() {
        this.roles = new ArrayList<>();
    }

    public User(int id, String fullName, String email, List<String> roles,
                LocalTime preferredTime, Integer maxDaysAhead) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
        this.preferredTime = preferredTime;
        this.maxDaysAhead = maxDaysAhead;
    }

    public static List<String> parseRoles(String stored) {
        if (stored == null || stored.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(stored.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static String serializeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return String.join(",", roles);
    }

    public boolean hasRole(String role) {
        if (roles == null || role == null || role.isBlank()) {
            return false;
        }
        String expected = normalizeRole(role);
        for (String r : roles) {
            if (expected.equals(normalizeRole(r))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeRole(String role) {
        String cleaned = role == null ? "" : role.trim().toUpperCase()
                .replace("\"", "")
                .replace("[", "")
                .replace("]", "");
        if (cleaned.startsWith("ROLE_")) {
            cleaned = cleaned.substring("ROLE_".length());
        }
        return cleaned;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles == null ? Collections.emptyList() : Collections.unmodifiableList(roles);
    }

    public void setRoles(List<String> roles) {
        this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
    }

    public LocalTime getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(LocalTime preferredTime) {
        this.preferredTime = preferredTime;
    }

    public Integer getMaxDaysAhead() {
        return maxDaysAhead;
    }

    public void setMaxDaysAhead(Integer maxDaysAhead) {
        this.maxDaysAhead = maxDaysAhead;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                '}';
    }
}
