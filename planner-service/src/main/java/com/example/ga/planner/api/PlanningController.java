package com.example.ga.planner.api;

import com.example.ga.planner.service.PlanningService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping("/optimize")
    public ResponseEntity<PlanResponse> optimize(@RequestBody @Valid PlanRequest request) {
        return ResponseEntity.ok(planningService.generatePlan(request));
    }

    @PostMapping(value = "/optimize/fastjson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> optimizeFastJson(@RequestBody @Valid PlanRequest request) {
        return ResponseEntity.ok(planningService.generatePlanFastJson(request));
    }
}
