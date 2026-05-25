package com.sprintlite.sprintlite_backend.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    public void sendInvitationEmail(String toEmail, String inviterName, 
                                     String projectName, String tempPassword, 
                                     String invitationLink, String customMessage) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("noreply@sprintlite.com");
            helper.setTo(toEmail);
            helper.setSubject("You're invited to join " + projectName + " on SprintLite");
            helper.setText(buildInvitationEmailContent(inviterName, projectName, tempPassword, invitationLink, customMessage), true);
            
            mailSender.send(message);
            log.info("✅ Invitation email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("❌ Failed to send email to: {}", toEmail, e);
        }
    }
    
    public void sendWelcomeNotification(com.sprintlite.sprintlite_backend.domain.entity.User user, 
                                         com.sprintlite.sprintlite_backend.domain.entity.Company company) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("noreply@sprintlite.com");
            helper.setTo(user.getEmail());
            helper.setSubject("Welcome to SprintLite!");
            helper.setText(buildWelcomeEmailContent(user, company), true);
            
            mailSender.send(message);
            log.info("✅ Welcome email sent successfully to: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("❌ Failed to send welcome email to: {}", user.getEmail(), e);
        }
    }
    
    private String buildInvitationEmailContent(String inviterName, String projectName, 
                                                String tempPassword, String invitationLink, 
                                                String customMessage) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background: linear-gradient(135deg, #6366F1, #8B5CF6); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                .header h1 { color: white; margin: 0; font-size: 28px; }
                .content { background: #f8fafc; padding: 30px; border-radius: 0 0 10px 10px; }
                .password-box { background: #e2e8f0; padding: 15px; border-radius: 8px; font-family: monospace; font-size: 20px; text-align: center; margin: 20px 0; letter-spacing: 2px; }
                .button { display: inline-block; background: #6366F1; color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; margin: 20px 0; font-weight: bold; }
                .button:hover { background: #4f46e5; }
                .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #666; }
                .warning { background: #fef3c7; padding: 15px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #f59e0b; }
                .info { background: #dbeafe; padding: 15px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #3b82f6; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🚀 SprintLite</h1>
                </div>
                <div class="content">
                    <h2>Hello!</h2>
                    <p><strong>%s</strong> has invited you to join the project <strong>%s</strong> on SprintLite.</p>
                    
                    %s
                    
                    <div class="info">
                        <strong>📋 Project Details:</strong><br>
                        • Project: %s<br>
                        • Role: Employee<br>
                        • Organization: SprintLite
                    </div>
                    
                    <div class="password-box">
                        <strong>🔑 Your Temporary Password:</strong><br>
                        <span style="font-size: 24px; font-weight: bold;">%s</span>
                    </div>
                    
                    <div class="warning">
                        ⚠️ <strong>Important:</strong> This is a temporary password. You will be required to change it when you first log in.
                    </div>
                    
                    <div style="text-align: center;">
                        <a href="%s" class="button">✅ Accept Invitation →</a>
                    </div>
                    
                    <p>Or copy this link:<br>
                    <small>%s</small></p>
                    
                    <hr>
                    <h3>What happens next?</h3>
                    <ol>
                        <li>Click the button above to accept the invitation</li>
                        <li>Login with your email and the temporary password above</li>
                        <li>You'll be prompted to change your password</li>
                        <li>Start collaborating with your team!</li>
                    </ol>
                </div>
                <div class="footer">
                    <p>This invitation will expire in 7 days.<br>
                    If you didn't expect this invitation, please ignore this email.</p>
                    <p>© 2026 SprintLite. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(inviterName, projectName, 
                customMessage != null && !customMessage.isEmpty() ? "<p><em>\"" + customMessage + "\"</em></p>" : "",
                projectName, tempPassword, invitationLink, invitationLink);
    }
    
    private String buildWelcomeEmailContent(com.sprintlite.sprintlite_backend.domain.entity.User user, 
                                             com.sprintlite.sprintlite_backend.domain.entity.Company company) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background: linear-gradient(135deg, #10b981, #059669); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                .header h1 { color: white; margin: 0; }
                .content { background: #f8fafc; padding: 30px; border-radius: 0 0 10px 10px; }
                .button { display: inline-block; background: #6366F1; color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; margin: 20px 0; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🎉 Welcome to SprintLite!</h1>
                </div>
                <div class="content">
                    <h2>Hello %s!</h2>
                    <p>Welcome to <strong>%s</strong> on SprintLite.</p>
                    <p>You're now part of a modern collaboration platform that combines project management, chat, video calls, and time tracking in one place.</p>
                    
                    <h3>Quick Start Guide:</h3>
                    <ul>
                        <li>📋 Check your assigned tasks</li>
                        <li>💬 Join team conversations in channels</li>
                        <li>📅 Mark your daily attendance</li>
                        <li>🎥 Start video meetings with your team</li>
                    </ul>
                    
                    <div style="text-align: center;">
                        <a href="http://localhost:5173/login" class="button">🚀 Go to Dashboard →</a>
                    </div>
                </div>
                <div class="footer">
                    <p>© 2026 SprintLite. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(user.getFirstName(), company.getName());
    }
}