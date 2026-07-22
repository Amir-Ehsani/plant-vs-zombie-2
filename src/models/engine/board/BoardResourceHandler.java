package models.engine.board;

public interface BoardResourceHandler {
    int stealStoredSun(int amount);
    int stealLooseSuns();
    void restoreSun(int amount);
}
