package co.flock.bootstrap;

import co.flock.bootstrap.database.*;
import co.flock.bootstrap.database.Question.LEVEL;
import co.flock.bootstrap.messaging.MessagingService;
import co.flock.www.FlockApiClient;
import co.flock.www.model.PublicProfile;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateViewRoute;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static co.flock.bootstrap.database.Candidate.ROLE;
import static spark.Spark.*;

public class Runner
{
    private static final Logger _logger = Logger.getLogger(Runner.class);
    private static DbManager _dbManager;
    private static final ScheduledExecutorService _executorService = Executors.newScheduledThreadPool(1);
    private static MessagingService _messagingService = new MessagingService();

    public static void main(String[] args) throws Exception
    {
        _logger.debug("Starting..");
        _dbManager = new DbManager(getDbConfig());
        port(9000);
        staticFileLocation("/public");
        HashMap map = new HashMap();
        map.put("resourcePrefix", "");

        post("/", (req, res) -> {
            _logger.debug("Req received : " + req.body());
            JSONObject jsonObject = new JSONObject(req.body());
            String type = (String) jsonObject.get("name");
            _logger.debug("Got event: " + type);

            if ("app.install".equals(type)) {
                String userId = jsonObject.getString("userId");
                String userToken = jsonObject.getString("userToken");
                User user = new User(userId, userToken);
                _dbManager.insertOrUpdateUser(user);
                _logger.debug("User inserted : " + userId + "  " + userToken);

                _executorService.schedule((Runnable) () -> {
                    try {
                        FlockApiClient flockApiClient = new FlockApiClient(userToken);
                        co.flock.www.model.User userInfo = flockApiClient.getUserInfo();
                        user.setName(userInfo.getFirstName() + ' ' + userInfo.getLastName());
                        _dbManager.insertOrUpdateUser(user);

                    } catch (Exception e) {
                        _logger.debug("Failed : ", e);
                    }
                }, 2, TimeUnit.SECONDS);

            } else if ("app.uninstall".equalsIgnoreCase(type)) {
                String userId = jsonObject.getString("userId");
                _dbManager.deleteUser(new User(userId, ""));
                _logger.debug("User deleted : " + userId);
            }

            return "";
        });

        post("/create", (req, res) -> {

            _logger.debug("Received request with body: " + req.body());

            JSONObject jsonObject = new JSONObject(req.body());
            JSONObject candidate = jsonObject.getJSONObject("candidate");
            JSONObject round = jsonObject.getJSONObject("round");
            String name = candidate.getString("name");
            String email = candidate.getString("email");
            String cvLink = candidate.getString("cv_link");
            ROLE role = candidate.getString("role").equalsIgnoreCase("platform") ? ROLE.PLATFORM : ROLE.APPLICATION;
            String creatorId = candidate.getString("creator_id");
            String groupId = candidate.getString("group_id");
            String interviewerId = round.getString("interviewer_id");
            String questionId = round.getString("question_id");
            Long scheduledTime = round.getLong("scheduled_time");
            String collabLink = round.getString("collab_link");

            Candidate candidateObj = new Candidate(email, name, creatorId, cvLink, role, groupId);
            _dbManager.insertOrUpdateCandidate(candidateObj);
            Round roundObj = new Round(email, interviewerId, 1, collabLink, questionId, new Date(scheduledTime));
            _dbManager.insertOrUpdateRound(roundObj);
            User creator = _dbManager.getUserById(candidateObj.getCreatorId());
            User interviewer = _dbManager.getUserById(roundObj.getInterviewerID());
            _messagingService.sendCreationMessage(candidateObj, roundObj, creator, interviewer);
            return "";
        });

        post("/update", (req, res) -> {
            JSONObject jsonObject = new JSONObject(req.body());

            String email = jsonObject.getString("email");
            String interviewerId = jsonObject.getString("interviewer_id");
            String comments = jsonObject.getString("comments");
            String rating = jsonObject.getString("rating");
            String verdict = jsonObject.getString("verdict");
            Float ratingFloat = Float.parseFloat(rating);
            Round.VERDICT v = verdict.equalsIgnoreCase("pass") ? Round.VERDICT.PASS : Round.VERDICT.REJECT;
            _dbManager.updateRound(email, interviewerId, comments, ratingFloat, v);
            return "";
        });


        get("/questions", (req, res) -> {

            String roleString = req.queryParams("role");
            String sequenceNo = req.queryParams("sequence");

            ROLE role = null;
            if (roleString != null) {
                role = roleString.equalsIgnoreCase("platform") ? ROLE.PLATFORM : ROLE.APPLICATION;
            }

            String groupId = req.queryParams("groupId");

            List<Question> questionsList = _dbManager.getQuestions(role);
            if (sequenceNo != null && role != null) {
                questionsList = filterQuestionsBasedOnSequence(role, sequenceNo, questionsList);
            }

            JSONArray questions = new JSONArray();

            for (Question question : questionsList) {
                JSONObject ques = new JSONObject();
                ques.put("id", question.getId());
                ques.put("title", question.getTitle());
                ques.put("level", question.getLevel());
                ques.put("text", question.getText());
                questions.put(ques);
            }

            return questions.toString();
        });

        get("/history", (req, res) -> {

            String email = req.queryParams("email");
            Candidate candidate = _dbManager.getCandidateByEmail(email);
            List<Round> candidateRounds = _dbManager.getCandidateRounds(email);

            JSONObject jsonObject = new JSONObject();

            JSONObject candidateJsonObject = new JSONObject();
            if (candidate != null) {
                candidateJsonObject.put("name", candidate.getName());
                candidateJsonObject.put("email", candidate.getEmail());
                candidateJsonObject.put("cv_link", candidate.getCvLink());
                candidateJsonObject.put("role", candidate.getRole());
            }
            jsonObject.put("candidate", candidateJsonObject);

            JSONArray rounds = new JSONArray();

            for (Round round : candidateRounds) {
                JSONObject roundJsonObject = new JSONObject();
                roundJsonObject.put("interviewer_id", round.getInterviewerID());
                roundJsonObject.put("sequence", round.getSequence());
                roundJsonObject.put("verdict", round.getVerdict() != null ? round.getVerdict() : "");
                roundJsonObject.put("comments", round.getComments() != null ? round.getComments() : "");
                roundJsonObject.put("collab_link", round.getCollabLink());
                roundJsonObject.put("rating", round.getRating() != null ? round.getRating() : "");
                roundJsonObject.put("scheduled_time", round.getScheduledTime());
                Question question = _dbManager.getQuestionById(round.getQuestionID());
                JSONObject ques = new JSONObject();
                ques.put("id", question.getId());
                ques.put("title", question.getTitle());
                ques.put("level", question.getLevel());
                ques.put("text", question.getText());
                roundJsonObject.put("question", ques);

                rounds.put(roundJsonObject);
            }

            jsonObject.put("rounds", rounds);

            return jsonObject;
        });

        get("/launcher-button-view", (req, res) -> new ModelAndView(getLauncherButtonView(req.queryParams("flockEvent")),
                "launcher-view.mustache"), new MustacheTemplateEngine());

        get("/interviewers", (req, res) -> {
            String groupId = req.queryParams("groupId");
            String userId = req.queryParams("userId");
            User user = _dbManager.getUserById(userId);

            if (user != null) {
                FlockApiClient flockApiClient = new FlockApiClient(user.getToken());
                PublicProfile[] groupMembers = flockApiClient.getGroupMembers(groupId);

                JSONArray jsonArray = new JSONArray();
                for (PublicProfile publicProfile : groupMembers) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("userId", publicProfile.getId());
                    jsonObject.put("name", publicProfile.getFirstName() + ' ' + publicProfile.getLastName());
                    jsonArray.put(jsonObject);
                }

                return jsonArray;
            }

            return "User doesnt exist";
        });

