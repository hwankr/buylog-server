//package midas.buylog_backend;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//@Component
//public class DbTestRunner implements CommandLineRunner {
//
//    private final JdbcTemplate jdbcTemplate;
//
//    public DbTestRunner(JdbcTemplate jdbcTemplate) {
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    @Override
//    public void run(String... args) {
//        try {
//            // DB에 아주 가벼운 테스트용 질문(SELECT 1)을 던짐
//            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
//
//            System.out.println("\n===============================================");
//            System.out.println("Supabase DB 연결 성공 (응답값: " + result + ")");
//            System.out.println("===============================================\n");
//        } catch (Exception e) {
//            System.err.println("\nDB 연결 실패: " + e.getMessage() + "\n");
//        }
//    }
//}