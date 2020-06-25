package com.nisovin.shopkeepers.user;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.nisovin.shopkeepers.api.user.User;

public interface UserStorage {

	public CompletableFuture<? extends User> loadUser(UUID playerId);

	public CompletableFuture<Void> saveUser(User user);
}
