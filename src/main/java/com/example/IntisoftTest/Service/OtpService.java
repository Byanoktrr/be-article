package com.example.IntisoftTest.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class OtpService {

    private final Map<String, String> otpMap = new HashMap<>();
    private final Map<String, LocalDateTime> otpExpiryMap = new HashMap<>();
    private final Map<String, Integer> attemptMap = new HashMap<>();
    private final Map<String, LocalDateTime> blockMap = new HashMap<>();

    public String generateOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpMap.put(email, otp);
        otpExpiryMap.put(email, LocalDateTime.now().plusMinutes(5));
        return otp;
    }

    public boolean isBlocked(String email) {
        if (!blockMap.containsKey(email)) return false;
        if (blockMap.get(email).isBefore(LocalDateTime.now())) {
            blockMap.remove(email);
            attemptMap.put(email, 0);
            return false;
        }
        return true;
    }

    public boolean validateOtp(String email, String otp) {
        if (!otpMap.containsKey(email)) return false;
        if (otpExpiryMap.get(email).isBefore(LocalDateTime.now())) return false;
        return otpMap.get(email).equals(otp);
    }

    public void registerFailedAttempt(String email) {
        int attempts = attemptMap.getOrDefault(email, 0) + 1;
        attemptMap.put(email, attempts);
        if (attempts >= 5) {
            blockMap.put(email, LocalDateTime.now().plusMinutes(30));
        }
    }


    public void clearOtp(String email) {
        otpMap.remove(email);
        otpExpiryMap.remove(email);
        attemptMap.put(email, 0);
    }
}
