package co.flock.bootstrap.database;

public class DbConfig
{
    private final String _host;
    private final int _port;
    private final String _dbName;
    private final String _username;
    private final String _password;

    public DbConfig(String host, int port, String dbName, String username, String password)
    {
        _host = host;
        _port = port;
        _dbName = dbName;
        _username = username;
        _password = password;
    }

    public String getHost()
    {
        return _host;
    }

    public int getPort()
    {
        return _port;
    }

    public String getDbName()
    {
        return _dbName;
    }

    public String getConnectionURL()
    {
        return String
            .format("jdbc:mysql://%s:%s/%s?user=%s&password=%s", _host, _port, _dbName, _username,
                _password);
    }

    @Override
    public String toString()
    {
        return "DbConfig{" +
               "_host='" + _host + '\'' +
               ", _port=" + _port +
               ", _dbName='" + _dbName + '\'' +
               ", _username='" + _username + '\'' +
               ", _password='" + _password + '\'' +
               '}';
    }
}