        get("/interviewer-view", (request, response) -> {
            String email = request.queryParams("email");
            String preview = request.queryParams("flockWidgetType");
            if (preview.equalsIgnoreCase("inline")) {
                return new ModelAndView(getPreviewMap(email, request.queryParams("flockEvent")), "interviewer-preview.html");
            } else {
                return new ModelAndView(getMap(email), "interviewer-view.html");
            }
        }, new MustacheTemplateEngine());

        get("/candidate-view", (req, res) -> new ModelAndView(map, "candidate-view.html"),
                new MustacheTemplateEngine());

        get("/new", (req, res) -> new ModelAndView(map, "candidate-new.html"),
                new MustacheTemplateEngine());
    }

    private static Map<String, String> getPreviewMap(String email, String flockEvent) throws SQLException
    {
        JSONObject jsonObject = new JSONObject(flockEvent);
        String userId = jsonObject.getString("userId");
        Candidate candidate = _dbManager.getCandidateByEmail(email);
        List<Round> candidateRounds = _dbManager.getCandidateRounds(email, userId);

        if (candidateRounds.size() > 0) {

            Round round = candidateRounds.get(0);
            Question question = _dbManager.getQuestionById(round.getQuestionID());
            Map<String, String> map = new HashMap<>();
            map.put("candidateName", candidate.getName());
            map.put("cvLink", candidate.getCvLink());
            map.put("sequence", String.valueOf(round.getSequence()));
            map.put("questionTitle", question.getTitle());
            map.put("questionLevel", question.getLevel().name());
            map.put("collabLink", round.getCollabLink());
            SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, hh:mm aaa");
            map.put("time", df.format(round.getScheduledTime()));
            map.put("email", candidate.getEmail());
            return map;
        }

        return new HashMap<>();

    }

    private static Map<String, Object> getMap(String email) throws SQLException
    {
        Candidate candidate = _dbManager.getCandidateByEmail(email);
        List<Round> candidateRounds = _dbManager.getCandidateRounds(email);
        Map<String, Object> map = new HashMap<>();
        map.put("candidate", candidate);
        map.put("rounds", candidateRounds);

        return map;
    }

    private static List<Question> filterQuestionsBasedOnSequence(ROLE role, String sequenceNo, List<Question> questionsList)
    {
        int seqNo = Integer.parseInt(sequenceNo);
        LEVEL level;
        if (role == ROLE.PLATFORM) {
            level = (seqNo == 1) ? LEVEL.MEDIUM : LEVEL.HARD;
        } else {
            level = (seqNo == 1) ? LEVEL.EASY : LEVEL.MEDIUM;
        }

        List<Question> filteredQuestions = new ArrayList<>(questionsList.size());

        for (Question question : questionsList) {
            if (question.getLevel().equals(level)) {
                filteredQuestions.add(question);
            }
        }

        return filteredQuestions;
    }

    private static DbConfig getDbConfig()
    {
        ResourceBundle bundle = ResourceBundle.getBundle("config", Locale.getDefault());
        return new DbConfig(bundle.getString("db_host"),
                Integer.parseInt(bundle.getString("db_port")), bundle.getString("db_name"),
                bundle.getString("db_username"), bundle.getString("db_password"));
    }

    public static String getBaseUrl()
    {
        ResourceBundle bundle = ResourceBundle.getBundle("config", Locale.getDefault());
        return bundle.getString("base_url");
    }

    private static Map<String, List<Round>> getLauncherButtonView(String queryString) throws SQLException
    {
        JSONObject jsonObject = new JSONObject(queryString);
        String userId = jsonObject.getString("userId");
        List<Round> roundsTaken = _dbManager.getRoundsOwnedByUser(userId);
        Map<String, List<Round>> s = new HashMap<>();
        s.put("roundsTaken", roundsTaken);
        _logger.debug("roundsTaken:" + roundsTaken);
        return s;
    }
}
