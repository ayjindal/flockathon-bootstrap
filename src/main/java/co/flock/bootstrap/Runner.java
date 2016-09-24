package co.flock.bootstrap;

import co.flock.bootstrap.database.DbConfig;
import co.flock.bootstrap.database.DbManager;
import org.apache.log4j.Logger;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import static spark.Spark.*;

public class Runner {
    private static final Logger _logger = Logger.getLogger(Runner.class);
    private static DbManager _dbManager;

    public static void main(String[] args) throws Exception {
        _logger.debug("Starting..");
        _dbManager = new DbManager(getDbConfig());
        port(9000);
        staticFileLocation("/public");
        HashMap<String, String> map = new HashMap<>();
        map.put("resourcePrefix", "");
        get("/interviewer-view", (req, res) -> new ModelAndView(map, "index.html"),
                new MustacheTemplateEngine());
    }

    private static DbConfig getDbConfig() {
        ResourceBundle bundle = ResourceBundle.getBundle("config", Locale.getDefault());
        return new DbConfig(bundle.getString("db_host"),
                Integer.parseInt(bundle.getString("db_port")), bundle.getString("db_name"),
                bundle.getString("db_username"), bundle.getString("db_password"));
    }
}
