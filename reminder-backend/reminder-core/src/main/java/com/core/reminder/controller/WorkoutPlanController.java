package com.core.reminder.controller;

import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.WorkoutPlan;
import com.common.reminder.model.WorkoutPlanItem;
import com.core.reminder.repository.WorkoutPlanItemRepository;
import com.core.reminder.repository.WorkoutPlanRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/workout-plans")
public class WorkoutPlanController {

    @Autowired
    private WorkoutPlanRepository planRepository;

    @Autowired
    private WorkoutPlanItemRepository itemRepository;

    @GetMapping
    public ResponseEntity<List<WorkoutPlan>> list(@RequestAttribute("currentUser") UserProfileDto currentUser,
                                                  @RequestParam(value = "keyword", required = false) String keyword) {
        List<WorkoutPlan> list = planRepository.searchAccessible(currentUser.getId(), emptyToNull(keyword));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanWithItems> get(@RequestAttribute("currentUser") UserProfileDto currentUser,
                                             @PathVariable Long id) {
        Optional<WorkoutPlan> optional = planRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        WorkoutPlan plan = optional.get();
        if (!Boolean.TRUE.equals(plan.getIsPublic()) && !currentUser.getId().equals(plan.getOwnerUserId())) {
            return ResponseEntity.status(403).build();
        }
        List<WorkoutPlanItem> items = itemRepository.findByPlanIdOrderByOrderIndexAsc(plan.getId());
        return ResponseEntity.ok(new PlanWithItems(plan, items));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<PlanWithItems> create(@RequestAttribute("currentUser") UserProfileDto currentUser,
                                                @Valid @RequestBody PlanWithItems payload) {
        WorkoutPlan plan = payload.getPlan();
        plan.setId(null);
        plan.setOwnerUserId(currentUser.getId());
        if (plan.getIsPublic() == null) plan.setIsPublic(false);
        WorkoutPlan saved = planRepository.save(plan);

        List<WorkoutPlanItem> items = normalizeItems(payload.getItems(), saved.getId());
        if (!items.isEmpty()) {
            itemRepository.saveAll(items);
        }
        return ResponseEntity.ok(new PlanWithItems(saved, itemRepository.findByPlanIdOrderByOrderIndexAsc(saved.getId())));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<PlanWithItems> update(@RequestAttribute("currentUser") UserProfileDto currentUser,
                                                @PathVariable Long id,
                                                @Valid @RequestBody PlanWithItems payload) {
        Optional<WorkoutPlan> optional = planRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        WorkoutPlan existing = optional.get();
        if (!Boolean.TRUE.equals(existing.getIsPublic()) && !currentUser.getId().equals(existing.getOwnerUserId())) {
            return ResponseEntity.status(403).build();
        }

        WorkoutPlan p = payload.getPlan();
        if (p.getName() != null) existing.setName(p.getName());
        if (p.getDescription() != null) existing.setDescription(p.getDescription());
        if (p.getIsPublic() != null) existing.setIsPublic(p.getIsPublic());
        WorkoutPlan saved = planRepository.save(existing);

        // 覆盖式更新条目
        itemRepository.deleteByPlanId(saved.getId());
        List<WorkoutPlanItem> items = normalizeItems(payload.getItems(), saved.getId());
        if (!items.isEmpty()) {
            itemRepository.saveAll(items);
        }
        return ResponseEntity.ok(new PlanWithItems(saved, itemRepository.findByPlanIdOrderByOrderIndexAsc(saved.getId())));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@RequestAttribute("currentUser") UserProfileDto currentUser,
                                       @PathVariable Long id) {
        Optional<WorkoutPlan> optional = planRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        WorkoutPlan existing = optional.get();
        if (!Boolean.TRUE.equals(existing.getIsPublic()) && !currentUser.getId().equals(existing.getOwnerUserId())) {
            return ResponseEntity.status(403).build();
        }
        itemRepository.deleteByPlanId(existing.getId());
        planRepository.deleteById(existing.getId());
        return ResponseEntity.noContent().build();
    }

    private List<WorkoutPlanItem> normalizeItems(List<WorkoutPlanItem> rawItems, Long planId) {
        List<WorkoutPlanItem> items = new ArrayList<>();
        if (rawItems == null || rawItems.isEmpty()) return items;
        int idx = 1;
        for (WorkoutPlanItem it : rawItems) {
            WorkoutPlanItem ni = new WorkoutPlanItem();
            ni.setId(null);
            ni.setPlanId(planId);
            ni.setOrderIndex(it.getOrderIndex() != null ? it.getOrderIndex() : idx);
            ni.setExerciseId(it.getExerciseId());
            ni.setReps(it.getReps() != null ? it.getReps() : 11);
            ni.setSets(it.getSets() != null ? it.getSets() : 3);
            ni.setRestSec(it.getRestSec() != null ? it.getRestSec() : 30);
            items.add(ni);
            idx++;
        }
        return items;
    }

    private String emptyToNull(String val) {
        return (val == null || val.trim().isEmpty()) ? null : val;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanWithItems {
        private WorkoutPlan plan;
        private List<WorkoutPlanItem> items;
    }
}


