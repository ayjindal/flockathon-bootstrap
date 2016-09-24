package co.flock.bootstrap.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.Dao.CreateOrUpdateStatus;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.List;

public class DbManager
{
    private Dao<User, String> _userDao;
    private Dao<Candidate, String> _candidateDao;

    public DbManager(DbConfig dbConfig) throws SQLException
    {
        setupDatabase(dbConfig);
    }

    public CreateOrUpdateStatus insertOrUpdateCandidate(Candidate candidate) throws SQLException
    {
        return _candidateDao.createOrUpdate(candidate);
    }

    public CreateOrUpdateStatus insertOrUpdateUser(User user) throws SQLException
    {
        return _userDao.createOrUpdate(user);
    }

    public User getUserById(String userID) throws SQLException
    {
        return _userDao.queryForId(userID);
    }

    public List<User> getAllUsers() throws SQLException
    {
        return _userDao.queryForAll();
    }

    public void deleteUser(User user) throws SQLException
    {
        _userDao.delete(user);
    }

    private void setupDatabase(DbConfig dbConfig) throws SQLException
    {
        JdbcPooledConnectionSource connectionSource = new JdbcPooledConnectionSource(
                dbConfig.getConnectionURL());
        TableUtils.createTableIfNotExists(connectionSource, User.class);
        TableUtils.createTableIfNotExists(connectionSource, Candidate.class);
        connectionSource.setMaxConnectionAgeMillis(Long.MAX_VALUE);
        _userDao = DaoManager.createDao(connectionSource, User.class);
        _candidateDao = DaoManager.createDao(connectionSource, Candidate.class);
    }
}
