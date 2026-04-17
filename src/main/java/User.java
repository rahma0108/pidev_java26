public class User {

    private int id;
    private String email;
    private String password;
    private String fullName;
    private String roles;
    private String status;
    private String phone;

    public User() {}

    public User(String email, String password, String fullName, String roles, String status) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.roles = roles;
        this.status = status;
    }

    // Getters
    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getRoles() { return roles; }
    public String getStatus() { return status; }
    public String getPhone() { return phone; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRoles(String roles) { this.roles = roles; }
    public void setStatus(String status) { this.status = status; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return fullName + " (" + email + ")";
    }
}