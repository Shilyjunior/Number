package com.studentapp;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

public class NumberGuessServletTest {
    private NumberGuessServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    @Before
    public void setUp() throws Exception {
        servlet = new NumberGuessServlet();
        servlet.init();
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
        Mockito.when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    public void testGuessTooLow() throws Exception {
        // Set target number to a known value for testing
        servlet.setTargetNumber(50);
        
        Mockito.when(request.getParameter("guess")).thenReturn("25");
        servlet.doPost(request, response);
        
        printWriter.flush(); // Ensure content is written
        String output = responseWriter.toString();
        assertTrue("Expected 'too low' message", output.contains("too low"));
    }

    @Test
    public void testGuessTooHigh() throws Exception {
        // Set target number to a known value for testing
        servlet.setTargetNumber(50);
        
        Mockito.when(request.getParameter("guess")).thenReturn("75");
        servlet.doPost(request, response);
        
        printWriter.flush();
        String output = responseWriter.toString();
        assertTrue("Expected 'too high' message", output.contains("too high"));
    }

    @Test
    public void testCorrectGuess() throws Exception {
        // Set target number to a known value for testing
        int targetNumber = 42;
        servlet.setTargetNumber(targetNumber);
        
        Mockito.when(request.getParameter("guess")).thenReturn(String.valueOf(targetNumber));
        servlet.doPost(request, response);
        
        printWriter.flush();
        String output = responseWriter.toString();
        assertTrue("Expected congratulations message", output.contains("Congratulations"));
    }

    @Test
    public void testInvalidInput() throws Exception {
        Mockito.when(request.getParameter("guess")).thenReturn("not_a_number");
        servlet.doPost(request, response);
        
        printWriter.flush();
        String output = responseWriter.toString();
        assertTrue("Expected invalid input message", output.contains("Invalid input"));
    }

    @Test
    public void testOutOfRangeInput() throws Exception {
        servlet.setTargetNumber(50);
        
        // Test number too high
        Mockito.when(request.getParameter("guess")).thenReturn("150");
        servlet.doPost(request, response);
        
        printWriter.flush();
        String output = responseWriter.toString();
        assertTrue("Expected range validation message", 
                   output.contains("between 1 and 100"));
    }

    @Test
    public void testDoGet() throws Exception {
        servlet.doGet(request, response);
        
        printWriter.flush();
        String output = responseWriter.toString();
        assertTrue("Expected game form", output.contains("<form"));
        assertTrue("Expected input field", output.contains("name='guess'"));
        assertTrue("Expected submit button", output.contains("Submit"));
    }

    @Test
    public void testGetTargetNumber() throws Exception {
        int targetNumber = servlet.getTargetNumber();
        assertTrue("Target number should be between 1 and 100", 
                   targetNumber >= 1 && targetNumber <= 100);
    }
}