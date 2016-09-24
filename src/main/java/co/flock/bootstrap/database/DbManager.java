package co.flock.bootstrap.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.Dao.CreateOrUpdateStatus;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class DbManager
{
    private Dao<Candidate, String> _candidateDao;

    public DbManager(DbConfig dbConfig) throws SQLException
    {
        setupDatabase(dbConfig);
    }

    public CreateOrUpdateStatus insertOrUpdateCandidate(Candidate candidate) throws SQLException
    {
        return _candidateDao.createOrUpdate(candidate);
    }

    private void setupDatabase(DbConfig dbConfig) throws SQLException
    {
        JdbcPooledConnectionSource connectionSource = new JdbcPooledConnectionSource(
                dbConfig.getConnectionURL());
        TableUtils.createTableIfNotExists(connectionSource, Candidate.class);
        connectionSource.setMaxConnectionAgeMillis(Long.MAX_VALUE);
        _candidateDao = DaoManager.createDao(connectionSource, Candidate.class);
    }
}
