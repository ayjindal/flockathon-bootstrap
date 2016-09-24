package co.flock.bootstrap;

import co.flock.bootstrap.database.*;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import static co.flock.bootstrap.database.Candidate.*;
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
            String scheduledTime = round.getString("scheduled_time");
            String collabLink = round.getString("collab_link");

            _dbManager.insertOrUpdateCandidate(new Candidate(email, name, creatorId, cvLink, role));
            _dbManager.insertOrUpdateRound(new Round(email, interviewerId, 1, collabLink, questionId, scheduledTime));

            return "";
        });

        get("/questions", (req, res) -> {

            String roleString = req.queryParams("role");
            ROLE role = null;
            if (roleString != null) {
                role = roleString.equalsIgnoreCase("platform") ? ROLE.PLATFORM : ROLE.APPLICATION;
            }

            String groupId = req.queryParams("groupId");

            _dbManager.getQuestions(role);

            return "";
        });

        get("/interviewer-view", (req, res) -> new ModelAndView(map, "interviewer-view.html"),
                new MustacheTemplateEngine());

        get("/new", (req, res) -> new ModelAndView(map, "candidate-new.html"),
                new MustacheTemplateEngine());
    }

    private static DbConfig getDbConfig()
    {
        ResourceBundle bundle = ResourceBundle.getBundle("config", Locale.getDefault());
        return new DbConfig(bundle.getString("db_host"),
                Integer.parseInt(bundle.getString("db_port")), bundle.getString("db_name"),
                bundle.getString("db_username"), bundle.getString("db_password"));
    }
}
