package co.flock.bootstrap.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.Dao.CreateOrUpdateStatus;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.List;

public class DbManager
{
    private Dao<User, String> _userDao;
    private Dao<Candidate, String> _candidateDao;
    private Dao<Round, String> _roundDao;
    private Dao<Question, String> _questionDao;

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

    public CreateOrUpdateStatus insertOrUpdateRound(Round round) throws SQLException
    {
        QueryBuilder<Round, String> queryBuilder = _roundDao.queryBuilder();
        Where<Round, String> where = queryBuilder.where();
        where.eq(DbConstants.Fields.INTERVIEWER_ID, round.getInterviewerID());
        where.and();
        where.eq(DbConstants.Fields.EMAIL, round.getCandidateEmail());
        PreparedQuery<Round> preparedQuery = queryBuilder.prepare();
        List<Round> rounds = _roundDao.query(preparedQuery);

        if (rounds.size() > 0) {
            _roundDao.deleteById(String.valueOf(rounds.get(0).getId()));
        }
        return _roundDao.createOrUpdate(round);

    }

    public CreateOrUpdateStatus insertOrUpdateQuestion(Question question) throws SQLException
    {
        return _questionDao.createOrUpdate(question);
    }

    public User getUserById(String userID) throws SQLException
    {
        return _userDao.queryForId(userID);
    }

    public Candidate getCandidateByEmail(String email) throws SQLException
    {
        return _candidateDao.queryForId(email);
    }

    public Question getQuestionById(String id) throws SQLException
    {
        return _questionDao.queryForId(id);
    }

    public List<User> getAllUsers() throws SQLException
    {
        return _userDao.queryForAll();
    }

    public void deleteUser(User user) throws SQLException
    {
        _userDao.delete(user);
    }

    public List<Question> getQuestions(Candidate.ROLE role, String groupId) throws SQLException
    {
        if (role != null) {
            QueryBuilder<Question, String> queryBuilder = _questionDao.queryBuilder();
            Where<Question, String> where = queryBuilder.where();
            where.eq(DbConstants.Fields.ROLE, role);
//            where.and();
//            where.eq(DbConstants.Fields.GROUP_ID, groupId);
            PreparedQuery<Question> preparedQuery = queryBuilder.prepare();
            return _questionDao.query(preparedQuery);
        } else {
            return _questionDao.queryForAll();
        }
    }

    public List<Round> getCandidateRounds(String email) throws SQLException
    {
        QueryBuilder<Round, String> queryBuilder = _roundDao.queryBuilder();
        queryBuilder.where().eq(DbConstants.Fields.EMAIL, email);
        PreparedQuery<Round> preparedQuery = queryBuilder.prepare();
        return _roundDao.query(preparedQuery);
    }

    public Round getCandidateFirstRound(String email) throws SQLException
    {
        QueryBuilder<Round, String> queryBuilder = _roundDao.queryBuilder();
        queryBuilder.where().eq(DbConstants.Fields.EMAIL, email);
        PreparedQuery<Round> preparedQuery = queryBuilder.prepare();
        List<Round> rounds = _roundDao.query(preparedQuery);
        if (rounds.size() > 0) {
            for (Round round : rounds) {
                if (round.getSequence() == 1) {
                    return round;
                }
            }
        }
        return null;

    }
    public List<Candidate> getCandidatesByGroupId(String groupId) throws SQLException
    {
        QueryBuilder<Candidate, String> queryBuilder = _candidateDao.queryBuilder();
        queryBuilder.where().eq(DbConstants.Fields.GROUP_ID, groupId);
        PreparedQuery<Candidate> preparedQuery = queryBuilder.prepare();
        return _candidateDao.query(preparedQuery);
    }

    public List<Round> getCandidateRounds(String email, String interviewerId) throws SQLException
    {
        QueryBuilder<Round, String> queryBuilder = _roundDao.queryBuilder();
        Where<Round, String> where = queryBuilder.where();
        where.eq(DbConstants.Fields.EMAIL, email);
        where.and();
        where.eq(DbConstants.Fields.INTERVIEWER_ID, interviewerId);
        PreparedQuery<Round> preparedQuery = queryBuilder.prepare();
        return _roundDao.query(preparedQuery);
    }

    public Round getRound(String email, String interviewerId) throws SQLException
    {
        QueryBuilder<Round, String> queryBuilder = _roundDao.queryBuilder();
        Where<Round, String> where = queryBuilder.where();
        where.eq(DbConstants.Fields.INTERVIEWER_ID, interviewerId);
        where.and();
        where.eq(DbConstants.Fields.EMAIL, email);
        PreparedQuery<Round> preparedQuery = queryBuilder.prepare();
        List<Round> rounds = _roundDao.query(preparedQuery);

        if (rounds.size() > 0) {
            Round round = rounds.get(0);
            return round;
        }
        return null;
    }

    public void updateRound(String email, String interviewerId, String comments, Float rating, Round.VERDICT verdict) throws SQLException
    {
        QueryBuilder<Round, String> queryBuilder = _roundDao.queryBuilder();
        Where<Round, String> where = queryBuilder.where();
        where.eq(DbConstants.Fields.INTERVIEWER_ID, interviewerId);
        where.and();
        where.eq(DbConstants.Fields.EMAIL, email);
        PreparedQuery<Round> preparedQuery = queryBuilder.prepare();
        List<Round> rounds = _roundDao.query(preparedQuery);

        if (rounds.size() > 0) {
            Round round = rounds.get(0);
            round.setComments(comments);
            round.setRating(rating);
            round.setVerdict(verdict);
            _roundDao.update(round);
        }
    }

    private void setupDatabase(DbConfig dbConfig) throws SQLException
    {
        JdbcPooledConnectionSource connectionSource = new JdbcPooledConnectionSource(
                dbConfig.getConnectionURL());
        TableUtils.createTableIfNotExists(connectionSource, User.class);
        TableUtils.createTableIfNotExists(connectionSource, Candidate.class);
        TableUtils.createTableIfNotExists(connectionSource, Round.class);
        TableUtils.createTableIfNotExists(connectionSource, Question.class);
        // TODO : increase varchar length of question's text field
        connectionSource.setMaxConnectionAgeMillis(Long.MAX_VALUE);
        _userDao = DaoManager.createDao(connectionSource, User.class);
        _candidateDao = DaoManager.createDao(connectionSource, Candidate.class);
        _roundDao = DaoManager.createDao(connectionSource, Round.class);
        _questionDao = DaoManager.createDao(connectionSource, Question.class);
    }

    public List<Round> getRoundsOwnedByUser(String userId) throws SQLException
    {
        QueryBuilder<Round, String> queryBuilder = _roundDao.queryBuilder();
        queryBuilder.where().eq(DbConstants.Fields.INTERVIEWER_ID, userId);
        PreparedQuery<Round> preparedQuery = queryBuilder.prepare();
        return _roundDao.query(preparedQuery);
    }

    public List<Candidate> getCandidatesForGroup(String groupId) throws SQLException
    {
        QueryBuilder<Candidate, String> queryBuilder = _candidateDao.queryBuilder();
        queryBuilder.where().eq(DbConstants.Fields.GROUP_ID, groupId);
        PreparedQuery<Candidate> preparedQuery = queryBuilder.prepare();
        return _candidateDao.query(preparedQuery);
    }
}
