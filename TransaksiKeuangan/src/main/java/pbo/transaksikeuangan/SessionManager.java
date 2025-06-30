// src/main/java/pbo/transaksikeuangan/SessionManager.java
package pbo.transaksikeuangan;

public class SessionManager {
    private static User currentUser;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("Tidak ada pengguna yang login.");
        }
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }
}