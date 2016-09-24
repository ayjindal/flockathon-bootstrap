package co.flock.bootstrap;

import co.flock.bootstrap.database.DbConfig;
import co.flock.bootstrap.database.DbManager;
import co.flock.bootstrap.database.User;
import org.apache.log4j.Logger;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

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

        get("/interviewer-view", (req, res) -> new ModelAndView(map, "index.html"),
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
