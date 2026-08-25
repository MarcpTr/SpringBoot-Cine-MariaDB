package com.marcptr.cine.service;

import org.springframework.stereotype.Service;
import com.marcptr.cine.exception.FieldValidationException;
import com.marcptr.cine.exception.ResourceNotFoundException;
import com.marcptr.cine.model.AppSetting;
import com.marcptr.cine.model.enums.ErrorCode;
import com.marcptr.cine.repository.AppSettingRepository;
import com.marcptr.cine.utils.MessageResolver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionPolicyService {
    private final MessageResolver messageResolver;
    private final AppSettingRepository repository;

    public int getMaxSessions() {
        return repository.findByConfigKey("security.max_sessions")
                .map(setting -> Integer.parseInt(setting.getConfigValue()))
                .orElse(1);
    }

    @Transactional
    public void updateMaxSessions(int value) {
        if (value < 1) {
            throw new FieldValidationException(ErrorCode.MIN_SESSIONS,
                    messageResolver.resolveMessage(ErrorCode.MIN_SESSIONS));
        }
        if (value > 10) {
            throw new FieldValidationException(ErrorCode.MAX_SESSIONS,
                    messageResolver.resolveMessage(ErrorCode.MAX_SESSIONS));
        }
        AppSetting setting = repository.findByConfigKey("security.max_sessions")
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SETTING_NOT_FOUND,
                        messageResolver.resolveMessage(ErrorCode.SETTING_NOT_FOUND)));

        setting.setConfigValue(String.valueOf(value));
        repository.save(setting);
    }
}