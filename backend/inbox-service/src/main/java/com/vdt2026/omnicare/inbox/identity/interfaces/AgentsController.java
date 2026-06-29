package com.vdt2026.omnicare.inbox.identity.interfaces;

import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents")
class AgentsController {

    private final AgentRepository agentRepository;

    AgentsController(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @GetMapping
    List<AgentResponse> listAgents() {
        return agentRepository.findByActiveTrueOrderByDisplayNameAsc()
            .stream()
            .map(AgentResponse::from)
            .toList();
    }
}
