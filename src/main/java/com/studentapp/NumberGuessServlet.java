package com.studentapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NumberGuessServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private int targetNumber;

    public void init() throws ServletException {
        targetNumber = new Random().nextInt(100) + 1;
    }

    // Add getter method for testing
    public int getTargetNumber() {
        return targetNumber;
    }

    // Add setter method for testing
    public void setTargetNumber(int targetNumber) {
        this.targetNumber = targetNumber;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><title>Number Guessing Game</title></head>");
        out.println("<body>");
        out.println("<h1>Number Guessing Game</h1>");
        out.println("<p>I'm thinking of a number between 1 and 100. Can you guess it?</p>");
        out.println("<form action='guess' method='post'>");
        out.println("Your guess: <input type='number' name='guess' min='1' max='100' required />");
        out.println("<input type='submit' value='Submit Guess' />");
        out.println("</form>");
        out.println("</body>");
        out.println("</html>");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><title>Number Guessing Game - Result</title></head>");
        out.println("<body>");
        
        try {
            int guess = Integer.parseInt(request.getParameter("guess"));
            
            if (guess < 1 || guess > 100) {
                out.println("<h2>Please enter a number between 1 and 100.</h2>");
            } else if (guess < targetNumber) {
                out.println("<h2>Your guess of " + guess + " is too low. Try again!</h2>");
            } else if (guess > targetNumber) {
                out.println("<h2>Your guess of " + guess + " is too high. Try again!</h2>");
            } else {
                out.println("<h2>🎉 Congratulations! You guessed the number " + targetNumber + "!</h2>");
                out.println("<p>Starting a new game...</p>");
                targetNumber = new Random().nextInt(100) + 1; // Reset game
            }
        } catch (NumberFormatException e) {
            out.println("<h2>Invalid input. Please enter a valid number.</h2>");
        }
        
        out.println("<a href='guess'>Play Again</a>");
        out.println("</body>");
        out.println("</html>");
    }
}