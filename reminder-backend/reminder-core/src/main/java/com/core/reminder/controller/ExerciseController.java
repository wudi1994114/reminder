package com.core.reminder.controller;

import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.Exercise;
import com.core.reminder.repository.ExerciseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    @Autowired
    private ExerciseRepository exerciseRepository;

    @GetMapping
    public ResponseEntity<List<Exercise>> list(@RequestAttribute("currentUser") UserProfileDto currentUser,
                                               @RequestParam(value = "keyword", required = false) String keyword,
                                               @RequestParam(value = "muscleGroup", required = false) String muscleGroup) {
        Long userId = currentUser.getId();
        List<Exercise> list = exerciseRepository.searchAccessible(userId, emptyToNull(keyword), emptyToNull(muscleGroup));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Exercise> get(@RequestAttribute("currentUser") UserProfileDto currentUser,
                                        @PathVariable Long id) {
        Optional<Exercise> optional = exerciseRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Exercise e = optional.get();
        if (!Boolean.TRUE.equals(e.getIsPublic()) && !currentUser.getId().equals(e.getOwnerUserId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(e);
    }

    @PostMapping
    public ResponseEntity<Exercise> create(@RequestAttribute("currentUser") UserProfileDto currentUser,
                                           @Valid @RequestBody Exercise payload) {
        payload.setId(null);
        if (payload.getOwnerUserId() == null) {
            payload.setOwnerUserId(currentUser.getId());
        }
        if (payload.getDefaultReps() == null) payload.setDefaultReps(11);
        if (payload.getDefaultSets() == null) payload.setDefaultSets(3);
        if (payload.getDefaultRestSec() == null) payload.setDefaultRestSec(30);
        if (payload.getIsPublic() == null) payload.setIsPublic(Boolean.TRUE);
        Exercise saved = exerciseRepository.save(payload);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Exercise> update(@RequestAttribute("currentUser") UserProfileDto currentUser,
                                           @PathVariable Long id,
                                           @Valid @RequestBody Exercise payload) {
        Optional<Exercise> optional = exerciseRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Exercise existing = optional.get();
        if (!Boolean.TRUE.equals(existing.getIsPublic()) && !currentUser.getId().equals(existing.getOwnerUserId())) {
            return ResponseEntity.status(403).build();
        }
        existing.setName(payload.getName() != null ? payload.getName() : existing.getName());
        existing.setMuscleGroup(payload.getMuscleGroup() != null ? payload.getMuscleGroup() : existing.getMuscleGroup());
        existing.setEquipment(payload.getEquipment() != null ? payload.getEquipment() : existing.getEquipment());
        if (payload.getDefaultReps() != null) existing.setDefaultReps(payload.getDefaultReps());
        if (payload.getDefaultSets() != null) existing.setDefaultSets(payload.getDefaultSets());
        if (payload.getDefaultRestSec() != null) existing.setDefaultRestSec(payload.getDefaultRestSec());
        if (payload.getAudioUrl() != null) existing.setAudioUrl(payload.getAudioUrl());
        if (payload.getIsPublic() != null) existing.setIsPublic(payload.getIsPublic());
        Exercise saved = exerciseRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestAttribute("currentUser") UserProfileDto currentUser,
                                       @PathVariable Long id) {
        Optional<Exercise> optional = exerciseRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Exercise existing = optional.get();
        if (!Boolean.TRUE.equals(existing.getIsPublic()) && !currentUser.getId().equals(existing.getOwnerUserId())) {
            return ResponseEntity.status(403).build();
        }
        exerciseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String emptyToNull(String val) {
        return (val == null || val.trim().isEmpty()) ? null : val;
    }
}


