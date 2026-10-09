package org.misha.authservice.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.misha.authservice.exception.AppException;
import org.springframework.http.HttpStatus;
import java.util.function.Supplier;
@Service @RequiredArgsConstructor
public class IdempotencyService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final RentalWriteLock lock;
    @Transactional
    public <T> T execute(String key, Object request, Class<T> type, Supplier<T> action) {
        if (key == null || key.isBlank()) return action.get();
        if (key.length() > 100) throw new AppException("INVALID_KEY", "Invalid operation key", HttpStatus.BAD_REQUEST);
        lock.acquire();
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(mapper.writeValueAsBytes(java.util.List.of(type.getName(), request))));
            var saved = jdbc.queryForList("SELECT request_hash, response_json FROM operation_receipts WHERE actor_id=? AND operation_id=?", actor, key);
            if (!saved.isEmpty()) {
                if (!hash.equals(saved.get(0).get("request_hash")))
                    throw new AppException("IDEMPOTENCY_CONFLICT", "Operation key already used", HttpStatus.CONFLICT);
                return mapper.readValue(saved.get(0).get("response_json").toString(), type);
            }
            T result = action.get();
            jdbc.update("INSERT INTO operation_receipts(actor_id,operation_id,request_hash,response_json) VALUES (?,?,?,?)",
                actor, key, hash, mapper.writeValueAsString(result));
            return result;
        } catch (AppException ex) { throw ex; }
        catch (com.fasterxml.jackson.core.JsonProcessingException | java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot persist operation receipt", ex);
        }
    }
}
