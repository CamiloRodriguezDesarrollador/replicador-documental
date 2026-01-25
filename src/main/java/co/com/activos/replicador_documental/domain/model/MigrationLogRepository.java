package co.com.activos.replicador_documental.domain.model;

import java.util.List;

public interface MigrationLogRepository {
    void save(MigrationLog migrationLog);
    void saveAll(List<MigrationLog> migrationLogs);
}
