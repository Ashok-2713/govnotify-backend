package com.govnotify.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai-assistant")
@CrossOrigin(origins = {"http://localhost:5173", "*"})
public class AIAssistantController {

    private final UserProfileRepository userProfileRepository;
    private final SavedJobRepository savedJobRepository;
    private final JobNotificationRepository jobNotificationRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ✅ WORKING GROQ MODEL
    private static final String GROQ_MODEL = "openai/gpt-oss-120b";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODELS_URL = "https://api.groq.com/openai/v1/models";

    @Value("${GROQ_CHATBOT_KEY:}")
    private String groqChatbotKey;

    public AIAssistantController(UserProfileRepository userProfileRepository,
                                 SavedJobRepository savedJobRepository,
                                 JobNotificationRepository jobNotificationRepository) {
        this.userProfileRepository = userProfileRepository;
        this.savedJobRepository = savedJobRepository;
        this.jobNotificationRepository = jobNotificationRepository;
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
    }

    private String resolveGroqKey() {
        if (groqChatbotKey != null && !groqChatbotKey.isBlank()) {
            return groqChatbotKey.trim();
        }
        String envKey = System.getenv("GROQ_CHATBOT_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        return null;
    }

    // ============ DEBUG ENDPOINT ============
    @GetMapping("/debug")
    public Map<String, String> debug() {
        Map<String, String> r = new HashMap<>();
        String key = resolveGroqKey();
        if (key == null || key.isBlank()) {
            r.put("key_status", "MISSING");
        } else {
            r.put("key_status", "LOADED");
            r.put("key_length", String.valueOf(key.length()));
            r.put("key_prefix", key.substring(0, Math.min(10, key.length())) + "...");
        }
        r.put("model", GROQ_MODEL);
        return r;
    }

    // ============ LIST AVAILABLE GROQ MODELS ============
    @GetMapping("/models")
    public ResponseEntity<?> listModels() {
        String apiKey = resolveGroqKey();
        if (apiKey == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "GROQ_CHATBOT_KEY is not loaded"));
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    GROQ_MODELS_URL, HttpMethod.GET, entity, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "error", e.getClass().getSimpleName(),
                    "message", e.getMessage()
            ));
        }
    }

    // ============ TEST GROQ CONNECTION ============
    @GetMapping("/test-groq")
    public Map<String, String> testGroq() {
        Map<String, String> r = new HashMap<>();

        try {
            java.net.InetAddress.getByName("api.groq.com");
            r.put("dns", "OK");
        } catch (Exception dnsEx) {
            r.put("dns", "FAILED: " + dnsEx.getMessage());
            return r;
        }

        String apiKey = resolveGroqKey();
        if (apiKey == null) {
            r.put("status", "NO_KEY");
            return r;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", GROQ_MODEL);
            body.put("max_tokens", 50);
            body.put("messages", List.of(
                    Map.of("role", "user", "content", "Reply with exactly: HELLO")
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_URL, entity, String.class);

            r.put("status", "SUCCESS");
            r.put("http_code", String.valueOf(response.getStatusCode().value()));
            r.put("model_used", GROQ_MODEL);
            r.put("raw_response", response.getBody() != null && response.getBody().length() > 300
                    ? response.getBody().substring(0, 300) + "..."
                    : response.getBody());

        } catch (Exception e) {
            r.put("status", "ERROR");
            r.put("model_used", GROQ_MODEL);
            r.put("error_type", e.getClass().getSimpleName());
            r.put("error_message", e.getMessage());
            if (e.getCause() != null) {
                r.put("cause", e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
        }
        return r;
    }

    // ============ STATS ENDPOINT ============
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<JobNotification> allJobs = jobNotificationRepository.findAll();
        List<JobNotification> activeJobs = allJobs.stream()
            .filter(j -> !"CLOSED".equalsIgnoreCase(j.getStatus()) 
                      && !"ARCHIVED".equalsIgnoreCase(j.getStatus()))
            .collect(Collectors.toList());

        Map<String, Object> r = new HashMap<>();
        r.put("total", activeJobs.size());
        r.put("activeTotal", activeJobs.size());
        r.put("allTotal", allJobs.size());
        r.put("closedTotal", allJobs.stream().filter(j -> "CLOSED".equalsIgnoreCase(j.getStatus())).count());
        r.put("archivedTotal", allJobs.stream().filter(j -> "ARCHIVED".equalsIgnoreCase(j.getStatus())).count());
        r.put("karnataka", countByState(activeJobs, "karnataka"));
        r.put("tamilNadu", countByState(activeJobs, "tamil nadu"));
        r.put("kerala", countByState(activeJobs, "kerala"));
        r.put("andhraPradesh", countByState(activeJobs, "andhra"));
        r.put("telangana", countByState(activeJobs, "telangana"));
        r.put("central", countCentral(activeJobs));
        return r;
    }

    // Helper: Robust state counting
    private long countByState(List<JobNotification> jobs, String stateKeyword) {
        return jobs.stream()
                .filter(j -> j.getState() != null)
                .filter(j -> j.getState().toLowerCase().contains(stateKeyword))
                .count();
    }

    // Helper: Robust central counting
    private long countCentral(List<JobNotification> jobs) {
        return jobs.stream()
                .filter(j -> j.getState() != null)
                .filter(j -> {
                    String s = j.getState().toLowerCase();
                    return s.contains("all india") || s.contains("central") || s.contains("all-india") || s.contains("allindia");
                })
                .count();
    }

    // Stop words to exclude when isolating job titles or keywords from conversational queries
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "how", "to", "apply", "what", "is", "the", "salary", "in", "of", "and", "or",
            "documents", "required", "document", "eligibility", "eligible", "for", "details",
            "tell", "me", "about", "can", "i", "job", "jobs", "notification", "notifications",
            "where", "when", "last", "date", "dates", "exam", "pattern", "syllabus", "benefits",
            "benefit", "link", "website", "online", "application", "fee", "fees", "criteria",
            "information", "post", "posts", "please", "give", "show", "get", "need", "know",
            "want", "any", "are", "there", "do", "you", "have", "with", "from", "on", "at",
            "by", "this", "that", "these", "those", "hi", "hello", "hey", "good", "morning",
            "afternoon", "evening", "thanks", "thank"
    ));

    private String cleanText(String s) {
        if (s == null) return "";
        return s.replaceAll("[^a-zA-Z0-9\\s]", " ").toLowerCase();
    }

    private String nvl(String s) {
        return (s != null && !s.isBlank()) ? s.trim() : "NOT SPECIFIED IN RECORD";
    }

    /**
     * Context-Aware Job Retrieval:
     * Identifies jobs matching the user's current query or recent conversational turns.
     */
    private List<JobNotification> findMatchingJobs(String userMessage, List<Map<String, String>> history, List<JobNotification> allJobs) {
        if (allJobs == null || allJobs.isEmpty() || userMessage == null) {
            return Collections.emptyList();
        }

        // 1. Extract query tokens from current message
        String cleanedMsg = cleanText(userMessage);
        List<String> queryTokens = Arrays.stream(cleanedMsg.split("\\s+"))
                .filter(t -> t.length() >= 3 && !STOP_WORDS.contains(t))
                .collect(Collectors.toList());

        // 2. Multi-turn Follow-up Handling:
        // If current query has few or no specific job tokens (e.g. "What is the salary?", "How to apply?", "Eligibility?"),
        // inspect recent conversation history to identify the referenced job.
        if (queryTokens.isEmpty() && history != null && !history.isEmpty()) {
            for (int i = history.size() - 1; i >= 0; i--) {
                Map<String, String> prevTurn = history.get(i);
                if (prevTurn != null) {
                    String content = prevTurn.get("content");
                    if (content != null && !content.isBlank()) {
                        String cleanedPrev = cleanText(content);
                        List<String> prevTokens = Arrays.stream(cleanedPrev.split("\\s+"))
                                .filter(t -> t.length() >= 3 && !STOP_WORDS.contains(t))
                                .collect(Collectors.toList());
                        if (!prevTokens.isEmpty()) {
                            queryTokens.addAll(prevTokens);
                            break;
                        }
                    }
                }
            }
        }

        if (queryTokens.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Relevance Scoring across all jobs
        Map<JobNotification, Integer> scoredJobs = new HashMap<>();

        for (JobNotification job : allJobs) {
            int score = 0;
            String title = (job.getTitle() != null) ? job.getTitle().toLowerCase() : "";
            String org = (job.getOrganization() != null) ? job.getOrganization().toLowerCase() : "";
            String exam = (job.getExamName() != null) ? job.getExamName().toLowerCase() : "";
            String post = (job.getPostTitle() != null) ? job.getPostTitle().toLowerCase() : "";
            String sector = (job.getSector() != null) ? job.getSector().toLowerCase() : "";
            String deptCode = (job.getDepartmentCode() != null) ? job.getDepartmentCode().toLowerCase() : "";
            String refId = (job.getNotificationReferenceId() != null) ? job.getNotificationReferenceId().toLowerCase() : "";

            // Direct title match
            if (!title.isEmpty() && (cleanedMsg.contains(title) || (title.length() > 5 && cleanedMsg.contains(title.substring(0, Math.min(25, title.length())))))) {
                score += 50;
            }

            int matchedTokensCount = 0;
            for (String token : queryTokens) {
                boolean matched = false;
                if (!title.isEmpty() && title.contains(token)) {
                    score += 15;
                    matched = true;
                }
                if (!org.isEmpty() && org.contains(token)) {
                    score += 10;
                    matched = true;
                }
                if (!exam.isEmpty() && exam.contains(token)) {
                    score += 12;
                    matched = true;
                }
                if (!post.isEmpty() && post.contains(token)) {
                    score += 12;
                    matched = true;
                }
                if (!sector.isEmpty() && sector.contains(token)) {
                    score += 8;
                    matched = true;
                }
                if (!deptCode.isEmpty() && deptCode.contains(token)) {
                    score += 15;
                    matched = true;
                }
                if (!refId.isEmpty() && refId.contains(token)) {
                    score += 25;
                    matched = true;
                }
                if (matched) {
                    matchedTokensCount++;
                }
            }

            if (matchedTokensCount > 1) {
                score += (matchedTokensCount * 5);
            }

            if (score >= 10) {
                scoredJobs.put(job, score);
            }
        }

        // ✅ FIXED: Null-safe mapping to avoid "Null type safety" warning
        return scoredJobs.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(entry -> entry != null ? entry.getKey() : null)   // ← Fix applied
                .filter(Objects::nonNull)                              // ← Safety net
                .collect(Collectors.toList());
    }

    // ============ MAIN CHAT ENDPOINT ============
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> request) {
        String userEmail = request != null && request.get("userEmail") != null ? String.valueOf(request.get("userEmail")) : null;
        String userMessage = request != null && request.get("message") != null ? String.valueOf(request.get("message")) : null;

        @SuppressWarnings("unchecked")
        List<Map<String, String>> clientHistory = (request != null && request.get("history") instanceof List<?>)
                ? (List<Map<String, String>>) request.get("history")
                : Collections.emptyList();

        if (userMessage == null || userMessage.trim().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("reply", "Please ask a question about jobs, eligibility, or deadlines."));
        }

        // 1. Fetch user profile
        String qualification = "Not specified";
        String age = "Not specified";
        String state = "Not specified";
        String category = "Not specified";

        if (userEmail != null && !userEmail.isBlank()) {
            Optional<UserProfile> profileOpt = userProfileRepository.findByEmail(userEmail.trim());
            if (profileOpt.isPresent()) {
                UserProfile p = profileOpt.get();
                if (p.getQualification() != null && !p.getQualification().isBlank()) qualification = p.getQualification();
                if (p.getAge() != null) age = String.valueOf(p.getAge());
                if (p.getState() != null && !p.getState().isBlank()) state = p.getState();
                if (p.getCategory() != null && !p.getCategory().isBlank()) category = p.getCategory();
            }
        }

        // 2. Fetch saved jobs
        List<String> savedTitles = new ArrayList<>();
        if (userEmail != null && !userEmail.isBlank()) {
            List<SavedJob> savedList = savedJobRepository.findByUserEmail(userEmail.trim());
            if (savedList != null) {
                for (SavedJob sj : savedList) {
                    if (sj.getJobTitle() != null && !sj.getJobTitle().isBlank()) {
                        savedTitles.add(sj.getJobTitle());
                    }
                }
            }
        }
        String savedJobsStr = savedTitles.isEmpty() ? "None" : String.join(", ", savedTitles);

        // 3. Fetch ALL jobs and compute real counts (active vs closed)
        List<JobNotification> allJobs = jobNotificationRepository.findAll();
        List<JobNotification> activeJobs = allJobs.stream()
            .filter(j -> !"CLOSED".equalsIgnoreCase(j.getStatus()) 
                      && !"ARCHIVED".equalsIgnoreCase(j.getStatus()))
            .collect(Collectors.toList());

        long totalJobs = activeJobs.size();
        long totalAllJobs = allJobs.size();
        long closedJobs = allJobs.stream().filter(j -> "CLOSED".equalsIgnoreCase(j.getStatus())).count();
        long archivedJobs = allJobs.stream().filter(j -> "ARCHIVED".equalsIgnoreCase(j.getStatus())).count();

        long karnataka = countByState(activeJobs, "karnataka");
        long tamilNadu = countByState(activeJobs, "tamil nadu");
        long kerala = countByState(activeJobs, "kerala");
        long andhra = countByState(activeJobs, "andhra");
        long telangana = countByState(activeJobs, "telangana");
        long central = countCentral(activeJobs);

        long openJobs = activeJobs.stream().filter(j -> "OPEN".equalsIgnoreCase(j.getStatus())).count();
        long upcomingJobs = activeJobs.stream().filter(j -> "UPCOMING".equalsIgnoreCase(j.getStatus())).count();
        long closingSoonJobs = activeJobs.stream().filter(j -> "CLOSING SOON".equalsIgnoreCase(j.getStatus())).count();

        // 4. Job Context Retrieval: Match specific jobs from database
        List<JobNotification> matchedJobs = findMatchingJobs(userMessage, clientHistory, allJobs);
        StringBuilder jobContextBuilder = new StringBuilder();

        if (!matchedJobs.isEmpty()) {
            jobContextBuilder.append("=== SPECIFIC JOB(S) FOUND IN DATABASE FOR THIS QUERY ===\n");
            for (int i = 0; i < matchedJobs.size(); i++) {
                JobNotification j = matchedJobs.get(i);
                jobContextBuilder.append("JOB RECORD #").append(i + 1).append(":\n");
                jobContextBuilder.append("- Title: ").append(nvl(j.getTitle())).append("\n");
                jobContextBuilder.append("- Organization: ").append(nvl(j.getOrganization())).append("\n");
                if (j.getPostTitle() != null && !j.getPostTitle().isBlank()) {
                    jobContextBuilder.append("- Post Title: ").append(j.getPostTitle()).append("\n");
                }
                if (j.getExamName() != null && !j.getExamName().isBlank()) {
                    jobContextBuilder.append("- Exam Name: ").append(j.getExamName()).append("\n");
                }
                if (j.getSector() != null && !j.getSector().isBlank()) {
                    jobContextBuilder.append("- Sector: ").append(j.getSector()).append("\n");
                }
                jobContextBuilder.append("- State: ").append(nvl(j.getState())).append("\n");
                jobContextBuilder.append("- Status: ").append(nvl(j.getStatus())).append("\n");
                jobContextBuilder.append("- Category: ").append(nvl(j.getCategory())).append("\n");
                jobContextBuilder.append("- Qualification Required: ").append(nvl(j.getQualification())).append("\n");
                jobContextBuilder.append("- Age Limit: ").append(nvl(j.getAgeLimit())).append("\n");
                jobContextBuilder.append("- Vacancies: ").append(j.getVacancies() != null ? String.valueOf(j.getVacancies()) : "NOT SPECIFIED IN RECORD").append("\n");
                jobContextBuilder.append("- Salary / Pay Scale: ").append(nvl(j.getSalary())).append("\n");
                jobContextBuilder.append("- Registration Start Date: ").append(j.getRegistrationStartDate() != null ? j.getRegistrationStartDate().toString() : "NOT SPECIFIED IN RECORD").append("\n");
                jobContextBuilder.append("- Last Date to Apply: ").append(j.getLastDate() != null ? j.getLastDate().toString() : "NOT SPECIFIED IN RECORD").append("\n");
                jobContextBuilder.append("- Exam Date: ").append(j.getExamDate() != null ? j.getExamDate().toString() : "NOT SPECIFIED IN RECORD").append("\n");
                jobContextBuilder.append("- Official Apply URL: ").append(nvl(j.getOfficialApplyUrl())).append("\n");
                jobContextBuilder.append("- Official Notification PDF URL: ").append(nvl(j.getOfficialPdfUrl())).append("\n");
                jobContextBuilder.append("- Source Website: ").append(j.getSourceWebsiteName() != null && !j.getSourceWebsiteName().isBlank() ? j.getSourceWebsiteName() : nvl(j.getSourceUrl())).append("\n");
                if (j.getDescription() != null && !j.getDescription().isBlank()) {
                    jobContextBuilder.append("- Description/Details: ").append(j.getDescription()).append("\n");
                }
                jobContextBuilder.append("\n");
            }
            jobContextBuilder.append("=== END SPECIFIC JOB(S) ===\n");
        } else {
            jobContextBuilder.append("NO SPECIFIC JOB FOUND IN DATABASE FOR THIS QUERY.\n");
        }

        // 5. Build general job sample (up to 40 active jobs for general queries)
        int sampleSize = Math.min(40, (int) totalJobs);
        List<JobNotification> topJobs = activeJobs.stream().limit(sampleSize).collect(Collectors.toList());

        StringBuilder availableJobsBuilder = new StringBuilder();
        for (JobNotification j : topJobs) {
            String title = (j.getTitle() != null) ? j.getTitle() : "Government Job";
            String jState = (j.getState() != null) ? j.getState() : "All-India";
            String deadline = (j.getLastDate() != null) ? j.getLastDate().toString() : "Not specified";
            String link = (j.getOfficialApplyUrl() != null && !j.getOfficialApplyUrl().isBlank())
                    ? j.getOfficialApplyUrl()
                    : ((j.getOfficialPdfUrl() != null) ? j.getOfficialPdfUrl() : "");
            availableJobsBuilder.append("- ").append(title)
                    .append(" | ").append(jState)
                    .append(" | Status: ").append(nvl(j.getStatus()))
                    .append(" | Last Date: ").append(deadline);
            if (!link.isBlank()) availableJobsBuilder.append(" | ").append(link);
            availableJobsBuilder.append("\n");
        }

        String availableJobsList = availableJobsBuilder.toString().trim();
        if (availableJobsList.isEmpty()) availableJobsList = "No active jobs listed currently.";

        // 6. Build system prompt with STRICT fallback & anti-hallucination rules
        String effectiveEmail = (userEmail != null && !userEmail.isBlank()) ? userEmail.trim() : "Guest";
        String systemPrompt =
            "You are GovNotify AI Assistant, an authoritative and helpful assistant for Indian government job seekers.\n\n" +
            "=== CRITICAL JOB COUNTS (MEMORIZE THESE) ===\n" +
            "ACTIVE JOBS (Default Count) = " + totalJobs + "\n" +
            "CLOSED JOBS = " + closedJobs + "\n" +
            "ARCHIVED JOBS = " + archivedJobs + "\n" +
            "TOTAL JOBS IN DATABASE = " + totalAllJobs + "\n" +
            "KARNATAKA ACTIVE = " + karnataka + "\n" +
            "TAMIL NADU ACTIVE = " + tamilNadu + "\n" +
            "KERALA ACTIVE = " + kerala + "\n" +
            "ANDHRA PRADESH ACTIVE = " + andhra + "\n" +
            "TELANGANA ACTIVE = " + telangana + "\n" +
            "CENTRAL / ALL INDIA ACTIVE = " + central + "\n" +
            "OPEN STATUS = " + openJobs + "\n" +
            "UPCOMING STATUS = " + upcomingJobs + "\n" +
            "CLOSING SOON STATUS = " + closingSoonJobs + "\n" +
            "=== END COUNTS ===\n\n" +
            "USER PROFILE:\n" +
            "- Email: " + effectiveEmail + "\n" +
            "- Qualification: " + qualification + "\n" +
            "- Age: " + age + "\n" +
            "- State: " + state + "\n" +
            "- Category: " + category + "\n" +
            "- Saved jobs: " + savedJobsStr + "\n\n" +
            "SPECIFIC JOB CONTEXT FOR CURRENT QUERY:\n" +
            jobContextBuilder.toString() + "\n\n" +
            "JOB DATABASE SAMPLE (showing " + sampleSize + " of " + totalJobs + " active jobs — DO NOT use this sample number for counts):\n" +
            availableJobsList + "\n\n" +
            "MANDATORY OPERATIONAL & ANTI-HALLUCINATION RULES:\n" +
            "1. SPECIFIC JOB QUERIES: If the user asks about a specific job and details are found in 'SPECIFIC JOB CONTEXT', answer accurately using ONLY those details.\n" +
            "   - 'How to apply?': Clearly explain using the Official Apply URL / Portal link from the record.\n" +
            "   - 'What documents?': List standard government job documents (Identity Proof like Aadhaar/PAN, Educational Certificates/Degree/10th marksheet, Passport-size Photo, Scanned Signature, Category/Caste Certificate if applicable) and advise checking the official notification PDF for post-specific document criteria.\n" +
            "   - 'Where to apply?': Provide the Official Apply URL or Source Website from the record.\n" +
            "   - 'What salary?': If Salary is specified in the record, state it clearly. If it is 'NOT SPECIFIED IN RECORD', follow Rule 3.\n" +
            "   - 'What benefits?': If benefits are not listed in the record, follow Rule 3.\n" +
            "   - 'Eligibility': Evaluate against the user's qualification, age, and state compared to the job record.\n" +
            "   - 'Job Status': If a job has status 'CLOSED', inform the user politely that the deadline has passed and applications are closed.\n" +
            "2. JOB NOT IN DATABASE (FALLBACK): If the user asks about a specific job, department, company, or exam that is NOT found in the database (or if SPECIFIC JOB CONTEXT says 'NO SPECIFIC JOB FOUND IN DATABASE FOR THIS QUERY'):\n" +
            "   You MUST respond politely with:\n" +
            "   \"I couldn't find this job in our database. Please check the official government portal or try searching with a different keyword.\"\n" +
            "   NEVER invent or hallucinate information about non-existent jobs.\n" +
            "3. INFORMATION MISSING FROM CONTEXT (FALLBACK): If a job exists in the database, but the user asks for a specific detail (e.g. syllabus, exam pattern, specific fee, salary, benefits, or posting location) that is marked as 'NOT SPECIFIED IN RECORD' or is absent from the record:\n" +
            "   You MUST respond politely with:\n" +
            "   \"I don't have that specific detail in my records. Please refer to the official notification PDF for complete information.\"\n" +
            "   (Include the Official Notification PDF URL or Official Apply URL from the record if available).\n" +
            "   NEVER fabricate or guess missing details.\n" +
            "4. UNRELATED QUESTIONS (FALLBACK): If the user asks about general topics unrelated to government jobs, exams, or recruitment (such as sports, weather, movies, cooking, coding, math, politics):\n" +
            "   You MUST respond with:\n" +
            "   \"I am specialized in government job notifications. I can't answer general questions. Is there a specific job you'd like to know about?\"\n" +
            "5. NEVER HALLUCINATE: Never make up dates, vacancies, salaries, links, or criteria.\n" +
            "6. TOTAL COUNTS: If the user asks 'how many jobs' or 'total jobs', reply with the ACTIVE job count (" + totalJobs + "). If the user specifically asks about 'closed jobs', 'expired jobs', or 'how many closed jobs', answer that there are " + closedJobs + " closed jobs (out of " + totalAllJobs + " total notifications in the system).\n" +
            "7. LANGUAGE: Detect user language and reply in the SAME language (English, Hindi, Kannada, Tamil, Telugu, Malayalam).\n" +
            "8. FORMAT: Keep responses under 200 words. Use bullet points and clickable markdown links where applicable: [Link Text](url).\n";

        // 7. Call Groq
        String apiKey = resolveGroqKey();
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("❌ GROQ KEY IS MISSING");
            return ResponseEntity.ok(Map.of("reply",
                    "I'm offline right now. Try: 'Show Kerala jobs' or 'Closing this week'."));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", GROQ_MODEL);
            body.put("temperature", 0.2);
            body.put("max_tokens", 600);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            // Include multi-turn conversation history (last 6 turns)
            if (clientHistory != null && !clientHistory.isEmpty()) {
                int startIdx = Math.max(0, clientHistory.size() - 6);
                for (int i = startIdx; i < clientHistory.size(); i++) {
                    Map<String, String> h = clientHistory.get(i);
                    if (h != null) {
                        String role = h.get("role");
                        String content = h.get("content");
                        if (role != null && content != null && !content.isBlank()) {
                            String validRole = "user".equalsIgnoreCase(role) ? "user" : "assistant";
                            messages.add(Map.of("role", validRole, "content", content));
                        }
                    }
                }
            }

            messages.add(Map.of("role", "user", "content", userMessage.trim()));
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String reply = choices.get(0).path("message").path("content").asText();
                    return ResponseEntity.ok(Map.of("reply", reply));
                }
            }

            return ResponseEntity.ok(Map.of("reply",
                    "I couldn't retrieve a response. Please ask again about jobs, eligibility, or deadlines."));

        } catch (Exception e) {
            System.err.println("❌ GROQ ERROR: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("reply",
                    "I'm offline right now. Try: 'Show Kerala jobs' or 'Closing this week'."));
        }
    }
}