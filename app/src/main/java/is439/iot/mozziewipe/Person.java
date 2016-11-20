package is439.iot.mozziewipe;

import com.google.firebase.auth.FirebaseUser;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Person implements Serializable {

    private Map userMap;

    public Person(FirebaseUser user) {
        this(user.getUid(), user.getDisplayName(), user.getEmail(), 0, 50);
    }

    public Person(String id, String displayName, String email, int points, int energy) {
        userMap = new HashMap();
        userMap.put("id", id);
        userMap.put("displayName", displayName);
        userMap.put("email", email);
        userMap.put("points", points);
        userMap.put("energy", energy);
    }

    public String getDisplayName() {
        return (String) userMap.get("displayName");
    }

    public String getPersonEmail() {
        return (String) userMap.get("email");
    }

    public String getPersonID() {
        return (String) userMap.get("id");
    }

    public int getEnergy() {
        return (int) userMap.get("energy");
    }

    public void setEnergy(int energy) {
        userMap.put("energy", energy);
    }

    public int getPoints() {
        return (int) userMap.get("points");
    }

    public void setPoints(int points) {
        userMap.put("points", points);
    }

    public Map getPersonMap() {
        return userMap;
    }

}
