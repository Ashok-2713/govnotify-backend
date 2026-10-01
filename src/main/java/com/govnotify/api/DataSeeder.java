package com.govnotify.api;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final SourceRegistryRepository sourceRegistryRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(
            DepartmentRepository departmentRepository,
            SourceRegistryRepository sourceRegistryRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.departmentRepository = departmentRepository;
        this.sourceRegistryRepository = sourceRegistryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        fixLegacySchemaConstraints();
        seedDepartments();
        seedSources();
    }

    private void fixLegacySchemaConstraints() {
        // List of potential legacy columns from earlier prototypes to alter to NULL
        String[] legacyDeptCols = {"department_name", "department_code", "sector_id", "state_id", "source_url", "is_active", "last_scraped_at"};
        for (String col : legacyDeptCols) {
            try {
                jdbcTemplate.execute("ALTER TABLE departments MODIFY COLUMN " + col + " VARCHAR(255) NULL");
            } catch (Exception ignored) {}
        }

        String[] legacySourceCols = {"sector_id", "state_id", "department_name", "department_code", "is_active", "last_scraped_at"};
        for (String col : legacySourceCols) {
            try {
                jdbcTemplate.execute("ALTER TABLE source_registry MODIFY COLUMN " + col + " VARCHAR(255) NULL");
            } catch (Exception ignored) {}
        }
    }

    private void seedDepartments() {
        List<Department> initialDepts = List.of(
            new Department("TN_REV", "Revenue Department", "Tamil Nadu", "Land administration, revenue collection, disaster management, and VAO services.", "https://www.tn.gov.in/department/26"),
            new Department("TN_CTD", "Commercial Taxes Department", "Tamil Nadu", "GST administration, commercial tax assessment, and revenue enforcement.", "https://ctd.tn.gov.in"),
            new Department("TN_REG", "Registration Department", "Tamil Nadu", "Property registration, stamp duty management, and society registrations.", "https://tnreginet.gov.in"),
            new Department("TN_POL", "Police Department", "Tamil Nadu", "Law enforcement, public safety, traffic control, and uniformed services.", "https://tnpolice.gov.in"),
            new Department("TN_EDU", "School Education Department", "Tamil Nadu", "Primary, secondary education, teacher appointments, and curriculum oversight.", "https://tnschools.gov.in"),
            new Department("TN_RD", "Rural Development and Panchayat Raj Department", "Tamil Nadu", "Panchayat administration, rural infrastructure, and local body recruitment.", "https://tnrd.tn.gov.in"),
            new Department("TN_MAWS", "Municipal Administration and Water Supply Department", "Tamil Nadu", "Urban local bodies, municipal corporations, water supply, and civic engineering.", "https://tnmaws.tn.gov.in"),
            new Department("TN_HEALTH", "Health and Family Welfare Department", "Tamil Nadu", "Public healthcare, government medical colleges, nursing, and hospital administration.", "https://tnhealth.tn.gov.in"),
            new Department("TN_SW", "Social Welfare and Women Empowerment Department", "Tamil Nadu", "Child development, women empowerment welfare schemes, and social security.", "https://tn.gov.in/department/30"),
            new Department("TN_COP", "Co-operation, Food and Consumer Protection Department", "Tamil Nadu", "Cooperative societies, PDS distribution, consumer protection, and cooperative banks.", "https://tnpds.gov.in")
        );

        for (Department dept : initialDepts) {
            if (!departmentRepository.existsByCode(dept.getCode())) {
                departmentRepository.save(dept);
            }
        }
    }

    private void seedSources() {
        // First, delete all existing sources to avoid duplicates
        sourceRegistryRepository.deleteAll();
        
        Department policeDept = departmentRepository.findByCode("TN_POL").orElse(null);

        List<SourceRegistryEntity> initialSources = List.of(
            new SourceRegistryEntity("SRC_TNPSC", null, "Tamil Nadu", "Tamil Nadu Public Service Commission", "https://www.tnpsc.gov.in", "PLAYWRIGHT"),
            new SourceRegistryEntity("SRC_TNUSRB", policeDept, "Tamil Nadu", "Tamil Nadu Uniformed Services Recruitment Board", "https://www.tnusrb.tn.gov.in", "PLAYWRIGHT"),
            new SourceRegistryEntity("SRC_KPSC", null, "Karnataka", "Karnataka Public Service Commission", "https://kpsc.kar.nic.in", "PLAYWRIGHT"),
            new SourceRegistryEntity("SRC_KEA", null, "Karnataka", "Karnataka Examinations Authority", "https://cetonline.karnataka.gov.in", "PLAYWRIGHT"),
            new SourceRegistryEntity("SRC_KPSC_KER", null, "Kerala", "Kerala Public Service Commission", "https://www.keralapsc.gov.in", "PLAYWRIGHT"),
            new SourceRegistryEntity("SRC_APPSC", null, "Andhra Pradesh", "Andhra Pradesh Public Service Commission", "https://psc.ap.gov.in", "PLAYWRIGHT"),
            new SourceRegistryEntity("SRC_TSPSC", null, "Telangana", "Telangana Public Service Commission", "https://tspsc.gov.in", "PLAYWRIGHT"),
            new SourceRegistryEntity("SRC_PY_GOVT", null, "Puducherry", "Puducherry UT Government Recruitment", "https://recruitment.py.gov.in", "PLAYWRIGHT")
        );

        for (SourceRegistryEntity source : initialSources) {
            if (!sourceRegistryRepository.existsBySourceCode(source.getSourceCode())) {
                sourceRegistryRepository.save(source);
            }
        }
    }
}