package com.baatcheat;

import android.net.Uri;

import com.baatcheat.model.User;

import java.io.IOException;
import java.util.Set;

public interface FirestoreDataInterface {
    void onFirestoreCallBack(Set<User> usersAlreadyFound);
    void onUriObtained(Uri obtainedUri);

    void onUserCallback(User retrievedUser) throws IOException;
}
