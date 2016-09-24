package co.flock.bootstrap;

import co.flock.bootstrap.database.*;
import co.flock.bootstrap.database.Question.LEVEL;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.*;

import static co.flock.bootstrap.database.Candidate.ROLE;
import static spark.Spark.*;

public class Runner
{
    private static final Logger _logger = Logger.getLogger(Runner.class);
    private static DbManager _dbManager;

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
                _dbManager.insertOrUpdateUser(new User(userId, userToken));
                _logger.debug("User inserted : " + userId + "  " + userToken);
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
            String interviewerId = round.getString("interviewer_id");
            String questionId = round.getString("question_id");
            Long scheduledTime = round.getLong("scheduled_time");
            String collabLink = round.getString("collab_link");

            _dbManager.insertOrUpdateCandidate(new Candidate(email, name, creatorId, cvLink, role));
            _dbManager.insertOrUpdateRound(new Round(email, interviewerId, 1, collabLink, questionId, new Date(scheduledTime)));

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
                roundJsonObject.put("verdict", round.getVerdict());
                roundJsonObject.put("comments", round.getComments());
                roundJsonObject.put("collab_link", round.getCollabLink());
                roundJsonObject.put("rating", round.getRating());
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

        get("/interviewers", (req, res) -> {
            String groupId = req.queryParams("groupId");
            return "";
        });

        get("/interviewer-view", (req, res) -> new ModelAndView(map, "interviewer-view.html"),
                new MustacheTemplateEngine());

        get("/candidate-view", (req, res) -> new ModelAndView(map, "candidate-view.html"),
                new MustacheTemplateEngine());

        get("/new", (req, res) -> new ModelAndView(map, "candidate-new.html"),
                new MustacheTemplateEngine());
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
}
