package co.flock.bootstrap;

import co.flock.bootstrap.database.*;
import co.flock.bootstrap.database.Question.LEVEL;
import co.flock.bootstrap.mail.MailServer;
import co.flock.bootstrap.messaging.MessagingService;
import co.flock.www.FlockApiClient;
import co.flock.www.model.PublicProfile;
import co.flock.www.model.messages.Message;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.SQLException;
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
    private static final int MILLIS_IN_FIFTEEN_MINS = 15 * 60 * 1000;
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
            User user = _dbManager.getUserById(creatorId);
            String creatorName = user.getName();
            _logger.debug(creatorName);
            Candidate candidateObj = new Candidate(email, name, creatorId, creatorName, cvLink, role, groupId);
            _dbManager.insertOrUpdateCandidate(candidateObj);
            Round roundObj = new Round(email, interviewerId, 1, collabLink, questionId, new Date(scheduledTime));
            _dbManager.insertOrUpdateRound(roundObj);
            User creator = _dbManager.getUserById(candidateObj.getCreatorId());
            User interviewer = _dbManager.getUserById(roundObj.getInterviewerID());
            _messagingService.sendCreationMessage(candidateObj, roundObj, creator, interviewer);
            MailServer.sendEmail(candidateObj.getEmail(), roundObj.getScheduledTime(),
                    roundObj.getCollabLink().replace("interviewer-view", "candidate-view"));
            scheduleReminderIfNeeded(scheduledTime, creator, interviewer);
            return "";
        });

        post("/edit", (req, res) -> {
            _logger.debug("Received request with body: " + req.body());
            JSONObject jsonObject = new JSONObject(req.body());
            JSONObject round = jsonObject.getJSONObject("round");
            String email = jsonObject.getString("email");
            Candidate candidate = _dbManager.getCandidateByEmail(email);
            _logger.debug("candidate: " + candidate);
            String interviewerId = round.getString("interviewer_id");
            String questionId = round.getString("question_id");
            Long scheduledTime = round.getLong("scheduled_time");
            String collabLink = round.getString("collab_link");
            User creator = _dbManager.getUserById(candidate.getCreatorId());
            Round roundObj = new Round(email, interviewerId, 2, collabLink, questionId, new Date(scheduledTime));
            Round firstRound = _dbManager.getCandidateFirstRound(email);
            User interviewer = _dbManager.getUserById(roundObj.getInterviewerID());
            _dbManager.insertOrUpdateRound(roundObj);
            _messagingService.sendUpdationMessage(candidate, firstRound, roundObj, creator, interviewer);
            return "";
        });

        post("/update", (req, res) -> {
            _logger.debug("Got request with body: " + req.body());
            JSONObject jsonObject = new JSONObject(req.body());

            String email = jsonObject.getString("email");
            String interviewerId = jsonObject.getString("interviewer_id");
            String comments = jsonObject.getString("comments");
            String rating = jsonObject.getString("rating");
            String verdict = jsonObject.getString("verdict");
            Float ratingFloat = Float.parseFloat(rating);
            Round.VERDICT v = verdict.equalsIgnoreCase("pass") ? Round.VERDICT.PASS : Round.VERDICT.REJECT;
            Round round = _dbManager.getRound(email, interviewerId);
            _dbManager.updateRound(email, interviewerId, comments, ratingFloat, v);
            Candidate candidate = _dbManager.getCandidateByEmail(email);
            User user = _dbManager.getUserById(interviewerId);
            User nextInterviewer = getNextInterviewer(candidate, user, round);
            _messagingService.sendRoundEndedMessage(candidate, user, nextInterviewer, round);
            _logger.debug("Done updating the round");
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
                "launcher-view.html"), new MustacheTemplateEngine());

        get("/chat-tab-button-view", (req, res) -> new ModelAndView(getChatTabButtonView(req.queryParams("flockEvent")),
                "chat-tab-view.html"), new MustacheTemplateEngine());

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
                return new ModelAndView(getPreviewMap(email), "interviewer-preview.html");
            } else {
                return new ModelAndView(getMap(email), "interviewer-view.html");
            }
        }, new MustacheTemplateEngine());

        get("/candidate-view", (req, res) -> new ModelAndView(map, "candidate-view.html"),
                new MustacheTemplateEngine());

        get("/stats", (req, res) -> new ModelAndView(getStatsMap(req.queryParams("flockEvent")), "template_stats.mustache"),
                new MustacheTemplateEngine());

        get("/new", (req, res) -> new ModelAndView(map, "candidate-new.html"),
                new MustacheTemplateEngine());

        get("/edit", (req, res) -> {
            String email = req.queryParams("email");
            return new ModelAndView(getEditMap(email), "candidate-edit.html");
        }, new MustacheTemplateEngine());
    }

    private static User getNextInterviewer(Candidate candidate, User user, Round round)
    {
        _logger.debug("getNextInterviewer candidate: " + candidate + " round: " + round);
        FlockApiClient flockApiClient = new FlockApiClient(user.getToken());
        PublicProfile[] groupMembers = new PublicProfile[0];
        try {
            groupMembers = flockApiClient.getGroupMembers(candidate.getGroupId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (PublicProfile publicProfile : groupMembers) {
            if (!publicProfile.getId().equalsIgnoreCase(round.getInterviewerID())) {
                _logger.debug("Next interviewer: " + publicProfile.getId());
                try {
                    return _dbManager.getUserById(publicProfile.getId());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static Map<String, Object> getStatsMap(String flockEvent) throws SQLException
    {
        JSONObject jsonObject = new JSONObject(flockEvent);
        String groupId = jsonObject.getString("chat");
        String userId = jsonObject.getString("userId");
        Map<String, Object> map = new HashMap<>();

        User user = _dbManager.getUserById(userId);
        FlockApiClient flockApiClient = new FlockApiClient(user.getToken());
        List<Candidate> candidateList = _dbManager.getCandidatesByGroupId(groupId);
        Integer completed = 0;
        Integer pending = 0;
        Map<String, Integer> userIdToScoreMap = new HashMap<>();

        if (candidateList.size() > 0) {
            for (Candidate candidate : candidateList) {
                List<Round> candidateRounds = _dbManager.getCandidateRounds(candidate.getEmail());
                int passVerdict = 0;
                for (Round round : candidateRounds) {

                    if (round.getVerdict() != null) {

                        if (!userIdToScoreMap.containsKey(round.getInterviewerID())) {
                            userIdToScoreMap.put(round.getInterviewerID(), 0);
                        }

                        userIdToScoreMap.put(round.getInterviewerID(), userIdToScoreMap.get(round.getInterviewerID()) + 1);

                        if (round.getVerdict().equals(Round.VERDICT.PASS)) {
                            passVerdict++;
                        }
                    }
                }

                if (passVerdict >= 2) {
                    completed++;
                } else {
                    pending++;
                }
            }
        }

        List<Score> scoreList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : userIdToScoreMap.entrySet()) {
            scoreList.add(new Score(_dbManager.getUserById(entry.getKey()).getName(), entry.getValue()));
        }


        map.put("total", candidateList.size());
        map.put("completed", completed);
        map.put("pending", pending);
        map.put("scores", scoreList);
        return map;
    }


    private static void scheduleReminderIfNeeded(Long scheduledTime, User creator, User interviewer)
    {
        long current = System.currentTimeMillis();
        long diff = scheduledTime - current;
        if (diff > MILLIS_IN_FIFTEEN_MINS) {
            long scheduleAfter = scheduledTime - MILLIS_IN_FIFTEEN_MINS;
            _executorService.schedule((Runnable) () -> {
                SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, hh:mm aaa");
                Message message = new Message(interviewer.getId(), "Reminder : You have an interview scheduled at " + df.format(scheduledTime));
                MessagingService.sendMessage(creator.getToken(), message);
            }, scheduleAfter, TimeUnit.MILLISECONDS);
        }
    }

    private static Map<String, String> getPreviewMap(String email) throws SQLException
    {
        Candidate candidate = _dbManager.getCandidateByEmail(email);
        List<Round> candidateRounds = _dbManager.getCandidateRounds(email);
        candidateRounds.sort((o1, o2) -> Integer.compare(o1.getSequence(), o2.getSequence()));

        if (candidateRounds.size() > 0) {

            Round round = candidateRounds.get(candidateRounds.size() - 1);

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

    private static Map<String, List<Candidate>> getChatTabButtonView(String queryString) throws SQLException
    {
        JSONObject jsonObject = new JSONObject(queryString);
        String groupId = jsonObject.getString("chat");
        List<Candidate> candidates = _dbManager.getCandidatesForGroup(groupId);
        Map<String, List<Candidate>> s = new HashMap<>();
        s.put("candidates", candidates);
        return s;
    }

    private static Map<String, Object> getEditMap(String email) throws SQLException {
        Candidate candidate = _dbManager.getCandidateByEmail(email);
        List<Round> candidateRounds = _dbManager.getCandidateRounds(email);
        Map<String, Object> map = new HashMap<>();
        map.put("candidate", candidate);
        map.put("rounds", candidateRounds);
        return map;
    }
}
