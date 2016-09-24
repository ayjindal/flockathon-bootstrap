package co.flock.bootstrap;

import org.apache.log4j.Logger;

import java.util.HashMap;

import static spark.Spark.port;
import static spark.Spark.staticFileLocation;

public class Runner
{
    private static final Logger _logger = Logger.getLogger(Runner.class);

    public static void main(String[] args) {
        _logger.debug("Starting..");
        port(9000);
        HashMap map = new HashMap();
        staticFileLocation("/public");
        map.put("resourcePrefix", "");
    }
}
