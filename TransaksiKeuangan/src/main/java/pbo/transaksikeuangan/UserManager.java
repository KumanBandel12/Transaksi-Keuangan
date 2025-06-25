// src/main/java/pbo/transaksikeuangan/UserManager.java
package pbo.transaksikeuangan;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private static UserManager instance;
    private final Map<String, User> users = new HashMap<>();
    private static final String USER_DATA_FILE = "users.dat";

    private UserManager() {
        loadUsers();
    }

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USER_DATA_FILE))) {
            Object obj = ois.readObject();
            if (obj instanceof HashMap) {
                users.putAll((HashMap<String, User>) obj);
            }
        } catch (FileNotFoundException e) {
            // File belum ada, akan dibuat saat pengguna pertama mendaftar
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_DATA_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean registerUser(String username, String password) {
        if (users.containsKey(username)) {
            return false; // Username sudah ada
        }
        users.put(username, new User(username, password));
        saveUsers();
        return true;
    }

    public User validateUser(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
}