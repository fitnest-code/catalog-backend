package az.fitnest.catalog.configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

@Configuration
public class ApplicationWarmupConfig {
    private final DataSource dataSource;
    @Value(value = "${app.warmup.enabled:true}")
    private boolean warmupEnabled;
    @Value(value = "${app.warmup.db:true}")
    private boolean warmupDb;

    public ApplicationWarmupConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(value = {ApplicationReadyEvent.class})
    @Async
    public void warmupApplication() {
        if (!this.warmupEnabled) {
            return;
        }
        if (this.warmupDb) {
            this.warmupDatabase();
        }
        this.warmupJit();
    }

    private void warmupDatabase() {
        try {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 3; ++i) {
                try (Connection conn = this.dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                     ResultSet rs = stmt.executeQuery();) {
                    if (!rs.next()) continue;
                    rs.getInt(1);
                    continue;
                }
            }
        } catch (Exception e) {
        }
    }

    private void warmupJit() {
        try {
            long start = System.currentTimeMillis();
            String test = "warmup-test-string";
            test.toLowerCase();
            test.toUpperCase();
            test.split("-");
            ArrayList<String> list = new ArrayList<String>();
            list.add("test");
            list.stream().filter(s -> s != null).count();
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("key", "value");
            map.get("key");
        } catch (Exception e) {
        }
    }
}
