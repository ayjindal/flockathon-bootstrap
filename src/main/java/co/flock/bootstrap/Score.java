package co.flock.bootstrap;

public class Score
{
    private String _name;
    private int _score;

    public Score(String name, int score)
    {
        _name = name;
        _score = score;
    }

    public String getName()
    {
        return _name;
    }

    public int getScore()
    {
        return _score;
    }
}
