package Hospitality_and_tourism_system;

/**
 * Remembers who is currently logged in, so every screen can show
 * the details of the correct user.
 */
public final class UserSession {

    private static String employeeId;
    private static String username;
    private static String role;

    private UserSession() { }

    public static void login(String empId, String user, String userRole) {
        employeeId = empId;
        username = user;
        role = userRole;
    }

    public static void logout() {
        employeeId = null;
        username = null;
        role = null;
    }

    public static String getEmployeeId() { return employeeId; }
    public static String getUsername()   { return username; }
    public static String getRole()       { return role; }
    public static boolean isLoggedIn()   { return employeeId != null; }
}
