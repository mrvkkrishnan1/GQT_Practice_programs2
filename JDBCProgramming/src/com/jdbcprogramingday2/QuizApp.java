package com.jdbcprogramingday2;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class QuizApp extends JFrame {

    private static final int TOTAL_QUESTIONS = 10;
    private static final int POINTS = 100;

    private final List<QuizQuis> questions = new ArrayList<>();
    private final Random random = new Random();

    private String playerName;
    private int currentQuestion = 0;
    private int score = 0;

    // Q1-Q4 special scoring
    private boolean firstFourCorrect = true;

    // Lifeline status
    private boolean audienceUsed = false;
    private boolean fiftyFiftyUsed = false;

    // Swing components
    private JLabel questionNumberLabel;
    private JLabel questionLabel;

    private final JRadioButton[] optionButtons =
            new JRadioButton[4];

    private ButtonGroup optionGroup;

    private JButton submitButton;
    private JButton audienceButton;
    private JButton fiftyFiftyButton;

    public QuizApp() {

        setTitle("Java Quiz Application");
        setSize(720, 500);
        setMinimumSize(new Dimension(600, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        playerName = JOptionPane.showInputDialog(
                this,
                "Enter your name:",
                "Welcome to Java Quiz",
                JOptionPane.QUESTION_MESSAGE
        );

        if (playerName == null) {
            dispose();
            return;
        }

        playerName = playerName.trim();

        if (playerName.isEmpty()) {
            playerName = "Player";
        }

        if (!loadQuestions()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Unable to load questions from MySQL.\n"
                    + "Check the database, password and JDBC driver.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
            dispose();
            return;
        }

        createGUI();
        loadQuestion();
        setVisible(true);
    }

    // Create the Swing interface
    private void createGUI() {

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        );

        JLabel titleLabel = new JLabel(
                "JAVA QUIZ",
                SwingConstants.CENTER
        );

        titleLabel.setFont(
                new Font("Arial", Font.BOLD, 28)
        );

        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Question section
        JPanel centerPanel = new JPanel();

        centerPanel.setLayout(
                new BoxLayout(centerPanel, BoxLayout.Y_AXIS)
        );

        questionNumberLabel = new JLabel();
        questionNumberLabel.setFont(
                new Font("Arial", Font.BOLD, 18)
        );

        questionLabel = new JLabel();
        questionLabel.setFont(
                new Font("Arial", Font.PLAIN, 18)
        );

        centerPanel.add(questionNumberLabel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(questionLabel);
        centerPanel.add(Box.createVerticalStrut(20));

        // Answer options
        JPanel optionsPanel = new JPanel(
                new GridLayout(4, 1, 5, 5)
        );

        optionGroup = new ButtonGroup();

        for (int i = 0; i < 4; i++) {

            optionButtons[i] = new JRadioButton();

            optionButtons[i].setFont(
                    new Font("Arial", Font.PLAIN, 16)
            );

            optionGroup.add(optionButtons[i]);
            optionsPanel.add(optionButtons[i]);
        }

        centerPanel.add(optionsPanel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 5)
        );

        audienceButton = new JButton("Audience Poll");
        fiftyFiftyButton = new JButton("50-50");
        submitButton = new JButton("Submit Answer");

        audienceButton.addActionListener(
                e -> useAudiencePoll()
        );

        fiftyFiftyButton.addActionListener(
                e -> useFiftyFifty()
        );

        submitButton.addActionListener(
                3 -> checkAnswer()
        );

        buttonPanel.add(audienceButton);
        buttonPanel.add(fiftyFiftyButton);
        buttonPanel.add(submitButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    // Load questions from MySQL using JDBC
    private boolean loadQuestions() {

        String sql =
                "SELECT id, question_text, option_a, option_b, "
                + "option_c, option_d, correct_option "
                + "FROM questions ORDER BY id LIMIT ?";

        try (Connection con = DataBaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, TOTAL_QUESTIONS);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String correct = rs.getString("correct_option");

                    if (correct == null || correct.trim().isEmpty()) {
                        throw new SQLException(
                                "A question has no correct answer."
                        );
                    }

                    String[] options = {
                        rs.getString("option_a"),
                        rs.getString("option_b"),
                        rs.getString("option_c"),
                        rs.getString("option_d")
                    };

                    QuizQuis q = new QuizQuis(
                            rs.getInt("id"),
                            rs.getString("question_text"),
                            options,
                            correct.trim().charAt(0)
                    );

                    questions.add(q);
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
            return false;
        }

        if (questions.size() != TOTAL_QUESTIONS) {

            JOptionPane.showMessageDialog(
                    null,
                    "The database must contain at least 10 questions.\n"
                    + "Questions found: " + questions.size()
            );

            return false;
        }

        return true;
    }

    // Display the current question and options
    private void loadQuestion() {

    	QuizQuis q = questions.get(currentQuestion);

        questionNumberLabel.setText(
                "Question " + (currentQuestion + 1)
                + " of " + TOTAL_QUESTIONS
        );

        questionLabel.setText(
                "<html><div style='width:600px'>"
                + escapeHtml(q.getQuestion())
                + "</div></html>"
        );

        optionGroup.clearSelection();

        for (int i = 0; i < 4; i++) {

            optionButtons[i].setText(
                    (char) ('A' + i) + ". "
                    + q.getOptions()[i]
            );

            optionButtons[i].setVisible(true);
            optionButtons[i].setEnabled(true);
        }

        // Lifelines are available only for Q1-Q7
        boolean available = currentQuestion < 7;

        audienceButton.setEnabled(
                available && !audienceUsed
        );

        fiftyFiftyButton.setEnabled(
                available && !fiftyFiftyUsed
        );
    }

    private String escapeHtml(String text) {

        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    // Check the selected answer and calculate the score
    private void checkAnswer() {

        int selectedAnswer = -1;

        for (int i = 0; i < 4; i++) {

            if (optionButtons[i].isSelected()) {
                selectedAnswer = i;
                break;
            }
        }

        if (selectedAnswer == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select an answer."
            );

            return;
        }

        QuizQuis q = questions.get(currentQuestion);

        char selectedLetter = (char) ('A' + selectedAnswer);
        char correctLetter = q.getCorrectOption();

        boolean isCorrect = selectedLetter == correctLetter;

        if (isCorrect) {

            JOptionPane.showMessageDialog(
                    this,
                    "Correct Answer!"
            );

            // Q5-Q10 receive individual marks
            if (currentQuestion >= 4) {
                score += POINTS;
            }

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "Wrong Answer!\nCorrect answer: "
                    + correctLetter
            );

            // Any wrong answer in Q1-Q4 cancels
            // the score for the entire first-four group
            if (currentQuestion < 4) {
                firstFourCorrect = false;
            }
        }

        currentQuestion++;

        // Award 400 only when Q1-Q4 are all correct
        if (currentQuestion == 4 && firstFourCorrect) {
            score += 4 * POINTS;
        }

        if (currentQuestion >= TOTAL_QUESTIONS) {
            finishQuiz();
        } else {
            loadQuestion();
        }
    }

    // Audience Poll lifeline
    private void useAudiencePoll() {

        if (audienceUsed || currentQuestion >= 7) {
            return;
        }

        audienceUsed = true;

        int correctIndex =
                questions.get(currentQuestion).getCorrectOption() - 'A';

        // Simulated poll: correct option receives 50%-80%
        int correctPercent = 50 + random.nextInt(31);
        int remaining = 100 - correctPercent;

        int[] percentages = new int[4];
        percentages[correctIndex] = correctPercent;

        List<Integer> wrongIndexes = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            if (i != correctIndex) {
                wrongIndexes.add(i);
            }
        }

        Collections.shuffle(wrongIndexes, random);

        int first = random.nextInt(remaining + 1);
        int second = random.nextInt(remaining - first + 1);
        int third = remaining - first - second;

        percentages[wrongIndexes.get(0)] = first;
        percentages[wrongIndexes.get(1)] = second;
        percentages[wrongIndexes.get(2)] = third;

        StringBuilder message = new StringBuilder(
                "Audience Poll Results:\n\n"
        );

        for (int i = 0; i < 4; i++) {

            message.append((char) ('A' + i))
                   .append(". ")
                   .append(percentages[i])
                   .append("%\n");
        }

        JOptionPane.showMessageDialog(
                this,
                message.toString(),
                "Audience Poll",
                JOptionPane.INFORMATION_MESSAGE
        );

        audienceButton.setEnabled(false);
    }

    // 50-50 lifeline
    private void useFiftyFifty() {

        if (fiftyFiftyUsed || currentQuestion >= 7) {
            return;
        }

        fiftyFiftyUsed = true;

        int correctIndex =
                questions.get(currentQuestion).getCorrectOption() - 'A';

        List<Integer> wrongIndexes = new ArrayList<>();

        for (int i = 0; i < 4; i++) {

            if (i != correctIndex) {
                wrongIndexes.add(i);
            }
        }

        Collections.shuffle(wrongIndexes, random);

        // Hide exactly two incorrect options
        for (int i = 0; i < 2; i++) {
            optionButtons[wrongIndexes.get(i)].setVisible(false);
        }

        fiftyFiftyButton.setEnabled(false);
    }

    // Save the result and display the final score
    private void finishQuiz() {

        saveResult();

        String performance;

        if (score == 1000) {
            performance = "PERFECT SCORE!";
        } else if (score >= 800) {
            performance = "EXCELLENT!";
        } else if (score >= 500) {
            performance = "GOOD PERFORMANCE!";
        } else {
            performance = "KEEP PRACTICING JAVA!";
        }

        JOptionPane.showMessageDialog(
                this,
                "Quiz Completed!\n\n"
                + "Player: " + playerName + "\n"
                + "Final Score: " + score + " / 1000\n\n"
                + performance,
                "Quiz Result",
                JOptionPane.INFORMATION_MESSAGE
        );

        dispose();
    }

    // Store the result in MySQL
    private void saveResult() {

        String sql =
                "INSERT INTO quiz_results "
                + "(player_name, score, total_questions) "
                + "VALUES (?, ?, ?)";

        try (Connection con = DataBaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, playerName);
            ps.setInt(2, score);
            ps.setInt(3, TOTAL_QUESTIONS);

            ps.executeUpdate();

        } catch (SQLException e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Quiz finished, but the result could not be saved.\n"
                    + e.getMessage(),
                    "Database Save Error",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new QuizApp();
        });
    }
}