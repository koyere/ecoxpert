package me.koyere.ecoxpert.modules.professions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ProfessionsManager {
    CompletableFuture<Optional<ProfessionRole>> getRole(UUID player);
    CompletableFuture<Boolean> setRole(UUID player, ProfessionRole role);
    CompletableFuture<Integer> getLevel(UUID player);
    CompletableFuture<Boolean> setLevel(UUID player, int level);
    CompletableFuture<Boolean> canChange(UUID player);
    List<ProfessionRole> getAvailableRoles();
}
