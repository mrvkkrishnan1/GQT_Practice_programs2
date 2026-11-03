package com.jdbcprogramingday2;

public class QuizQuis {
	private final int id;
    private final String question;
    private final String[] options;
    private final char correctOption;

    public QuizQuis(int id, String question,
                        String[] options, char correctOption) {
        this.id = id;
        this.question = question;
        this.options = options;
        this.correctOption =
                Character.toUpperCase(correctOption);
    }

    public int getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public String[] getOptions() {
        return options;
    }

    public char getCorrectOption() {
        return correctOption;
    }
    
    
}
