package co.flock.bootstrap.database;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = DbConstants.Table.ROUND)

public class Round
{
    @DatabaseField(columnName = DbConstants.Fields.EMAIL, canBeNull = false)
    private String _candidateEmail;

    @DatabaseField(columnName = DbConstants.Fields.INTERVIEWER_ID, canBeNull = false)
    private String _interviewerID;

    @DatabaseField(columnName = DbConstants.Fields.SEQUENCE, canBeNull = false)
    private int _sequence;

    @DatabaseField(columnName = DbConstants.Fields.VERDICT, dataType = DataType.ENUM_INTEGER)
    private VERDICT _verdict;

    @DatabaseField(columnName = DbConstants.Fields.COMMENTS)
    private String _comments;

    @DatabaseField(columnName = DbConstants.Fields.COLLAB_LINK, canBeNull = false)
    private String _collabLink;

    @DatabaseField(columnName = DbConstants.Fields.RATING)
    private float _rating;

    @DatabaseField(columnName = DbConstants.Fields.QUESTION_ID, canBeNull = false)
    private String _questionID;

    @DatabaseField(columnName = DbConstants.Fields.SCHEDULED_TIME, canBeNull = false)
    private String _scheduledTime;


    public Round()
    {
    }

    public Round(String candidateEmail, String interviewerID, int sequence, String collabLink, String questionID, String scheduledTime)
    {
        _candidateEmail = candidateEmail;
        _interviewerID = interviewerID;
        _sequence = sequence;
        _collabLink = collabLink;
        _questionID = questionID;
        _scheduledTime = scheduledTime;
    }

    public String getCandidateEmail()
    {
        return _candidateEmail;
    }

    public String getInterviewerID()
    {
        return _interviewerID;
    }

    public int getSequence()
    {
        return _sequence;
    }

    public VERDICT getVerdict()
    {
        return _verdict;
    }

    public String getComments()
    {
        return _comments;
    }

    public String getCollabLink()
    {
        return _collabLink;
    }

    public float getRating()
    {
        return _rating;
    }

    public String getQuestionID()
    {
        return _questionID;
    }

    public String getScheduledTime()
    {
        return _scheduledTime;
    }

    public void setVerdict(VERDICT verdict)
    {
        _verdict = verdict;
    }

    public void setComments(String comments)
    {
        _comments = comments;
    }

    public void setRating(float rating)
    {
        _rating = rating;
    }

    @Override
    public String toString()
    {
        return "Round{" +
                "_candidateEmail='" + _candidateEmail + '\'' +
                ", _interviewerID='" + _interviewerID + '\'' +
                ", _sequence=" + _sequence +
                ", _verdict=" + _verdict +
                ", _comments='" + _comments + '\'' +
                ", _collabLink='" + _collabLink + '\'' +
                ", _rating=" + _rating +
                ", _questionID='" + _questionID + '\'' +
                ", _scheduledTime='" + _scheduledTime + '\'' +
                '}';
    }

    public enum VERDICT
    {
        PASS, REJECT;
    }
}
