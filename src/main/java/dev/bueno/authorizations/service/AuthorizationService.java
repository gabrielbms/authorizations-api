package dev.bueno.authorizations.service;

import dev.bueno.authorizations.domain.Authorization;
import dev.bueno.authorizations.domain.DateRange;
import dev.bueno.authorizations.dto.PaginatedResponse;
import dev.bueno.authorizations.repository.AuthorizationRepository;
import dev.bueno.authorizations.util.CursorUtil;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.List;

@Service
public class AuthorizationService {

    private final AuthorizationRepository repository;

    public AuthorizationService(AuthorizationRepository repository) {
        this.repository = repository;
    }

    public Authorization create(String userId, String status) {
        Authorization auth = new Authorization();
        auth.setUserId(userId);
        auth.setStatus(status);
        return repository.save(auth);
    }

    public PaginatedResponse<Authorization> search(String userId, String status, String startDate, String endDate, String nextToken) {

        DateRange range = new DateRange(startDate, endDate);

        var startKey = CursorUtil.decode(nextToken);

        Page<Authorization> page = repository.search(userId, status, range.start(), range.end(), startKey);

        if (page == null) {
            return new PaginatedResponse<>(List.of(), null);
        }

        String newNextToken = CursorUtil.encode(page.lastEvaluatedKey());
        return new PaginatedResponse<>(page.items(), newNextToken);
    }

}